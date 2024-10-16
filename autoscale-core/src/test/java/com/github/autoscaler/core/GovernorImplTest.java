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
import com.github.autoscaler.api.ScalingAction;
import com.github.autoscaler.api.ScalingConfiguration;
import com.github.autoscaler.api.ScalingOperation;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.Test;

import java.util.Collections;

public class GovernorImplTest {

    private final static String SCALE_UP = "SCALE_UP";
    private final static String SCALE_DOWN = "SCALE_DOWN";
    private final static String NONE = "NONE";

    /**
     * Test that a scale up is allowed when other services are at their minimum
     */
    @Test
    public void testSingleServiceShouldNotOverrideScaleUpWhenMinimumNotReached() {
        final Governor governor = new GovernorImpl(1, 3, 5);
        final ScalingConfiguration scalingConfigurationForServiceOne = new ScalingConfiguration();
        scalingConfigurationForServiceOne.setBackoffAmount(1000);
        scalingConfigurationForServiceOne.setId("service1");
        scalingConfigurationForServiceOne.setInterval(1000);
        scalingConfigurationForServiceOne.setMaxInstances(10);
        scalingConfigurationForServiceOne.setMinInstances(3);
        scalingConfigurationForServiceOne.setScalingProfile("scalingprofile");
        scalingConfigurationForServiceOne.setScalingTarget("scalingtarget");
        final ScalingConfiguration scalingConfigurationForServiceTwo = new ScalingConfiguration();
        scalingConfigurationForServiceTwo.setBackoffAmount(1000);
        scalingConfigurationForServiceTwo.setId("service2");
        scalingConfigurationForServiceTwo.setInterval(1000);
        scalingConfigurationForServiceTwo.setMaxInstances(10);
        scalingConfigurationForServiceTwo.setMinInstances(3);
        scalingConfigurationForServiceTwo.setScalingProfile("scalingprofile");
        scalingConfigurationForServiceTwo.setScalingTarget("scalingtarget");

        governor.register(scalingConfigurationForServiceOne);
        governor.register(scalingConfigurationForServiceTwo);

        final InstanceInfo firstServiceInstanceInfo = new InstanceInfo(0, 0, Collections.emptyList());
        governor.recordInstances(scalingConfigurationForServiceOne.getId(), firstServiceInstanceInfo);
        final InstanceInfo secondServiceInstanceInfo = new InstanceInfo(0, 0, Collections.emptyList());
        governor.recordInstances(scalingConfigurationForServiceTwo.getId(), secondServiceInstanceInfo);

        final ScalingAction scalingAction = new ScalingAction(ScalingOperation.SCALE_UP, 1);

        final ScalingAction governedAction = governor.govern(scalingConfigurationForServiceOne.getId(), scalingAction,
                new ResourceLimitStagesReached(ResourceLimitStage.NO_STAGE, ResourceLimitStage.NO_STAGE));

        assertEquals(3, governedAction.getAmount());
        assertEquals(SCALE_UP, governedAction.getOperation().toString());
    }

    /**
     * Test that a scale up is allowed when other services are at their minimum
     */
    @Test
    public void testSingleServiceShouldNotOverrideScaleUp() {
        final Governor governor = new GovernorImpl(1, 3, 5);
        final ScalingConfiguration scalingConfiguration = new ScalingConfiguration();
        scalingConfiguration.setBackoffAmount(1000);
        scalingConfiguration.setId("service1");
        scalingConfiguration.setInterval(1000);
        scalingConfiguration.setMaxInstances(10);
        scalingConfiguration.setMinInstances(1);
        scalingConfiguration.setScalingProfile("scalingprofile");
        scalingConfiguration.setScalingTarget("scalingtarget");

        governor.register(scalingConfiguration);

        final InstanceInfo instanceInfo = new InstanceInfo(5, 0, Collections.emptyList());
        governor.recordInstances(scalingConfiguration.getId(), instanceInfo);

        final ScalingAction scalingAction = new ScalingAction(ScalingOperation.SCALE_UP, 1);

        final ScalingAction governedAction = governor.govern(scalingConfiguration.getId(), scalingAction,
                new ResourceLimitStagesReached(ResourceLimitStage.NO_STAGE, ResourceLimitStage.NO_STAGE));

        assertEquals(1, governedAction.getAmount());
        assertEquals(SCALE_UP, governedAction.getOperation().toString());
    }

    /**
     * Test that a scale up is allowed when other services are at their minimum
     */
    @Test
    public void testSingleServiceShouldOverrideScaleUp() {
        final Governor governor = new GovernorImpl(1, 3, 5);
        final ScalingConfiguration scalingConfiguration = new ScalingConfiguration();
        scalingConfiguration.setBackoffAmount(1000);
        scalingConfiguration.setId("service1");
        scalingConfiguration.setInterval(1000);
        scalingConfiguration.setMaxInstances(10);
        scalingConfiguration.setMinInstances(1);
        scalingConfiguration.setScalingProfile("scalingprofile");
        scalingConfiguration.setScalingTarget("scalingtarget");

        governor.register(scalingConfiguration);

        final InstanceInfo instanceInfo = new InstanceInfo(150, 0, Collections.emptyList());
        governor.recordInstances(scalingConfiguration.getId(), instanceInfo);

        final ScalingAction scalingAction = new ScalingAction(ScalingOperation.SCALE_UP, 10);

        final ScalingAction governedAction = governor.govern(scalingConfiguration.getId(), scalingAction,
                new ResourceLimitStagesReached(ResourceLimitStage.NO_STAGE, ResourceLimitStage.NO_STAGE));

        assertEquals(140, governedAction.getAmount());
        assertEquals(SCALE_DOWN, governedAction.getOperation().toString());
    }

    /**
     * Test that a scale up is allowed when other services are at their minimum
     */
    @Test
    public void testSingleServiceShouldNotOverrideScaleDown() {
        final Governor governor = new GovernorImpl(1, 3, 5);
        final ScalingConfiguration scalingConfiguration = new ScalingConfiguration();
        scalingConfiguration.setBackoffAmount(1000);
        scalingConfiguration.setId("service1");
        scalingConfiguration.setInterval(1000);
        scalingConfiguration.setMaxInstances(10);
        scalingConfiguration.setMinInstances(1);
        scalingConfiguration.setScalingProfile("scalingprofile");
        scalingConfiguration.setScalingTarget("scalingtarget");

        governor.register(scalingConfiguration);

        final InstanceInfo instanceInfo = new InstanceInfo(5, 0, Collections.emptyList());
        governor.recordInstances(scalingConfiguration.getId(), instanceInfo);

        final ScalingAction scalingAction = new ScalingAction(ScalingOperation.SCALE_DOWN, 1);

        final ScalingAction governedAction = governor.govern(scalingConfiguration.getId(), scalingAction,
                new ResourceLimitStagesReached(ResourceLimitStage.NO_STAGE, ResourceLimitStage.NO_STAGE));

        assertEquals(1, governedAction.getAmount());
        assertEquals(SCALE_DOWN, governedAction.getOperation().toString());
    }

    /**
     * Test that a scale up is allowed when other services are at their minimum
     */
    @Test
    public void testSingleServiceShouldOverrideScaleDown() {
        final Governor governor = new GovernorImpl(1, 3, 5);
        final ScalingConfiguration scalingConfiguration = new ScalingConfiguration();
        scalingConfiguration.setBackoffAmount(1000);
        scalingConfiguration.setId("service1");
        scalingConfiguration.setInterval(1000);
        scalingConfiguration.setMaxInstances(10);
        scalingConfiguration.setMinInstances(1);
        scalingConfiguration.setScalingProfile("scalingprofile");
        scalingConfiguration.setScalingTarget("scalingtarget");

        governor.register(scalingConfiguration);

        final InstanceInfo instanceInfo = new InstanceInfo(1, 0, Collections.emptyList());
        governor.recordInstances(scalingConfiguration.getId(), instanceInfo);

        final ScalingAction scalingAction = new ScalingAction(ScalingOperation.SCALE_DOWN, 1);

        final ScalingAction governedAction = governor.govern(scalingConfiguration.getId(), scalingAction,
                new ResourceLimitStagesReached(ResourceLimitStage.NO_STAGE, ResourceLimitStage.NO_STAGE));

        assertEquals(0, governedAction.getAmount());
        assertEquals(NONE, governedAction.getOperation().toString());
    }
    /**
     * Tests that if a service is above its minimum and below its maximum instances that a NONE action is respected
     */
    @Test
    public void testSingleServiceShouldNotOverrideScaleNone() {
        final Governor governor = new GovernorImpl(1, 3, 5);
        final ScalingConfiguration scalingConfiguration = new ScalingConfiguration();
        scalingConfiguration.setBackoffAmount(1000);
        scalingConfiguration.setId("service1");
        scalingConfiguration.setInterval(1000);
        scalingConfiguration.setMaxInstances(10);
        scalingConfiguration.setMinInstances(1);
        scalingConfiguration.setScalingProfile("scalingprofile");
        scalingConfiguration.setScalingTarget("scalingtarget");

        governor.register(scalingConfiguration);

        final InstanceInfo instanceInfo = new InstanceInfo(5, 0, Collections.emptyList());
        governor.recordInstances(scalingConfiguration.getId(), instanceInfo);

        final ScalingAction scalingAction = new ScalingAction(ScalingOperation.NONE, 0);

        final ScalingAction governedAction = governor.govern(scalingConfiguration.getId(), scalingAction,
                new ResourceLimitStagesReached(ResourceLimitStage.NO_STAGE, ResourceLimitStage.NO_STAGE));

        assertEquals(0, governedAction.getAmount());
        assertEquals(NONE, governedAction.getOperation().toString());
    }

    /**
     * Test that a service not currently at its minimum will be scaled up even if the analyser determines that no instance is required
     */
    @Test
    public void testSingleServiceShouldOverrideScaleNoneUnderLimit() {
        final Governor governor = new GovernorImpl(1, 3, 5);
        final ScalingConfiguration scalingConfiguration = new ScalingConfiguration();
        scalingConfiguration.setBackoffAmount(1000);
        scalingConfiguration.setId("service1");
        scalingConfiguration.setInterval(1000);
        scalingConfiguration.setMaxInstances(10);
        scalingConfiguration.setMinInstances(1);
        scalingConfiguration.setScalingProfile("scalingprofile");
        scalingConfiguration.setScalingTarget("scalingtarget");

        governor.register(scalingConfiguration);

        final InstanceInfo instanceInfo = new InstanceInfo(0, 0, Collections.emptyList());
        governor.recordInstances(scalingConfiguration.getId(), instanceInfo);

        final ScalingAction scalingAction = new ScalingAction(ScalingOperation.NONE, 0);

        final ScalingAction governedAction = governor.govern(scalingConfiguration.getId(), scalingAction,
                new ResourceLimitStagesReached(ResourceLimitStage.NO_STAGE, ResourceLimitStage.NO_STAGE));

        assertEquals(1, governedAction.getAmount());
        assertEquals(SCALE_UP, governedAction.getOperation().toString());
    }

    /**
     * Test that a service currently over its maximum instances will be scaled down even if the analyser determines that no instance
     * change is required.
     */
    @Test
    public void testSingleServiceShouldOverrideScaleNoneOverLimit() {
        final Governor governor = new GovernorImpl(1, 3, 5);
        final ScalingConfiguration scalingConfiguration = new ScalingConfiguration();
        scalingConfiguration.setBackoffAmount(1000);
        scalingConfiguration.setId("service1");
        scalingConfiguration.setInterval(1000);
        scalingConfiguration.setMaxInstances(10);
        scalingConfiguration.setMinInstances(1);
        scalingConfiguration.setScalingProfile("scalingprofile");
        scalingConfiguration.setScalingTarget("scalingtarget");

        governor.register(scalingConfiguration);

        final InstanceInfo instanceInfo = new InstanceInfo(11, 0, Collections.emptyList());
        governor.recordInstances(scalingConfiguration.getId(), instanceInfo);

        final ScalingAction scalingAction = new ScalingAction(ScalingOperation.NONE, 0);

        final ScalingAction governedAction = governor.govern(scalingConfiguration.getId(), scalingAction,
                new ResourceLimitStagesReached(ResourceLimitStage.NO_STAGE, ResourceLimitStage.NO_STAGE));

        assertEquals(1, governedAction.getAmount());
        assertEquals(SCALE_DOWN, governedAction.getOperation().toString());
    }

    /**
     * Test that a scale up is replaced with a gradual scale down when other services are not at their minimum
     */
    @Test
    public void testMultipleServicesPreventScaleUp() {
        Governor governor = new GovernorImpl(1, 3, 5);
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
        ScalingAction governedAction = governor.govern("service1", scalingAction, new ResourceLimitStagesReached(ResourceLimitStage.NO_STAGE, ResourceLimitStage.NO_STAGE));
        assertEquals(ScalingOperation.SCALE_DOWN, governedAction.getOperation());
        assertTrue(governedAction.getAmount()>0);
        service1CurrentInstances = service1CurrentInstances - governedAction.getAmount();

        while(governedAction.getOperation()==ScalingOperation.SCALE_DOWN){
            InstanceInfo instanceInfo = new InstanceInfo(service1CurrentInstances, 0, Collections.emptyList());
            governor.recordInstances("service1", instanceInfo);

            //The service still wishes to scale up
            scalingAction = new ScalingAction(ScalingOperation.SCALE_UP, 100);
            governedAction = governor.govern("service1", scalingAction,
                    new ResourceLimitStagesReached(ResourceLimitStage.NO_STAGE, ResourceLimitStage.NO_STAGE));
            service1CurrentInstances = service1CurrentInstances - governedAction.getAmount();

            System.out.println(String.format("Current instances %d", service1CurrentInstances));
        }
    }
    
    /**
     * Test that a scale up is allowed when other services have been scaled down because of the message broker memory backoff.
     */
    @Test
    public void testScalingWhenBackPressureBackOffInProgress() {
        final Governor governor = new GovernorImpl(1, 3, 5);
        final ScalingConfiguration scalingConfigurationForServiceOne = new ScalingConfiguration();
        scalingConfigurationForServiceOne.setBackoffAmount(1000);
        scalingConfigurationForServiceOne.setId("service1");
        scalingConfigurationForServiceOne.setInterval(1000);
        scalingConfigurationForServiceOne.setMaxInstances(10);
        scalingConfigurationForServiceOne.setMinInstances(1);
        scalingConfigurationForServiceOne.setScalingProfile("scalingprofile");
        scalingConfigurationForServiceOne.setScalingTarget("scalingtarget");
        final ScalingConfiguration scalingConfigurationForServiceTwo = new ScalingConfiguration();
        scalingConfigurationForServiceTwo.setBackoffAmount(1000);
        scalingConfigurationForServiceTwo.setId("service2");
        scalingConfigurationForServiceTwo.setInterval(1000);
        scalingConfigurationForServiceTwo.setMaxInstances(10);
        scalingConfigurationForServiceTwo.setMinInstances(1);
        scalingConfigurationForServiceTwo.setScalingProfile("scalingprofile");
        scalingConfigurationForServiceTwo.setScalingTarget("scalingtarget");

        governor.register(scalingConfigurationForServiceOne);
        governor.register(scalingConfigurationForServiceTwo);

        final InstanceInfo firstServiceInstanceInfo = new InstanceInfo(0, 0, Collections.emptyList());
        governor.recordInstances(scalingConfigurationForServiceOne.getId(), firstServiceInstanceInfo);
        final InstanceInfo secondServiceInstanceInfo = new InstanceInfo(1, 0, Collections.emptyList());
        governor.recordInstances(scalingConfigurationForServiceTwo.getId(), secondServiceInstanceInfo);

        final ScalingAction scalingAction = new ScalingAction(ScalingOperation.SCALE_UP, 3);

        final ScalingAction governedAction = governor.govern(scalingConfigurationForServiceOne.getId(), scalingAction,
                new ResourceLimitStagesReached(ResourceLimitStage.STAGE_1, ResourceLimitStage.NO_STAGE));

        assertEquals(3, governedAction.getAmount());
        assertEquals(SCALE_UP, governedAction.getOperation().toString());
    }

    /**
     * Test that a scale up is allowed when other services have been scaled down because of the message broker memory high backoff.
     */
    @Test
    public void testScalingWhenMemoryLimitReachedAndBackPressureBackOffInProgressMaxInstanceProtection() {
        final Governor governor = new GovernorImpl(1, 3, 5);
        final ScalingConfiguration scalingConfigurationForServiceOne = new ScalingConfiguration();
        scalingConfigurationForServiceOne.setBackoffAmount(1000);
        scalingConfigurationForServiceOne.setId("service1");
        scalingConfigurationForServiceOne.setInterval(1000);
        scalingConfigurationForServiceOne.setMaxInstances(10);
        scalingConfigurationForServiceOne.setMinInstances(1);
        scalingConfigurationForServiceOne.setScalingProfile("scalingprofile");
        scalingConfigurationForServiceOne.setScalingTarget("scalingtarget");
        final ScalingConfiguration scalingConfigurationForServiceTwo = new ScalingConfiguration();
        scalingConfigurationForServiceTwo.setBackoffAmount(1000);
        scalingConfigurationForServiceTwo.setId("service2");
        scalingConfigurationForServiceTwo.setInterval(1000);
        scalingConfigurationForServiceTwo.setMaxInstances(3);
        scalingConfigurationForServiceTwo.setMinInstances(1);
        scalingConfigurationForServiceTwo.setScalingProfile("scalingprofile");
        scalingConfigurationForServiceTwo.setScalingTarget("scalingtarget");

        governor.register(scalingConfigurationForServiceOne);
        governor.register(scalingConfigurationForServiceTwo);

        final InstanceInfo firstServiceInstanceInfo = new InstanceInfo(0, 0, Collections.emptyList(), 1, 0);
        governor.recordInstances(scalingConfigurationForServiceOne.getId(), firstServiceInstanceInfo);
        final InstanceInfo secondServiceInstanceInfo = new InstanceInfo(1, 0, Collections.emptyList());
        governor.recordInstances(scalingConfigurationForServiceTwo.getId(), secondServiceInstanceInfo);

        final ScalingAction scalingAction = new ScalingAction(ScalingOperation.SCALE_UP, 3);

        final ScalingAction governedAction = governor.govern(scalingConfigurationForServiceTwo.getId(), scalingAction,
                new ResourceLimitStagesReached(ResourceLimitStage.STAGE_1, ResourceLimitStage.NO_STAGE));

        assertEquals(2, governedAction.getAmount());
        assertEquals(SCALE_UP, governedAction.getOperation().toString());
    }

    /**
     * Test that a scale up is allowed when other services have been scaled down because of the message broker disk space low backoff.
     */
    @Test
    public void testScalingWhenDiskLimitReachedAndBackPressureBackOffInProgressMaxInstanceProtection() {
        final Governor governor = new GovernorImpl(1, 3, 5);
        final ScalingConfiguration scalingConfigurationForServiceOne = new ScalingConfiguration();
        scalingConfigurationForServiceOne.setBackoffAmount(1000);
        scalingConfigurationForServiceOne.setId("service1");
        scalingConfigurationForServiceOne.setInterval(1000);
        scalingConfigurationForServiceOne.setMaxInstances(10);
        scalingConfigurationForServiceOne.setMinInstances(1);
        scalingConfigurationForServiceOne.setScalingProfile("scalingprofile");
        scalingConfigurationForServiceOne.setScalingTarget("scalingtarget");
        final ScalingConfiguration scalingConfigurationForServiceTwo = new ScalingConfiguration();
        scalingConfigurationForServiceTwo.setBackoffAmount(1000);
        scalingConfigurationForServiceTwo.setId("service2");
        scalingConfigurationForServiceTwo.setInterval(1000);
        scalingConfigurationForServiceTwo.setMaxInstances(3);
        scalingConfigurationForServiceTwo.setMinInstances(1);
        scalingConfigurationForServiceTwo.setScalingProfile("scalingprofile");
        scalingConfigurationForServiceTwo.setScalingTarget("scalingtarget");

        governor.register(scalingConfigurationForServiceOne);
        governor.register(scalingConfigurationForServiceTwo);

        final InstanceInfo firstServiceInstanceInfo = new InstanceInfo(0, 0, Collections.emptyList(), 1, 0);
        governor.recordInstances(scalingConfigurationForServiceOne.getId(), firstServiceInstanceInfo);
        final InstanceInfo secondServiceInstanceInfo = new InstanceInfo(1, 0, Collections.emptyList());
        governor.recordInstances(scalingConfigurationForServiceTwo.getId(), secondServiceInstanceInfo);

        final ScalingAction scalingAction = new ScalingAction(ScalingOperation.SCALE_UP, 3);

        final ScalingAction governedAction = governor.govern(scalingConfigurationForServiceTwo.getId(), scalingAction,
                new ResourceLimitStagesReached(ResourceLimitStage.NO_STAGE, ResourceLimitStage.STAGE_1));

        assertEquals(2, governedAction.getAmount());
        assertEquals(SCALE_UP, governedAction.getOperation().toString());
    }

    /**
     * Test that a scale up is allowed when other services have been scaled down because of the message broker memory high and
     * disk space low backoff.
     */
    @Test
    public void testScalingWhenMemoryAndDiskLimitReachedAndBackPressureBackOffInProgressMaxInstanceProtection() {
        final Governor governor = new GovernorImpl(1, 3, 5);
        final ScalingConfiguration scalingConfigurationForServiceOne = new ScalingConfiguration();
        scalingConfigurationForServiceOne.setBackoffAmount(1000);
        scalingConfigurationForServiceOne.setId("service1");
        scalingConfigurationForServiceOne.setInterval(1000);
        scalingConfigurationForServiceOne.setMaxInstances(10);
        scalingConfigurationForServiceOne.setMinInstances(1);
        scalingConfigurationForServiceOne.setScalingProfile("scalingprofile");
        scalingConfigurationForServiceOne.setScalingTarget("scalingtarget");
        final ScalingConfiguration scalingConfigurationForServiceTwo = new ScalingConfiguration();
        scalingConfigurationForServiceTwo.setBackoffAmount(1000);
        scalingConfigurationForServiceTwo.setId("service2");
        scalingConfigurationForServiceTwo.setInterval(1000);
        scalingConfigurationForServiceTwo.setMaxInstances(3);
        scalingConfigurationForServiceTwo.setMinInstances(1);
        scalingConfigurationForServiceTwo.setScalingProfile("scalingprofile");
        scalingConfigurationForServiceTwo.setScalingTarget("scalingtarget");

        governor.register(scalingConfigurationForServiceOne);
        governor.register(scalingConfigurationForServiceTwo);

        final InstanceInfo firstServiceInstanceInfo = new InstanceInfo(0, 0, Collections.emptyList(), 1, 0);
        governor.recordInstances(scalingConfigurationForServiceOne.getId(), firstServiceInstanceInfo);
        final InstanceInfo secondServiceInstanceInfo = new InstanceInfo(1, 0, Collections.emptyList());
        governor.recordInstances(scalingConfigurationForServiceTwo.getId(), secondServiceInstanceInfo);

        final ScalingAction scalingAction = new ScalingAction(ScalingOperation.SCALE_UP, 3);

        final ScalingAction governedAction = governor.govern(scalingConfigurationForServiceTwo.getId(), scalingAction,
                new ResourceLimitStagesReached(ResourceLimitStage.STAGE_1, ResourceLimitStage.STAGE_1));

        assertEquals(2, governedAction.getAmount());
        assertEquals(SCALE_UP, governedAction.getOperation().toString());
    }
}
