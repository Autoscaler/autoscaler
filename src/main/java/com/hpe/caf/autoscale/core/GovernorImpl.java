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
    private final Map<String, InstanceInfo> instanceInfoMap = new ConcurrentHashMap<>() ;
    private final Map<String, ScalingConfiguration> scalingConfigurationMap = new ConcurrentHashMap<>();

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
    public ScalingAction govern(String serviceRef, ScalingAction action) {

        ScalingConfiguration scalingConfiguration = scalingConfigurationMap.getOrDefault(serviceRef, null);
        InstanceInfo lastInstanceInfo = instanceInfoMap.getOrDefault(serviceRef, null);

        if(scalingConfiguration==null){
            throw new RuntimeException(String.format("Scaling configuration not found for {%s}", serviceRef));
        }

        boolean otherServicesMinimumInstancesMet = otherServicesMinimumInstancesMet(serviceRef);

        switch(action.getOperation()){
            case NONE:
            case SCALE_UP:
            {
                if(!otherServicesMinimumInstancesMet && lastInstanceInfo!=null){
                    if(lastInstanceInfo.getTotalInstances()==scalingConfiguration.getMinInstances()){
                        return new ScalingAction(ScalingOperation.NONE, 0);
                    }
                    else if(lastInstanceInfo.getTotalInstances()>scalingConfiguration.getMinInstances()){
                        //Gradually reduce the totalInstances by a percentage until Minimums are met.
                        //This should be configurable, however there should be a Governor specific configuration
                        //to allow different Governor implementations to be added without polluting the AutoscaleConfiguration

                        int target = Math.max(scalingConfiguration.getMinInstances(), (int)Math.floor(lastInstanceInfo.getTotalInstances() * reduceToPercentage));
                        int amount = lastInstanceInfo.getTotalInstances() - target;

                        return new ScalingAction(ScalingOperation.SCALE_DOWN, amount);
                    }
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
}
