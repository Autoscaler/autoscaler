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
import com.hpe.caf.api.autoscale.ScalingAction;
import com.hpe.caf.api.autoscale.ScalingConfiguration;
import com.hpe.caf.api.autoscale.ScalingOperation;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * This object implements a cautious approach to governing the scaling requests for a service.

 */
public class GovernorImpl implements Governor {

    private static final double reduceToPercentage = 0.90;
    private final Map<String, InstanceInfo> instanceInfoMap;
    private final Map<String, ScalingConfiguration> scalingConfigurationMap;
    private final int stageOneShutdownPriorityLimit;
    private final int stageTwoShutdownPriorityLimit;
    private final int stageThreeShutdownPriorityLimit;

    public GovernorImpl(final int stageOneLimit, final int stageTwoLimit, final int stageThreeLimit)
    {
        this.instanceInfoMap = new ConcurrentHashMap<>();
        this.scalingConfigurationMap = new ConcurrentHashMap<>();
        this.stageOneShutdownPriorityLimit = stageOneLimit;
        this.stageTwoShutdownPriorityLimit = stageTwoLimit;
        this.stageThreeShutdownPriorityLimit = stageThreeLimit;
    }

    @Override
    public void register(ScalingConfiguration scalingConfiguration) {
        scalingConfigurationMap.put(scalingConfiguration.getId(), scalingConfiguration);
    }

    @Override
    public void recordInstances(String serviceRef, InstanceInfo instances) {
        instanceInfoMap.put(serviceRef, instances);
    }

    @Override
    public void remove(String serviceRef) {
        instanceInfoMap.remove(serviceRef);
        scalingConfigurationMap.remove(serviceRef);
    }

    @Override
    public ScalingAction govern(String serviceRef, ScalingAction action, final int shutdownPriority, final int currentMemoryLimitStage) {

        ScalingConfiguration scalingConfiguration = scalingConfigurationMap.getOrDefault(serviceRef, null);
        InstanceInfo lastInstanceInfo = instanceInfoMap.getOrDefault(serviceRef, null);

        if(scalingConfiguration==null){
            throw new RuntimeException(String.format("Scaling configuration not found for {%s}", serviceRef));
        }
        if (lastInstanceInfo == null) {
            return action;
        }
        final boolean otherServicesMinimumInstancesMet = otherServicesMinimumInstancesMet(serviceRef);

        switch(action.getOperation()){
            case NONE: {
                if (lastInstanceInfo.getTotalInstances() < scalingConfiguration.getMinInstances()) {
                    return new ScalingAction(ScalingOperation.SCALE_UP,
                                             scalingConfiguration.getMinInstances() - lastInstanceInfo.getTotalInstances());
                } else if (lastInstanceInfo.getTotalInstances() > scalingConfiguration.getMaxInstances()) {
                    return new ScalingAction(ScalingOperation.SCALE_DOWN,
                                             lastInstanceInfo.getTotalInstances() - scalingConfiguration.getMaxInstances());
                }
                break;
            }
            case SCALE_UP:
            {
                if (lastInstanceInfo.getTotalInstances() > scalingConfiguration.getMaxInstances()) {
                    return new ScalingAction(ScalingOperation.SCALE_DOWN,
                                             lastInstanceInfo.getTotalInstances() - scalingConfiguration.getMaxInstances());
                } else if (!otherServicesMinimumInstancesMet) {
                    if (lastInstanceInfo.getTotalInstances() < scalingConfiguration.getMinInstances()) {
                        return new ScalingAction(ScalingOperation.SCALE_UP,
                                                 scalingConfiguration.getMinInstances() - lastInstanceInfo.getTotalInstances());
                    } else if (lastInstanceInfo.getTotalInstances() == scalingConfiguration.getMinInstances()) {
                        return new ScalingAction(ScalingOperation.NONE, 0);
                    } else if (lastInstanceInfo.getTotalInstances() > scalingConfiguration.getMinInstances()
                        && shouldGovern(currentMemoryLimitStage, shutdownPriority)) {
                        //Gradually reduce the totalInstances by a percentage until Minimums are met.
                        //This should be configurable, however there should be a Governor specific configuration
                        //to allow different Governor implementations to be added without polluting the AutoscaleConfiguration

                        int target = Math.max(scalingConfiguration.getMinInstances(),
                                              (int) Math.floor(lastInstanceInfo.getTotalInstances() * reduceToPercentage));
                        int amount = lastInstanceInfo.getTotalInstances() - target;

                        return new ScalingAction(ScalingOperation.SCALE_DOWN, amount);
                    }
                } else {
                    final int delta = Math.min(scalingConfiguration.getMaxInstances() - lastInstanceInfo.getTotalInstances(),
                                                Math.max(0, action.getAmount()));
                    return new ScalingAction(ScalingOperation.SCALE_UP, delta);
                }
                break;
            }
            case SCALE_DOWN: {
                if (lastInstanceInfo.getTotalInstances() == scalingConfiguration.getMinInstances()) {
                    return new ScalingAction(ScalingOperation.NONE, 0);
                } else if (lastInstanceInfo.getTotalInstances() < scalingConfiguration.getMinInstances()) {
                    return new ScalingAction(ScalingOperation.SCALE_UP,
                                             scalingConfiguration.getMinInstances() - lastInstanceInfo.getTotalInstances());
                } else if ((lastInstanceInfo.getTotalInstances() - action.getAmount()) < scalingConfiguration.getMinInstances()) {
                    return new ScalingAction(ScalingOperation.SCALE_DOWN,
                                             lastInstanceInfo.getTotalInstances() - scalingConfiguration.getMinInstances());
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
    private boolean otherServicesMinimumInstancesMet(String serviceRef){
        for(String key:scalingConfigurationMap.keySet()){
            if(key.equals(serviceRef)){
                continue;
            }
            ScalingConfiguration scalingConfiguration = scalingConfigurationMap.getOrDefault(key, null);
            InstanceInfo lastInstanceInfo = instanceInfoMap.getOrDefault(key, null);

            //If there are no instance info be cautious and assume minimum instances have not been met.
            if(lastInstanceInfo==null || lastInstanceInfo.getTotalInstances() < scalingConfiguration.getMinInstances()){
                return false;
            }
        }
        return true;
    }

    private boolean shouldGovern(final int currentMemoryLoadLimit, final int shutdownPriority)
    {
        if (shutdownPriority == -1) {
            return false;
        }
        switch (currentMemoryLoadLimit) {
            case 1: {
                return shutdownPriority <= stageOneShutdownPriorityLimit;
            }
            case 2: {
                return shutdownPriority <= stageTwoShutdownPriorityLimit;
            }
            case 3: {
                return shutdownPriority <= stageThreeShutdownPriorityLimit;
            }
            default: {
                return true;
            }
        }
    }
}
