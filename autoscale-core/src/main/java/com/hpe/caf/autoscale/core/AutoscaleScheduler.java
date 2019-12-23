/*
 * Copyright 2015-2020 Micro Focus or one of its affiliates.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.hpe.caf.autoscale.core;


import com.hpe.caf.api.HealthReporter;
import com.hpe.caf.api.HealthResult;
import com.hpe.caf.api.HealthStatus;
import com.hpe.caf.api.autoscale.AlertDispatcher;
import com.hpe.caf.api.autoscale.ScalerException;
import com.hpe.caf.api.autoscale.ScalingConfiguration;
import com.hpe.caf.api.autoscale.ServiceScaler;
import com.hpe.caf.api.autoscale.WorkloadAnalyser;
import com.hpe.caf.api.autoscale.WorkloadAnalyserFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.List;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.stream.Collectors;

import static java.util.stream.Collectors.toMap;
import static java.util.stream.Collectors.toSet;


/**
 * This object is responsible for taking a Set of ScalingConfiguration objects, and determining
 * which of these services we wish to monitor and autoscale. For any that we do, a ScalerThread
 * is created with its appropriate dependencies for the service, and is scheduled to run
 * periodically with a ScheduledExecutorService. Services that are no longer present will be
 * removed from monitoring and have its ScalerThread cancelled. Services that change in configuration
 * will have their ScalerThread cancelled and a new one created with the new configuration to
 * replace it.
 */
public class AutoscaleScheduler implements HealthReporter
{
    private final ScheduledExecutorService scheduler;
    private final ServiceValidator validator;
    private final Map<String, ScheduledScalingService> scheduledServices = new HashMap<>();
    /**
     * Prevents simultaneous refreshes of the scheduled services.
     */
    private final Lock servicesLock = new ReentrantLock();
    /**
     * All available mechanisms for analysing workload.
     */
    private final Map<String, WorkloadAnalyserFactory> analyserFactories;
    private final ServiceScaler scaler;
    private static final int INITIAL_SCALING_DELAY = 30;
    private static final Logger LOG = LoggerFactory.getLogger(AutoscaleScheduler.class);
    private final Governor governor;
    private final Map<String, AlertDispatcher> alertDispatchers;
    private final ResourceMonitoringConfiguration resourceConfig;
    private final AlertDispatchConfiguration alertConfig;

    public AutoscaleScheduler(final Map<String, WorkloadAnalyserFactory> analyserFactories, final ServiceScaler scaler,
                              final ScheduledExecutorService scheduler, final ServiceValidator serviceValidator,
                              final Map<String, AlertDispatcher> alertDispatchers, final ResourceMonitoringConfiguration resourceConfig,
                              final AlertDispatchConfiguration alertConfig)
    {
        this.validator = Objects.requireNonNull(serviceValidator);
        this.analyserFactories = Objects.requireNonNull(analyserFactories);
        this.scaler = Objects.requireNonNull(scaler);
        this.scheduler = Objects.requireNonNull(scheduler);
        this.alertDispatchers = alertDispatchers;
        this.resourceConfig = resourceConfig;
        this.alertConfig = alertConfig;
        this.governor = new GovernorImpl(resourceConfig.getResourceLimitOneShutdownThreshold(),
                                         resourceConfig.getResourceLimitTwoShutdownThreshold(),
                                         resourceConfig.getResourceLimitThreeShutdownThreshold());
    }

    /**
     * Reload available services to monitor.
     * Firstly, get all available services from the ServiceSource, pass them to a ServiceValidator to determine the ones to track,
     * and then determine changes. Ones that are no longer present are cancelled from the scheduler. New ones or updated ones
     * are scheduled or rescheduled as appropriate. There is an initial scaling delay that increases, so that we don't try and
     * perform all the workload analysis and scaling calls at the same time.
     */
    public void updateServices(final Set<ScalingConfiguration> sourceServices)
    {
        servicesLock.lock();
        try {
            LOG.debug("Reloading services");
            Set<ScalingConfiguration> acquired = validator.getValidatedServices(sourceServices);
            Map<String, ScalingConfiguration> acquiredMap = acquired.stream().collect(toMap(ScalingConfiguration::getId, c -> c));
            getServicesToCancel(scheduledServices.keySet(), acquiredMap).forEach(this::cancel);
            int delay = 0;
            final Alerter alerter = new Alerter(alertDispatchers, alertConfig);
            for ( ScalingConfiguration s : getServicesToSchedule(scheduledServices, acquiredMap) ) {
                try {
                    scheduleOrReschedule(s, getAnalyser(s), INITIAL_SCALING_DELAY + delay++, alerter);
                } catch (ScalerException e) {
                    LOG.error("Failed to schedule service {}", s.getId(), e);
                    cancel(s.getId());
                }
            }
        } finally {
            servicesLock.unlock();
        }
    }


    public Map<String, ScheduledScalingService> getScheduledServices()
    {
        return Collections.unmodifiableMap(scheduledServices);
    }


    public void shutdown()
    {
        servicesLock.lock();
        try {
            scheduledServices.keySet().stream().collect(toSet()).forEach(this::cancel);
        } finally {
            servicesLock.unlock();
        }
    }


    /**
     * Quickly determine which services not to autoscale anymore, by looking for service names we have but are no longer in the new source.
     * @param currentServiceNames the names of the services we are currently monitoring
     * @param newServices the service names (and their configuration) of the latest update from the service source
     * @return which services (by name) to cancel and no longer monitor
     */
    private Collection<String> getServicesToCancel(final Set<String> currentServiceNames, final Map<String, ScalingConfiguration> newServices)
    {
        Collection<String> toBeCancelled = new HashSet<>();
        for ( String name : currentServiceNames ) {
            if ( !newServices.containsKey(name) ) {
                toBeCancelled.add(name);
            }
        }
        return toBeCancelled;
    }


    /**
     * Determine which services we need to schedule (or re-schedule). This consists of new services (ie. we didn't have a service by this
     * name before) and services that have changed (same id, but their ScalingConfiguration objects do not match).
     * @param currentServices the service names (and their schedule) of everything we are currently monitoring
     * @param newServices the service names (and their configuration) of the latest update from the service source
     * @return which service configurations to schedule (or reschedule)
     */
    private Collection<ScalingConfiguration> getServicesToSchedule(final Map<String, ScheduledScalingService> currentServices, final Map<String, ScalingConfiguration> newServices)
    {
        Collection<ScalingConfiguration> toBeScheduled = new HashSet<>();
        for ( Map.Entry<String, ScalingConfiguration> entry : newServices.entrySet() ) {
            Set<String> current = currentServices.keySet();
            String serviceName = entry.getKey();
            ScalingConfiguration service = entry.getValue();
            if ( !current.contains(serviceName) || !currentServices.get(serviceName).getConfig().equals(service) ) {
                toBeScheduled.add(entry.getValue());
            }
        }
        return toBeScheduled;
    }


    /**
     * Schedule or reschedule a service for workload analysis and scaling.
     * @param config the ScalingConfiguration that describes a service to monitor and scale
     * @param analyser the WorkloadAnalyser that will be used to monitor and scale the service
     * @param initialDelay the initial delay before the scheduled thread kicks off
     */
    private void scheduleOrReschedule(final ScalingConfiguration config, final WorkloadAnalyser analyser, final int initialDelay,
                                      final Alerter alerter)
    {
        LOG.debug("Scheduling service {}", config.getId());
        // if we're already monitoring this service (ie. we're updating it), cancel the current ScalerThread
        if ( scheduledServices.containsKey(config.getId()) ) {
            cancel(config.getId());
        }
        governor.register(config);
        ScheduledFuture future = scheduler.scheduleWithFixedDelay(new ScalerThread(governor, analyser, scaler, config.getId(),
                                                                                   config.getMinInstances(), config.getMaxInstances(),
                                                                                   config.getBackoffAmount(),
                                                                                   alerter,
                                                                                   resourceConfig),
                                                                  initialDelay, config.getInterval(), TimeUnit.SECONDS);
        scheduledServices.put(config.getId(), new ScheduledScalingService(config, future));
    }


    /**
     * Cancel the scheduling of a thread, and remove it from the map of monitored services.
     * @param id the name/reference of the service to stop monitoring
     */
    private void cancel(final String id)
    {
        if ( scheduledServices.containsKey(id) ) {
            LOG.info("Cancelling service {}", id);
            scheduledServices.get(id).getSchedule().cancel(true);
            scheduledServices.remove(id);
            governor.remove(id);
        }
    }


    /**
     * Each service to monitor, described by a ScalingConfiguration, should have a 'workload metric' which is a named
     * reference to a type of WorkloadAnalyser that will be used to monitor and scale the service. Here, try and acquire
     * the WorkloadAnalyser the ScalingConfiguration has requested from the available WorkloadAnalyserFactory objects
     * acquired from the classpath at boot time.
     * @param s the service to get a WorkloadAnalyser for
     * @return the WorkloadAnalyser for this service
     * @throws ScalerException if the requested WorkloadAnalyser is not available
     */
    private WorkloadAnalyser getAnalyser(final ScalingConfiguration s)
            throws ScalerException
    {
        if ( analyserFactories.containsKey(s.getWorkloadMetric()) ) {
            return analyserFactories.get(s.getWorkloadMetric()).getAnalyser(s.getScalingTarget(), s.getScalingProfile());
        } else {
            throw new ScalerException("Invalid workload metric " + s.getWorkloadMetric());
        }
    }

    @Override
    public HealthResult healthCheck()
    {
        final List<ScheduledScalingService> failedServices =
            scheduledServices.values().stream()
                .filter(svc -> svc.getSchedule().isDone())
                .collect(Collectors.toList());

        if (failedServices.isEmpty()) {
            return HealthResult.RESULT_HEALTHY;

        } else {
            return new HealthResult(HealthStatus.UNHEALTHY,
                "Scaling threads have stopped running.  " +
                "The service must be restarted to continue scaling.  " +
                "Affected services: " +
                failedServices.stream()
                    .map(s -> s.getConfig().getId())
                    .collect(Collectors.joining(", ")));
        }
    }

}
