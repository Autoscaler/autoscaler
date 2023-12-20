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
import com.github.autoscaler.api.ScalerException;
import com.github.autoscaler.api.ScalingAction;
import com.github.autoscaler.api.ScalingConfiguration;
import com.github.autoscaler.api.ScalingOperation;
import java.util.Comparator;
import java.util.HashMap;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This object implements a cautious approach to governing the scaling requests for a service.

 */
public class GovernorImpl implements Governor {

    private static final double reduceToPercentage = 0.90;
    private final Map<String, ScalerThread> scalerThreads;
    private final Map<String, AdvancedInstanceInfo> instanceInfoMap;
    private final Map<String, ScalingConfiguration> scalingConfigurationMap;
    private final int stageOneShutdownPriorityLimit;
    private final int stageTwoShutdownPriorityLimit;
    private final int stageThreeShutdownPriorityLimit;
    private static final Logger LOG = LoggerFactory.getLogger(GovernorImpl.class);

    public GovernorImpl(final int stageOneLimit, final int stageTwoLimit, final int stageThreeLimit)
    {
        this.instanceInfoMap = new ConcurrentHashMap<>();
        this.scalingConfigurationMap = new ConcurrentHashMap<>();
        this.stageOneShutdownPriorityLimit = stageOneLimit;
        this.stageTwoShutdownPriorityLimit = stageTwoLimit;
        this.stageThreeShutdownPriorityLimit = stageThreeLimit;
        this.scalerThreads = new HashMap<>();
    }

    @Override
    public boolean freeUpResourcesForService(final String serviceRef)
    {
        final AdvancedInstanceInfo lastInstanceInfo = instanceInfoMap.get(serviceRef);
        if (lastInstanceInfo == null) {
            LOG.error("Failed to retrieve service information for {} from internal map", serviceRef);
            return false;
        }
        final double relativeDifference = lastInstanceInfo.getRelativeDifference();
        final String candidate = instanceInfoMap.entrySet().stream()
            .filter(e -> e.getValue().getRelativeDifference() < relativeDifference
                && scalingConfigurationMap.get(e.getKey()).getMinInstances() < e.getValue().getTotalRunningAndStageInstances())
            .sorted(Comparator.comparingDouble(e -> e.getValue().getRelativeDifference()))
            .map(e -> e.getKey())
            .findFirst()
            .orElse(null);
        if (candidate == null) {
            LOG.info("Unable to make room for application {} as all other applications have a higher percentage difference of current "
                + "instances to their desired instances", serviceRef);
            return false;
        }
        LOG.info("Attempting to scale down service {} to make room for service {}...", candidate, serviceRef);
        try {
            scalerThreads.get(candidate).scaleDownNow();
        } catch (final ScalerException ex) {
            LOG.error("Unable to scale down {} to make room for {} due to exception.", candidate, serviceRef, ex);
            return false;
        }
        return true;
    }

    @Override
    public void register(ScalingConfiguration scalingConfiguration) {
        scalingConfigurationMap.put(scalingConfiguration.getId(), scalingConfiguration);
    }

    @Override
    public void registerListener(final String serviceRef, final ScalerThread thread)
    {
        scalerThreads.put(serviceRef, thread);
    }

    @Override
    public void recordInstances(String serviceRef, InstanceInfo instances) {
        instanceInfoMap.put(serviceRef, new AdvancedInstanceInfo(instances));
    }

    @Override
    public void remove(String serviceRef) {
        instanceInfoMap.remove(serviceRef);
        scalingConfigurationMap.remove(serviceRef);
    }

    @Override
    public ScalingAction govern(String serviceRef, ScalingAction action, final ResourceLimitStagesReached resourceLimitStagesReached) {

        ScalingConfiguration scalingConfiguration = scalingConfigurationMap.getOrDefault(serviceRef, null);
        final AdvancedInstanceInfo lastInstanceInfo = instanceInfoMap.getOrDefault(serviceRef, null);

        if(scalingConfiguration==null){
            throw new RuntimeException(String.format("Scaling configuration not found for {%s}", serviceRef));
        }
        if (lastInstanceInfo == null) {
            return action;
        }
        lastInstanceInfo.setDesiredInstances(action);
        final boolean otherServicesMinimumInstancesMet = otherServicesMinimumInstancesMet(serviceRef, resourceLimitStagesReached);

        switch(action.getOperation()){
            case NONE: {
                if (lastInstanceInfo.getTotalRunningAndStageInstances() < scalingConfiguration.getMinInstances()) {
                    final int instanceDelta = scalingConfiguration.getMinInstances() - lastInstanceInfo.getTotalRunningAndStageInstances();
                    LOG.debug("Action is currently set to NONE however service {} is currently running less than it's configured minimum"
                        + " number instances. For this reason action has been reset to SCALE UP by {} instances", serviceRef,
                              instanceDelta);
                    return new ScalingAction(ScalingOperation.SCALE_UP, instanceDelta);
                } else if (lastInstanceInfo.getTotalRunningAndStageInstances() > scalingConfiguration.getMaxInstances()) {
                    final int instanceDelta = lastInstanceInfo.getTotalRunningAndStageInstances() - scalingConfiguration.getMaxInstances();
                    LOG.debug("Action is currently set to NONE however service {} is currently running more than it's configured maximum"
                        + " number instances. For this reason action has been reset to SCALE DOWN by {} instances", serviceRef,
                              instanceDelta);
                    return new ScalingAction(ScalingOperation.SCALE_DOWN, instanceDelta);
                }
                break;
            }
            case SCALE_UP:
            {
                if (lastInstanceInfo.getTotalRunningAndStageInstances() > scalingConfiguration.getMaxInstances()) {
                    final int instanceDelta = lastInstanceInfo.getTotalRunningAndStageInstances() - scalingConfiguration.getMaxInstances();
                    LOG.debug("Action is currently set to SCALE_UP however service {} is currently running more than it's configured "
                        + "maximum number instances. For this reason action has been reset to SCALE DOWN by {} instances", serviceRef,
                              instanceDelta);
                    return new ScalingAction(ScalingOperation.SCALE_DOWN,
                                             lastInstanceInfo.getTotalRunningAndStageInstances() - scalingConfiguration.getMaxInstances());
                } else if (!otherServicesMinimumInstancesMet) {
                    if (lastInstanceInfo.getTotalRunningAndStageInstances() < scalingConfiguration.getMinInstances()) {
                    final int instanceDelta = scalingConfiguration.getMinInstances() - lastInstanceInfo.getTotalRunningAndStageInstances();
                        LOG.debug("Action is currently set to SCALE_UP however not all services have reached their minimum instances yet."
                            + "Service scaling is usually disabled during this time however service {} is currently running less than "
                            + "it's configured minimum number instances. The scaling command has been overriden to scale the service "
                            + "to its minimum. New command is SCALE_UP by {} instances", serviceRef, instanceDelta);
                        return new ScalingAction(ScalingOperation.SCALE_UP, instanceDelta);
                    } else if (lastInstanceInfo.getTotalRunningAndStageInstances() == scalingConfiguration.getMinInstances()) {
                        LOG.debug("Action is currently set to SCALE_UP however not all services have reached their minimum instances yet."
                            + "Service {} is currently running at it's configured minimum instances, for this reason scaling action has "
                            + "been overriden and no scaling action will occur for this service.", serviceRef);
                        return new ScalingAction(ScalingOperation.NONE, 0);
                    } else if (lastInstanceInfo.getTotalRunningAndStageInstances() > scalingConfiguration.getMinInstances()) {
                        //Gradually reduce the totalInstances by a percentage until Minimums are met.
                        //This should be configurable, however there should be a Governor specific configuration
                        //to allow different Governor implementations to be added without polluting the AutoscaleConfiguration

                        int target = Math.max(scalingConfiguration.getMinInstances(),
                                              (int) Math.floor(lastInstanceInfo.getTotalRunningAndStageInstances() * reduceToPercentage));
                        int amount = lastInstanceInfo.getTotalRunningAndStageInstances() - target;
                        LOG.debug("Action is currently set to SCALE_UP however not all services have reached their minimum instances yet."
                            + "Service scaling is disabled during this time. Service {} is currently running more than it's configured "
                            + "minimum number instances, due to the scaling restriction this will be reduced to attempt to allow other "
                            + "services to reach their minimum. The scaling command has been overriden to scale the service "
                            + "down. New command is SCALE_DOWN by {} instances", serviceRef, amount);
                        return new ScalingAction(ScalingOperation.SCALE_DOWN, amount);
                    }
                } else {
                    final int delta = Math.min(scalingConfiguration.getMaxInstances() - lastInstanceInfo.getTotalRunningAndStageInstances(),
                                                Math.max(0, action.getAmount()));
                    if(delta < action.getAmount()){
                        LOG.info("Service {} requested {} more instances but will only be scaled by {} as the service has a max instance "
                            + "restriction of {} instances.", serviceRef, action.getAmount(), delta,
                                 scalingConfiguration.getMaxInstances());
                    }
                    return new ScalingAction(ScalingOperation.SCALE_UP, delta);
                }
                break;
            }
            case SCALE_DOWN: {
                if (lastInstanceInfo.getTotalRunningAndStageInstances() == scalingConfiguration.getMinInstances()) {
                    LOG.debug("Action is currently set to SCALE_DOWN however service {} is currently running at its configured minimum "
                        + "instances. Action overriden to take no action.", serviceRef);
                    return new ScalingAction(ScalingOperation.NONE, 0);
                } else if (lastInstanceInfo.getTotalRunningAndStageInstances() < scalingConfiguration.getMinInstances()) {
                    final int instanceDelta = scalingConfiguration.getMinInstances() - lastInstanceInfo.getTotalRunningAndStageInstances();
                    LOG.debug("Action is currently set to SCALE_DOWN however service {} is currently running less than it's configured "
                        + "minimum number instances. For this reason action has been reset to SCALE UP by {} instances", serviceRef,
                              instanceDelta);
                    return new ScalingAction(ScalingOperation.SCALE_UP, instanceDelta);
                } else if ((lastInstanceInfo.getTotalRunningAndStageInstances() - action.getAmount()) < scalingConfiguration.getMinInstances()) {
                    LOG.debug("Action is currently set to scale service {} down by {} instances, this would leave the service below its "
                        + "configured minimum number of instances. Action overriden to scale the service down to its minimum instances");
                    return new ScalingAction(ScalingOperation.SCALE_DOWN,
                                             lastInstanceInfo.getTotalRunningAndStageInstances() - scalingConfiguration.getMinInstances());
                }
                break;
            }
        }
        return action;
    }

    /**
     * Determine if the other services have met their minimum instances.
     * @param serviceRef the service identifier to exclude from the evaluation
     * @return True if other services have met their minimum instance requirement
     */
    private boolean otherServicesMinimumInstancesMet(String serviceRef, final ResourceLimitStagesReached resourceLimitStagesReached){
        for(String key:scalingConfigurationMap.keySet()){
            if(key.equals(serviceRef)){
                continue;
            }
            ScalingConfiguration scalingConfiguration = scalingConfigurationMap.getOrDefault(key, null);
            final AdvancedInstanceInfo lastInstanceInfo = instanceInfoMap.getOrDefault(key, null);

            //If there are no instance info be cautious and assume minimum instances have not been met.
            if (lastInstanceInfo == null) {
                return false;
            }
            if(lastInstanceInfo.getTotalRunningAndStageInstances() < scalingConfiguration.getMinInstances()){
                if(shouldBeScaledDown(resourceLimitStagesReached, lastInstanceInfo.getShutdownPriority())){
                   continue;
                }
                return false;
            }
        }
        return true;
    }

    private boolean shouldBeScaledDown(final ResourceLimitStagesReached resourceLimitStagesReached, final int shutdownPriority)
    {
        if (shutdownPriority == -1) {
            return false;
        }

        final ResourceLimitStage highestResourceLimitStageReached = ResourceLimitStage.max(
                resourceLimitStagesReached.getMemoryLimitStageReached(),
                resourceLimitStagesReached.getDiskLimitStageReached());

        switch (highestResourceLimitStageReached) {
            case STAGE_1: {
                return shutdownPriority <= stageOneShutdownPriorityLimit;
            }
            case STAGE_2: {
                return shutdownPriority <= stageTwoShutdownPriorityLimit;
            }
            case STAGE_3: {
                return shutdownPriority <= stageThreeShutdownPriorityLimit;
            }
            default: {
                return false;
            }
        }
    }

    private static final class AdvancedInstanceInfo extends InstanceInfo
    {
        private int desiredInstances;

        private AdvancedInstanceInfo(final InstanceInfo instanceInfo)
        {
            super(instanceInfo.getInstancesRunning(), instanceInfo.getInstancesStaging(), instanceInfo.getHosts(),
                  instanceInfo.getShutdownPriority(), instanceInfo.getInstances());
            this.desiredInstances = 0;
        }

        public int getDesiredInstances()
        {
            return desiredInstances;
        }

        public void setDesiredInstances(final ScalingAction action)
        {
            switch (action.getOperation()) {
                case NONE:
                    this.desiredInstances = getTotalRunningAndStageInstances();
                    break;
                case SCALE_UP:
                    this.desiredInstances = getInstancesRunning() + action.getAmount();
                    break;
                case SCALE_DOWN:
                    this.desiredInstances = getInstancesRunning() - action.getAmount();
                    break;
            }
        }

        /**
         * Calculates the relative difference between the current number of instances running and the desired number of instances required 
         * to fullfil the workload currently on the service within the next five minutes.
         * <p>
         * Example:
         * <pre>
         * If a worker currently has 5 running instances and it desires to have 4 based on its workload at present then it will have a 
         * relative difference of 0.8
         * 
         * Further examples can be found below.
         * 
         * |-------------------|-------------------|-----------------------------------------|
         * | Current Instances | Desired Instances | Relative Difference (Desired / Current) |
         * |-------------------|-------------------|-----------------------------------------|
         * |        5          |        4          |                  0.8                    |
         * |        5          |        5          |                   1                     |
         * |        5          |        15         |                   3                     |
         * |        5          |        0          |                   0                     |
         * |        0          |        5          |           Positive Infinity             |
         * |        0          |        0          |           Positive Infinity             |
         * |-------------------|-------------------|-----------------------------------------|
         * </pre>
         */
        public double getRelativeDifference()
        {
            return getTotalRunningAndStageInstances() == 0 && this.desiredInstances == 0
                ? 1.0 / 0
                : (double) this.desiredInstances / getTotalRunningAndStageInstances();
        }
    }
}
