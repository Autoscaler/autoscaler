package com.hpe.caf.autoscale.core;

import com.hpe.caf.api.autoscale.InstanceInfo;
import com.hpe.caf.api.autoscale.ScalingAction;
import com.hpe.caf.api.autoscale.ScalingConfiguration;
import com.hpe.caf.api.autoscale.ScalingOperation;
import org.junit.Assert;
import org.junit.Test;

import java.util.Collections;

public class GovernorImplTest {

    /**
     * Test that a scale up is allowed when other services are at their minimum
     */
    @Test
    public void testSingleServiceAllowScaleUp() {
        Governor governor = new GovernorImpl();
        ScalingConfiguration scalingConfiguration = new ScalingConfiguration();
        scalingConfiguration.setBackoffAmount(1000);
        scalingConfiguration.setId("service1");
        scalingConfiguration.setInterval(1000);
        scalingConfiguration.setMaxInstances(10);
        scalingConfiguration.setMinInstances(1);
        scalingConfiguration.setScalingProfile("scalingprofile");
        scalingConfiguration.setScalingTarget("scalingtarget");

        governor.register(scalingConfiguration);

        InstanceInfo instanceInfo = new InstanceInfo(100, 50, Collections.emptyList());
        governor.recordInstances(scalingConfiguration.getId(), instanceInfo);

        ScalingAction scalingAction = new ScalingAction(ScalingOperation.SCALE_UP, 100);

        ScalingAction governedAction = governor.govern(scalingConfiguration.getId(), scalingAction);

        Assert.assertEquals(scalingAction.getAmount(), governedAction.getAmount());
        Assert.assertEquals(scalingAction.getOperation(), governedAction.getOperation());

    }


    /**
     * Test that a scale up is replaced with a gradual scale down when other services are not at their minimum
     */
    @Test
    public void testMultipleServicesPreventScaleUp() {
        Governor governor = new GovernorImpl();
        int service1StartingInstances = 150;
        int service1CurrentInstances = service1StartingInstances;

        {
            ScalingConfiguration scalingConfiguration = new ScalingConfiguration();
            scalingConfiguration.setBackoffAmount(1000);
            scalingConfiguration.setId("service1");
            scalingConfiguration.setInterval(1000);
            scalingConfiguration.setMaxInstances(10);
            scalingConfiguration.setMinInstances(1);
            scalingConfiguration.setScalingProfile("scalingprofile");
            scalingConfiguration.setScalingTarget("scalingtarget");
            governor.register(scalingConfiguration);
            InstanceInfo instanceInfo = new InstanceInfo(service1StartingInstances, 0, Collections.emptyList());
            governor.recordInstances(scalingConfiguration.getId(), instanceInfo);
        }

        {
            ScalingConfiguration scalingConfiguration = new ScalingConfiguration();
            scalingConfiguration.setBackoffAmount(1000);
            scalingConfiguration.setId("service2");
            scalingConfiguration.setInterval(1000);
            scalingConfiguration.setMaxInstances(10);
            scalingConfiguration.setMinInstances(1);
            scalingConfiguration.setScalingProfile("scalingprofile");
            scalingConfiguration.setScalingTarget("scalingtarget");

            governor.register(scalingConfiguration);
            InstanceInfo instanceInfo = new InstanceInfo(0, 0, Collections.emptyList());
            governor.recordInstances(scalingConfiguration.getId(), instanceInfo);
        }

        ScalingAction scalingAction = new ScalingAction(ScalingOperation.SCALE_UP, 100);
        ScalingAction governedAction = governor.govern("service1", scalingAction);
        Assert.assertEquals(ScalingOperation.SCALE_DOWN, governedAction.getOperation());
        Assert.assertTrue(governedAction.getAmount()>0);
        service1CurrentInstances = service1CurrentInstances - governedAction.getAmount();

        while(governedAction.getOperation()==ScalingOperation.SCALE_DOWN){
            InstanceInfo instanceInfo = new InstanceInfo(service1CurrentInstances, 0, Collections.emptyList());
            governor.recordInstances("service1", instanceInfo);

            //The service still wishes to scale up
            scalingAction = new ScalingAction(ScalingOperation.SCALE_UP, 100);
            governedAction = governor.govern("service1", scalingAction);
            service1CurrentInstances = service1CurrentInstances - governedAction.getAmount();

            System.out.println(String.format("Current instances %d", service1CurrentInstances));
        }
    }
}

