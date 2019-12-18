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

import com.hpe.caf.api.autoscale.InstanceInfo;
import com.hpe.caf.api.autoscale.ScalerException;
import com.hpe.caf.api.autoscale.ScalingAction;
import com.hpe.caf.api.autoscale.ServiceScaler;
import com.hpe.caf.api.autoscale.WorkloadAnalyser;
import java.text.DecimalFormat;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Objects;

/**
 * A ScalerThread is responsible for calling out to a WorkloadAnalyser, taking its recommendation and then acting appropriately,
 * potentially including a call out to a ServiceScaler. These threads are run periodically by a scheduled executor in the autoscaler
 * application, and there is one per service being scaled.
 */
public class ScalerThread implements Runnable
{

    private final DecimalFormat df = new DecimalFormat("##.00");
    private final Alerter alertDispatcher;
    private final WorkloadAnalyser analyser;
    private final ServiceScaler scaler;
    private final int backoffAmount;
    private final String serviceRef;
    private int backoffCount = 0;
    private boolean backoff = false;
    private static final Logger LOG = LoggerFactory.getLogger(ScalerThread.class);

    private final Governor governor;
    private final ResourceMonitoringConfiguration resourceConfig;

    /**
     * Create a new ScalerThread.
     *
     * @param governor a Governor instance to prevent one service from starving others
     * @param workloadAnalyser the method for this thread to analyse the workload of a service
     * @param serviceScaler the method for this thread to scale a service
     * @param serviceReference the named reference to the service this thread will analyse and scale
     * @param minInstances the minimum number of instances of the service that must be instantiated
     * @param maxInstances the maximum number of instances of the service that can be instantiated
     * @param backoffAmount the number of analysis runs to skip after a scaling is triggered
     * @param alertDispatcher dispatcher to send alerts if required
     */
    public ScalerThread(final Governor governor, final WorkloadAnalyser workloadAnalyser, final ServiceScaler serviceScaler,
                        final String serviceReference, final int minInstances, final int maxInstances, final int backoffAmount,
                        final Alerter alertDispatcher, final ResourceMonitoringConfiguration resourceConfig)
    {
        this.resourceConfig = resourceConfig;
        this.alertDispatcher = alertDispatcher;
        this.governor = governor;
        this.analyser = Objects.requireNonNull(workloadAnalyser);
        this.scaler = Objects.requireNonNull(serviceScaler);
        this.serviceRef = Objects.requireNonNull(serviceReference);
        if (minInstances < 0 || maxInstances < 1) {
            throw new IllegalArgumentException("Instance count limits invalid");
        }
        this.backoffAmount = backoffAmount;
    }

    /**
     * Determine whether to trigger an analysis run or not, depending on the current backoff state.
     */
    @Override
    public void run()
    {
        if (backoff) {
            backoffCount++;
            if (backoffCount > backoffAmount) {
                backoff = false;
                backoffCount = 0;
            }
        }

        if (backoff) {
            LOG.debug("Not performing workload analysis for service {}, backing off", serviceRef);
        } else {
            LOG.debug("Workload analysis run for service {}", serviceRef);
            handleAnalysis();
        }
    }

    /**
     * Perform an analysis run. This always begins with getting the current information about the service this thread is responsible for,
     * then taking action. For the very first run, the thread will ensure the current number of instances meets the basic criteria it has
     * been given. On subsequent runs, recommendations on scaling will be retrieved from the WorkloadAnalyser and acted upon (with
     * limitations such as min/max instances). Exceptions will fail a single run of this thread, but will not halt subsequent runs.
     */
    private void handleAnalysis()
    {
        try {
            InstanceInfo instances = scaler.getInstanceInfo(serviceRef);
            if (handleMemoryLoadIssues(instances)) {
                return;
            }
            governor.recordInstances(serviceRef, instances);
            ScalingAction action;
            LOG.debug("Performing scaling checks for service {}", serviceRef);
            action = analyser.analyseWorkload(instances);
            LOG.info("Workload Analyser determined that the autoscaler should {} {} by {} instances",
                     action.getOperation(), serviceRef, action.getAmount());
            action = governor.govern(serviceRef, action);
            LOG.info("Governor determined that the autoscaler should {} {} by {} instances",
                     action.getOperation(), serviceRef, action.getAmount());

            switch (action.getOperation()) {
                case SCALE_UP:
                    scaleUp(action.getAmount());
                    break;
                case SCALE_DOWN:
                    scaleDown(action.getAmount());
                    break;
                case NONE:
                default:
                    break;
            }
        } catch (ScalerException e) {
            LOG.warn("Failed analysis run for service {}", serviceRef, e);
        } catch (final RuntimeException e) {
            // library methods have been known to throw RuntimeException when there's no programming
            // error - but if we throw, we won't be scheduled to run again, so we must catch and
            // ignore
            LOG.error("Unexpected error in analysis run for service {}", serviceRef, e);
        } catch (final Throwable e) {
            // if the thread throws, the error isn't logged
            LOG.error("Unexpected error in analysis run for service {}.  The scheduler will now stop; the service must be restarted to continue scaling.", serviceRef, e);
            throw e;
        }
    }

    /**
     * Perform a scale up
     *
     * @param instances information on the current number of instances of a service
     * @param amount the requested number of instances to scale up by
     * @throws ScalerException if the scaling operation fails
     */
    private void scaleUp(final int amount)
        throws ScalerException
    {
        LOG.debug("Triggering scale up of service {} by amount {}", serviceRef, amount);
        scaler.scaleUp(serviceRef, amount);
        backoff = true;
    }

    /**
     * Perform a scale down
     *
     * @param instances information on the current number of instances of a service
     * @param amount the requested number of instances to scale down by
     * @throws ScalerException if the scaling operation fails
     */
    private void scaleDown(final int amount)
        throws ScalerException
    {
        LOG.debug("Triggering scale down of service {} by amount {}", serviceRef, amount);
        scaler.scaleDown(serviceRef, amount);
        backoff = true;
    }

    private boolean handleMemoryLoadIssues(final InstanceInfo instances) throws ScalerException
    {
        final double currentMemoryLoad = analyser.getCurrentMemoryLoad();
        final int shutdownPriority = instances.getShutdownPriority();

        if (shutdownPriority == -1) {
            return false;
        }

        handleAlerterDispatch(currentMemoryLoad);

        if (currentMemoryLoad >= resourceConfig.getResourceLimitOne()
            && shutdownPriority <= resourceConfig.getResourceLimitOneShutdownThreshold()) {
            scaleDown(instances.getTotalInstances());
            return true;
        } else if (currentMemoryLoad >= resourceConfig.getResourceLimitTwo()
            && shutdownPriority <= resourceConfig.getResourceLimitTwoShutdownThreshold()) {
            scaleDown(instances.getTotalInstances());
            return true;
        } else if (currentMemoryLoad >= resourceConfig.getResourceLimitThree()
            && shutdownPriority <= resourceConfig.getResourceLimitThreeShutdownThreshold()) {
            scaleDown(instances.getTotalInstances());
            return true;
        }
        return false;
    }

    private void handleAlerterDispatch(final double memLoad) throws ScalerException
    {
        if (memLoad > resourceConfig.getAlertDispatchThreshold()) {
            final String emailBody = analyser.getMemoryOverloadWarning(df.format(memLoad));
            alertDispatcher.dispatchAlert(emailBody);
        }
    }
}
