/*
 * Copyright 2015-2025 Open Text.
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
import com.github.autoscaler.api.ResourceUtilisation;
import com.github.autoscaler.api.ScalerException;
import com.github.autoscaler.api.ScalingAction;
import com.github.autoscaler.api.ServiceScaler;
import com.github.autoscaler.api.WorkloadAnalyser;
import java.util.HashMap;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mockito;

import java.util.LinkedList;
import java.util.Optional;

import static org.mockito.AdditionalAnswers.returnsSecondArg;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class ScalerThreadTest
{
    private final static String SERVICE_REF = "unitTest";
    private static ResourceMonitoringConfiguration mockResourceMonitoringConfiguration;

    @BeforeAll
    public static void beforeAll()
    {
        mockResourceMonitoringConfiguration = Mockito.mock(ResourceMonitoringConfiguration.class);
        Mockito.when(mockResourceMonitoringConfiguration.getMemoryUsedPercentLimitStageOne()).thenReturn(70.0);
        Mockito.when(mockResourceMonitoringConfiguration.getMemoryUsedPercentLimitStageTwo()).thenReturn(80.0);
        Mockito.when(mockResourceMonitoringConfiguration.getMemoryUsedPercentLimitStageThree()).thenReturn(90.0);

        Mockito.when(mockResourceMonitoringConfiguration.getDiskFreeMbLimitStageOne()).thenReturn(400);
        Mockito.when(mockResourceMonitoringConfiguration.getDiskFreeMbLimitStageTwo()).thenReturn(200);
        Mockito.when(mockResourceMonitoringConfiguration.getDiskFreeMbLimitStageThree()).thenReturn(100);

        Mockito.when(mockResourceMonitoringConfiguration.getResourceLimitOneShutdownThreshold()).thenReturn(1);
        Mockito.when(mockResourceMonitoringConfiguration.getResourceLimitTwoShutdownThreshold()).thenReturn(3);
        Mockito.when(mockResourceMonitoringConfiguration.getResourceLimitThreeShutdownThreshold()).thenReturn(5);

        Mockito.when(mockResourceMonitoringConfiguration.getMemoryUsedPercentAlertDispatchThreshold()).thenReturn(70);
        Mockito.when(mockResourceMonitoringConfiguration.getDiskFreeMbAlertDispatchThreshold()).thenReturn(500);
    }

    @Test
    public void testScaleUp()
            throws ScalerException
    {
        WorkloadAnalyser analyser = Mockito.mock(WorkloadAnalyser.class);
        ServiceScaler scaler = Mockito.mock(ServiceScaler.class);
        InstanceInfo info = new InstanceInfo(1, 0, new LinkedList<>());
        Mockito.when(scaler.getInstanceInfo(SERVICE_REF)).thenReturn(info);
        Governor governor = Mockito.mock(Governor.class);
        Mockito.when(governor.govern(Mockito.anyString(), Mockito.any(), Mockito.any())).then(returnsSecondArg());

        int min = 0;
        int max = 5;
        ScalerThread t = new ScalerThread(governor, analyser, scaler, SERVICE_REF, min, max, 0,
            new Alerter(new HashMap<>(), new AlertDispatchConfiguration()), new Alerter(new HashMap<>(),
                new AlertDispatchConfiguration()), new ResourceMonitoringConfiguration());
        t.run();
        Mockito.when(analyser.getCurrentResourceUtilisation()).thenReturn(new ResourceUtilisation(0.0, Optional.of(0)));
        Mockito.when(analyser.analyseWorkload(info)).thenReturn(ScalingAction.SCALE_UP);
        t.run();
        Mockito.verify(scaler, Mockito.times(1)).scaleUp(SERVICE_REF, 1);
    }


    @Test
    public void testScaleDown()
            throws ScalerException
    {
        WorkloadAnalyser analyser = Mockito.mock(WorkloadAnalyser.class);
        ServiceScaler scaler = Mockito.mock(ServiceScaler.class);
        InstanceInfo info = new InstanceInfo(1, 0, new LinkedList<>());
        Mockito.when(scaler.getInstanceInfo(SERVICE_REF)).thenReturn(info);
        Governor governor = Mockito.mock(Governor.class);
        Mockito.when(governor.govern(Mockito.anyString(), Mockito.any(), Mockito.any())).then(returnsSecondArg());

        int min = 0;
        int max = 5;
        ScalerThread t = new ScalerThread(governor, analyser, scaler, SERVICE_REF, min, max, 0, 
            new Alerter(new HashMap<>(), new AlertDispatchConfiguration()), new Alerter(new HashMap<>(),
                new AlertDispatchConfiguration()), new ResourceMonitoringConfiguration());
        t.run();
        Mockito.when(analyser.getCurrentResourceUtilisation()).thenReturn(new ResourceUtilisation(0.0, Optional.of(0)));
        Mockito.when(analyser.analyseWorkload(info)).thenReturn(ScalingAction.SCALE_DOWN);
        t.run();
        Mockito.verify(scaler, Mockito.times(1)).scaleDown(SERVICE_REF, 1);
    }

    @Test
    public void testScaleDownDueToHighMemoryUsage()
            throws ScalerException
    {
        WorkloadAnalyser analyser = Mockito.mock(WorkloadAnalyser.class);
        ServiceScaler scaler = Mockito.mock(ServiceScaler.class);
        InstanceInfo info = new InstanceInfo(1, 0, new LinkedList<>(), 5, 1);
        Mockito.when(scaler.getInstanceInfo(SERVICE_REF)).thenReturn(info);
        Governor governor = Mockito.mock(Governor.class);
        Alerter memoryOverloadAlerter = Mockito.mock(Alerter.class);

        // Set the current memory used to 90% to reach the stage 3 limit, which should cause a scale down operation
        Mockito.when(analyser.getCurrentResourceUtilisation()).thenReturn(new ResourceUtilisation(90, Optional.empty()));

        int min = 0;
        int max = 5;

        ScalerThread t = new ScalerThread(governor, analyser, scaler, SERVICE_REF, min, max, 0,
                memoryOverloadAlerter, new Alerter(new HashMap<>(), new AlertDispatchConfiguration()),
                mockResourceMonitoringConfiguration);

        t.run();

        Mockito.verify(memoryOverloadAlerter, Mockito.times(1)).dispatchAlert(Mockito.any());
        Mockito.verify(scaler, Mockito.times(1)).scaleDown(SERVICE_REF, 1);
    }

    @Test
    public void testScaleDownDueToLowDiskSpace()
            throws ScalerException
    {
        WorkloadAnalyser analyser = Mockito.mock(WorkloadAnalyser.class);
        ServiceScaler scaler = Mockito.mock(ServiceScaler.class);
        InstanceInfo info = new InstanceInfo(1, 0, new LinkedList<>(), 1, 1);
        Mockito.when(scaler.getInstanceInfo(SERVICE_REF)).thenReturn(info);
        Governor governor = Mockito.mock(Governor.class);
        Alerter diskSpaceLowAlerter = Mockito.mock(Alerter.class);

        // Set the current disk space free to be 400MB to reach the stage 1 limit, which should cause a scale down operation
        Mockito.when(analyser.getCurrentResourceUtilisation()).thenReturn(new ResourceUtilisation(0.0, Optional.of(400)));

        int min = 0;
        int max = 5;

        ScalerThread t = new ScalerThread(governor, analyser, scaler, SERVICE_REF, min, max, 0,
                new Alerter(new HashMap<>(), new AlertDispatchConfiguration()), diskSpaceLowAlerter, mockResourceMonitoringConfiguration);

        t.run();

        Mockito.verify(diskSpaceLowAlerter, Mockito.times(1)).dispatchAlert(Mockito.any());
        Mockito.verify(scaler, Mockito.times(1)).scaleDown(SERVICE_REF, 1);
    }

    @Test
    public void testNoScale()
            throws ScalerException
    {
        WorkloadAnalyser analyser = Mockito.mock(WorkloadAnalyser.class);
        ServiceScaler scaler = Mockito.mock(ServiceScaler.class);
        InstanceInfo info = new InstanceInfo(1, 0, new LinkedList<>());
        Governor governor = Mockito.mock(Governor.class);

        int min = 0;
        int max = 5;
        ScalerThread t = new ScalerThread(governor, analyser, scaler, SERVICE_REF, min, max, 0, 
            new Alerter(new HashMap<>(), new AlertDispatchConfiguration()), new Alerter(new HashMap<>(),
                new AlertDispatchConfiguration()), new ResourceMonitoringConfiguration());
        t.run();
        t.run();
        Mockito.verify(scaler, Mockito.times(0)).scaleUp(Mockito.any(), Mockito.anyInt());
        Mockito.verify(scaler, Mockito.times(0)).scaleDown(Mockito.any(), Mockito.anyInt());
    }

    @Test
    public void testWithRuntimeException() throws ScalerException {
        WorkloadAnalyser analyser = Mockito.mock(WorkloadAnalyser.class);
        ServiceScaler scaler = Mockito.mock(ServiceScaler.class);
        InstanceInfo info = new InstanceInfo(1, 0, new LinkedList<>());
        Governor governor = Mockito.mock(Governor.class);

        int min = 0;
        int max = 5;
        ScalerThread t = new ScalerThread(governor, analyser, scaler, SERVICE_REF, min, max, 0,
            new Alerter(new HashMap<>(), new AlertDispatchConfiguration()), new Alerter(new HashMap<>(),
                new AlertDispatchConfiguration()), new ResourceMonitoringConfiguration());
        // should not throw
        t.run();
        Mockito.verify(scaler, Mockito.times(0)).scaleUp(Mockito.any(), Mockito.anyInt());
        Mockito.verify(scaler, Mockito.times(0)).scaleDown(Mockito.any(), Mockito.anyInt());
    }

    @Test
    public void testScaledDown_Stage_One_FreeDisk_LimitReached() throws ScalerException {
        testScalingOperation(1, 0, 400, 1, 1);
    }

    @Test
    public void testScaledDown_Stage_Two_FreeDisk_LimitReached() throws ScalerException {
        testScalingOperation(1, 0, 200, 1, 1);
    }

    @Test
    public void testScaledDown_Stage_Three_FreeDisk_LimitReached() throws ScalerException {
        testScalingOperation(1, 0, 100, 1, 1);
    }

    @Test
    public void testNotScaledDown_Stage_One_FreeDisk_LimitReached_PriorityTooHigh() throws ScalerException {
        testScalingOperation(5, 0, 400, 1, 0);
    }

    @Test
    public void testNotScaledDown_Stage_Two_FreeDisk_LimitReached_PriorityTooHigh() throws ScalerException {
        testScalingOperation(5, 0, 200, 1, 0);
    }

    @Test
    public void testNotScaledDown_Stage_Three_FreeDisk_LimitReached_PriorityTooHigh() throws ScalerException {
        testScalingOperation(10, 0, 100, 1, 0);
    }

    @Test
    public void testScaledDown_Stage_One_MemoryPercentageLimitReached() throws ScalerException {
        testScalingOperation(1, 70, 600, 0, 1);
    }

    @Test
    public void testScaledDown_Stage_Two_MemoryPercentageLimitReached() throws ScalerException {
        testScalingOperation(1, 80, 600, 0, 1);
    }

    @Test
    public void testScaledDown_Stage_Memory_PercentageDiskLimitReached() throws ScalerException {
        testScalingOperation(1, 90, 600, 0, 1);
    }

    @Test
    public void testNotScaledDown_Stage_One_MemoryPercentageLimitReached_PriorityTooHigh() throws ScalerException {
        testScalingOperation(5, 70, 600, 0, 0);
    }

    @Test
    public void testNotScaledDown_Stage_Two_MemoryPercentageLimitReached_PriorityTooHigh() throws ScalerException {
        testScalingOperation(5, 80, 600, 0, 0);
    }

    @Test
    public void testNotScaledDown_Stage_Memory_PercentageDiskLimitReached_PriorityTooHigh() throws ScalerException {
        testScalingOperation(10, 90, 600, 0, 0);
    }

    private void testScalingOperation(
            final int shutdownPriority, 
            final int memoryUsedPercent, 
            final int freeDiskMb,
            final int wantedDispatchAlertInvocations,
            final int wantedScaleDownInvocations
    ) throws ScalerException {
        WorkloadAnalyser analyser = Mockito.mock(WorkloadAnalyser.class);
        ServiceScaler scaler = Mockito.mock(ServiceScaler.class);
        InstanceInfo info = new InstanceInfo(1, 0, new LinkedList<>(), shutdownPriority, 1);
        Mockito.when(scaler.getInstanceInfo(SERVICE_REF)).thenReturn(info);
        Governor governor = Mockito.mock(Governor.class);
        Alerter diskSpaceLowAlerter = Mockito.mock(Alerter.class);

        Mockito.when(analyser.getCurrentResourceUtilisation()).thenReturn(new ResourceUtilisation(memoryUsedPercent, Optional.of(freeDiskMb)));

        int min = 0;
        int max = 5;

        ScalerThread t = new ScalerThread(governor, analyser, scaler, SERVICE_REF, min, max, 0,
                new Alerter(new HashMap<>(), new AlertDispatchConfiguration()), diskSpaceLowAlerter, mockResourceMonitoringConfiguration);

        t.run();

        Mockito.verify(diskSpaceLowAlerter, Mockito.times(wantedDispatchAlertInvocations)).dispatchAlert(Mockito.any());
        Mockito.verify(scaler, Mockito.times(wantedScaleDownInvocations)).scaleDown(SERVICE_REF, 1);
    }

}
