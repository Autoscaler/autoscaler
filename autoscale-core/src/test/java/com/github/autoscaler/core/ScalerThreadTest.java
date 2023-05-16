/*
 * Copyright 2015-2023 Open Text.
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
import org.junit.Test;
import org.mockito.Mockito;

import java.util.LinkedList;
import java.util.Optional;

import static org.mockito.AdditionalAnswers.returnsSecondArg;


public class ScalerThreadTest
{
    private final static String SERVICE_REF = "unitTest";

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
            new Alerter(new HashMap<>(), new AlertDispatchConfiguration()), new ResourceMonitoringConfiguration());
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
            new Alerter(new HashMap<>(), new AlertDispatchConfiguration()), new ResourceMonitoringConfiguration());
        t.run();
        Mockito.when(analyser.getCurrentResourceUtilisation()).thenReturn(new ResourceUtilisation(0.0, Optional.of(0)));
        Mockito.when(analyser.analyseWorkload(info)).thenReturn(ScalingAction.SCALE_DOWN);
        t.run();
        Mockito.verify(scaler, Mockito.times(1)).scaleDown(SERVICE_REF, 1);
    }


    @Test
    public void testNoScale()
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
            new Alerter(new HashMap<>(), new AlertDispatchConfiguration()), new ResourceMonitoringConfiguration());
        t.run();
        Mockito.when(analyser.analyseWorkload(info)).thenReturn(ScalingAction.NO_ACTION);
        t.run();
        Mockito.verify(scaler, Mockito.times(0)).scaleUp(Mockito.any(), Mockito.anyInt());
        Mockito.verify(scaler, Mockito.times(0)).scaleDown(Mockito.any(), Mockito.anyInt());
    }

    @Test
    public void testWithRuntimeException() throws ScalerException {
        WorkloadAnalyser analyser = Mockito.mock(WorkloadAnalyser.class);
        ServiceScaler scaler = Mockito.mock(ServiceScaler.class);
        InstanceInfo info = new InstanceInfo(1, 0, new LinkedList<>());
        Mockito.when(scaler.getInstanceInfo(SERVICE_REF))
            .thenThrow(new RuntimeException("network error"));
        Governor governor = Mockito.mock(Governor.class);
        Mockito.when(governor.govern(Mockito.anyString(), Mockito.any(), Mockito.any())).then(returnsSecondArg());

        int min = 0;
        int max = 5;
        ScalerThread t = new ScalerThread(governor, analyser, scaler, SERVICE_REF, min, max, 0,
            new Alerter(new HashMap<>(), new AlertDispatchConfiguration()), new ResourceMonitoringConfiguration());
        // should not throw
        t.run();
        Mockito.verify(scaler, Mockito.times(0)).scaleUp(Mockito.any(), Mockito.anyInt());
        Mockito.verify(scaler, Mockito.times(0)).scaleDown(Mockito.any(), Mockito.anyInt());
    }
}
