/*
 * Copyright 2015-2024 Open Text.
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
package com.github.autoscaler.core;

import com.github.autoscaler.api.InstanceInfo;
import com.github.autoscaler.api.QueueNotFoundException;
import com.github.autoscaler.api.ResourceUtilisation;
import com.github.autoscaler.api.ScalerException;
import com.github.autoscaler.api.ScalingAction;
import com.github.autoscaler.api.ScalingOperation;
import com.github.autoscaler.api.ServiceScaler;
import com.github.autoscaler.api.WorkloadAnalyser;
import java.text.DecimalFormat;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Objects;
import java.util.Optional;

/**
 * A ScalerThread is responsible for calling out to a WorkloadAnalyser, taking its recommendation and then acting appropriately,
 * potentially including a call out to a ServiceScaler. These threads are run periodically by a scheduled executor in the autoscaler
 * application, and there is one per service being scaled.
 */
public class ScalerThread implements Runnable
{

    private final DecimalFormat df = new DecimalFormat("##.00");
    private final Alerter memoryOverloadAlerter;
    private final Alerter diskSpaceLowAlerter;
    private final WorkloadAnalyser analyser;
    private final ServiceScaler scaler;
    private final int backoffAmount;
    private final int scaleDownBackoffAmount;
    private final int scaleUpBackoffAmount;
    private final String serviceRef;
    private int backoffCount = 0;
    private boolean backoff = false;
    private static final Logger LOG = LoggerFactory.getLogger(ScalerThread.class);

    private final Governor governor;
    private final ResourceMonitoringConfiguration resourceConfig;

    private ScalingOperation lastOperation;

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
     * @param memoryOverloadAlerter dispatcher to send memory overload alerts if required
     * @param diskSpaceLowAlerter dispatcher to send disk space low alerts if required
     */
    public ScalerThread(final Governor governor, final WorkloadAnalyser workloadAnalyser, final ServiceScaler serviceScaler,
                        final String serviceReference, final int minInstances, final int maxInstances, final int backoffAmount,
                        final Alerter memoryOverloadAlerter, final Alerter diskSpaceLowAlerter,
                        final ResourceMonitoringConfiguration resourceConfig)
    {
        this(governor, workloadAnalyser, serviceScaler, serviceReference, minInstances, maxInstances, backoffAmount, -1, -1,
                memoryOverloadAlerter, diskSpaceLowAlerter, resourceConfig);
    }

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
     * @param scaleUpBackoffAmount the number of analysis runs to skip after a scaling up is triggered
     * @param scaleDownBackoffAmount the number of analysis runs to skip after a scaling down is triggered
     * @param memoryOverloadAlerter dispatcher to send memory overload alerts if required
     * @param diskSpaceLowAlerter dispatcher to send disk space low alerts if required
     */
    public ScalerThread(final Governor governor, final WorkloadAnalyser workloadAnalyser, final ServiceScaler serviceScaler,
                        final String serviceReference, final int minInstances, final int maxInstances, final int backoffAmount,
                        final int scaleUpBackoffAmount, final int scaleDownBackoffAmount, final Alerter memoryOverloadAlerter,
                        final Alerter diskSpaceLowAlerter, final ResourceMonitoringConfiguration resourceConfig)
    {
        this.resourceConfig = resourceConfig;
        this.scaleUpBackoffAmount = scaleUpBackoffAmount;
        this.scaleDownBackoffAmount = scaleDownBackoffAmount;
        this.memoryOverloadAlerter = memoryOverloadAlerter;
        this.diskSpaceLowAlerter = diskSpaceLowAlerter;
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
        if (isShouldBackoff()) {
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
            final ResourceUtilisation resourceUtilisation = analyser.getCurrentResourceUtilisation();
            LOG.debug("Resource utilisation for service {}: {}", serviceRef, resourceUtilisation);
            final ResourceLimitStagesReached resourceLimitStagesReached = establishResourceLimitStagesReached(resourceUtilisation);
            LOG.debug("Resource limit stages reached for service {}: {}", serviceRef, resourceLimitStagesReached);
            InstanceInfo instances = scaler.getInstanceInfo(serviceRef);
            LOG.debug("Instance info for service {}: {}", serviceRef, instances);
            final int shutdownPriority = instances.getShutdownPriority();
            if (handleResourceLimitReached(instances, resourceUtilisation, resourceLimitStagesReached, shutdownPriority)) {
                return;
            }
            governor.recordInstances(serviceRef, instances);
            ScalingAction action;
            LOG.debug("Performing scaling checks for service {}", serviceRef);
            action = analyser.analyseWorkload(instances);
            LOG.debug("Workload Analyser determined that the autoscaler should {} {} by {} instances",
                     action.getOperation(), serviceRef, action.getAmount());
            action = governor.govern(serviceRef, action, resourceLimitStagesReached);
            LOG.debug("Governor determined that the autoscaler should {} {} by {} instances",
                     action.getOperation(), serviceRef, action.getAmount());
            if (action.getAmount() == 0) {
                return;
            }
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
        } catch (final QueueNotFoundException e) {
            LOG.warn("Queue not found {}", serviceRef);
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
        LOG.debug("Attempting scale up of service {} by amount {}", serviceRef, amount);
        scaler.scaleUp(serviceRef, amount);
        lastOperation = ScalingOperation.SCALE_UP;
        try {
            InstanceInfo refreshedInsanceInfo = scaler.getInstanceInfo(serviceRef);
            while (refreshedInsanceInfo.getInstances() > refreshedInsanceInfo.getTotalRunningAndStageInstances()) {
                boolean instanceMet = false;
                for (int i = 1; i <= 6; i++) {
                    final int sleep = i * 10 * 1000;
                    LOG.debug("Sleeping for {} to allow instances to come up.", sleep);
                    Thread.sleep(sleep);
                    refreshedInsanceInfo = scaler.getInstanceInfo(serviceRef);
                    if (refreshedInsanceInfo.getTotalRunningAndStageInstances() == refreshedInsanceInfo.getInstances()) {
                        instanceMet = true;
                        break;
                    }
                }
                if (!instanceMet) {
                    if (!governor.freeUpResourcesForService(serviceRef)) {
                        throw new ScalerException(
                            "Unable to scale service " + serviceRef + " due to an inability to make room for it on the orchestrator.");
                    }
                }
            }
        } catch (final InterruptedException ex) {
            // Set interupted flag
            Thread.currentThread().interrupt();
            // Throw exception to suppress further calls from current scaler thread until after scaler thread refresh
            throw new ScalerException("An error occured during an attempt to have the main thread sleep before rechecking the number"
                + " of instance present for the application.", ex);
        }
        LOG.info("Service {} scaled up by {} instances", serviceRef, amount);
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
        LOG.debug("Attempting scale down of service {} by {} instances", serviceRef, amount);
        scaler.scaleDown(serviceRef, amount);
        lastOperation = ScalingOperation.SCALE_DOWN;
        try {
            Thread.sleep(1000);
        } catch (InterruptedException ex) {
            // Set interupted flag
            Thread.currentThread().interrupt();
            // Throw exception to suppress further calls from current scaler thread until after scaler thread refresh
            throw new ScalerException("An error occured during an attempt to have the main thread sleep before rechecking the number"
                + " of instance present for the application.", ex);
        }
        LOG.info("Service {} scaled down by {} instances.", serviceRef, amount);
        final InstanceInfo info = scaler.getInstanceInfo(serviceRef);
        governor.recordInstances(serviceRef, info);
        backoff = true;
    }

    public void scaleDownNow() throws ScalerException
    {
        scaleDown(1);
    }

    private boolean handleResourceLimitReached(
            final InstanceInfo instances,
            final ResourceUtilisation resourceUtilisation,
            final ResourceLimitStagesReached resourceLimitStagesReached,
            final int shutdownPriority)
        throws ScalerException
    {
        if (shutdownPriority == -1) {
            return false;
        }

        handleAlerterDispatch(resourceUtilisation);

        final ResourceLimitStage highestResourceLimitStageReached = ResourceLimitStage.max(
                resourceLimitStagesReached.getMemoryLimitStageReached(),
                resourceLimitStagesReached.getDiskLimitStageReached());

        if (highestResourceLimitStageReached == ResourceLimitStage.STAGE_1 &&
                shutdownPriority <= resourceConfig.getResourceLimitOneShutdownThreshold()) {
            LOG.warn("Attempting to scale down service {} due to resource limit stage 1 being reached and " +
                            "shutdownPriority {} <= resourceLimitOneShutdownThreshold {}. The current resource limit stage is " +
                            "determined by the highest resource limit stage among: {}",
                    serviceRef,
                    shutdownPriority,
                    resourceConfig.getResourceLimitOneShutdownThreshold(),
                    resourceLimitStagesReached);

            scaleDown(instances.getTotalRunningAndStageInstances());
            return true;
        } else if (highestResourceLimitStageReached == ResourceLimitStage.STAGE_2 &&
                shutdownPriority <= resourceConfig.getResourceLimitTwoShutdownThreshold()) {
            LOG.warn("Attempting to scale down service {} due to resource limit stage 2 being reached and " +
                            "shutdownPriority {} <= resourceLimitTwoShutdownThreshold {}. The current resource limit stage is " +
                            "determined by the highest resource limit stage among: {}",
                    serviceRef,
                    shutdownPriority,
                    resourceConfig.getResourceLimitTwoShutdownThreshold(),
                    resourceLimitStagesReached);

            scaleDown(instances.getTotalRunningAndStageInstances());
            return true;
        } else if (highestResourceLimitStageReached == ResourceLimitStage.STAGE_3 &&
                shutdownPriority <= resourceConfig.getResourceLimitThreeShutdownThreshold()) {
            LOG.warn("Attempting to scale down service {} due to resource limit stage 3 being reached and " +
                            "shutdownPriority {} <= resourceLimitThreeShutdownThreshold {}. The current resource limit stage is " +
                            "determined by the highest resource limit stage among: {}",
                    serviceRef,
                    shutdownPriority,
                    resourceConfig.getResourceLimitThreeShutdownThreshold(),
                    resourceLimitStagesReached);

            scaleDown(instances.getTotalRunningAndStageInstances());
            return true;
        }
        return false;
    }

    private void handleAlerterDispatch(final ResourceUtilisation resourceUtilisation) throws ScalerException
    {
        final double memoryUsedPercent = resourceUtilisation.getMemoryUsedPercent();
        if (memoryUsedPercent >= resourceConfig.getMemoryUsedPercentAlertDispatchThreshold()) {
            final String memoryOverloadWarningEmailBody = analyser.getMemoryOverloadWarning(df.format(memoryUsedPercent));
            memoryOverloadAlerter.dispatchAlert(memoryOverloadWarningEmailBody);
        }

        final Optional<Integer> diskFreeMbOpt = resourceUtilisation.getDiskFreeMbOpt();
        if (diskFreeMbOpt.isPresent() && diskFreeMbOpt.get() <= resourceConfig.getDiskFreeMbAlertDispatchThreshold()) {
            final String diskLowWarningEmailBody = analyser.getDiskSpaceLowWarning(df.format(diskFreeMbOpt.get()));
            diskSpaceLowAlerter.dispatchAlert(diskLowWarningEmailBody);
        }
    }

    private ResourceLimitStagesReached establishResourceLimitStagesReached(final ResourceUtilisation resourceUtilisation)
    {
        final double memoryUsedPercent = resourceUtilisation.getMemoryUsedPercent();
        final ResourceLimitStage memoryLimitStageReached;
        if (memoryUsedPercent >= resourceConfig.getMemoryUsedPercentLimitStageThree()) {
            memoryLimitStageReached = ResourceLimitStage.STAGE_3;
        } else if (memoryUsedPercent >= resourceConfig.getMemoryUsedPercentLimitStageTwo()) {
            memoryLimitStageReached = ResourceLimitStage.STAGE_2;
        } else if (memoryUsedPercent >= resourceConfig.getMemoryUsedPercentLimitStageOne()) {
            memoryLimitStageReached = ResourceLimitStage.STAGE_1;
        } else {
            memoryLimitStageReached = ResourceLimitStage.NO_STAGE;
        }

        final Optional<Integer> diskFreeMbOpt = resourceUtilisation.getDiskFreeMbOpt();
        final ResourceLimitStage diskLimitStageReached;
        if (diskFreeMbOpt.isPresent()) {
            final int diskFreeMb = diskFreeMbOpt.get();
            if (diskFreeMb <= resourceConfig.getDiskFreeMbLimitStageThree()) {
                diskLimitStageReached = ResourceLimitStage.STAGE_3;
            } else if (diskFreeMb <= resourceConfig.getDiskFreeMbLimitStageTwo()) {
                diskLimitStageReached = ResourceLimitStage.STAGE_2;
            } else if (diskFreeMb <= resourceConfig.getDiskFreeMbLimitStageOne()) {
                diskLimitStageReached = ResourceLimitStage.STAGE_1;
            } else {
                diskLimitStageReached = ResourceLimitStage.NO_STAGE;
            }
        } else {
            diskLimitStageReached = ResourceLimitStage.NO_STAGE;
        }

        return new ResourceLimitStagesReached(memoryLimitStageReached, diskLimitStageReached);
    }

    private boolean isShouldBackoff()
    {
        if (!backoff) {
            return false;
        }
        final int backoffLimit;
        backoffCount++;
        switch (lastOperation) {
            case SCALE_DOWN: {
                backoffLimit = scaleUpBackoffAmount == -1 ? backoffAmount : scaleDownBackoffAmount;
                LOG.debug("Last Action was scale down, setting backoff amount to " + backoffLimit);
                break;
            }
            case SCALE_UP: {
                backoffLimit = scaleUpBackoffAmount == -1 ? backoffAmount : scaleUpBackoffAmount;
                LOG.debug("Last Action was scale up, setting backoff amount to " + backoffLimit);
                break;
            }
            default: {
                backoffLimit = backoffAmount;
                LOG.debug("Setting backoff amount to " + backoffLimit);
                break;
            }
        }

        if (backoffCount > backoffLimit) {
            backoff = false;
            backoffCount = 0;
            return false;
        }
        return true;
    }
}
