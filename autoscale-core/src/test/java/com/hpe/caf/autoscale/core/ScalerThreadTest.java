package com.hpe.caf.autoscale.core;


import com.hpe.caf.api.autoscale.InstanceInfo;
import com.hpe.caf.api.autoscale.ScalerException;
import com.hpe.caf.api.autoscale.ScalingAction;
import com.hpe.caf.api.autoscale.ScalingOperation;
import com.hpe.caf.api.autoscale.ServiceScaler;
import com.hpe.caf.api.autoscale.WorkloadAnalyser;
import org.junit.Test;
import org.mockito.Mockito;

import java.util.LinkedList;

import static org.mockito.AdditionalAnswers.returnsFirstArg;
import static org.mockito.AdditionalAnswers.returnsSecondArg;


public class ScalerThreadTest
{
    private final static String SERVICE_REF = "unitTest";


    @Test
    public void testFirstRunNoScaling()
            throws ScalerException
    {
        WorkloadAnalyser analyser = Mockito.mock(WorkloadAnalyser.class);
        ServiceScaler scaler = Mockito.mock(ServiceScaler.class);
        InstanceInfo info = new InstanceInfo(0, 0, new LinkedList<>());
        Mockito.when(scaler.getInstanceInfo(SERVICE_REF)).thenReturn(info);
        Governor governor = Mockito.mock(Governor.class);
        Mockito.when(governor.govern(Mockito.anyString(), Mockito.any())).then(returnsSecondArg());

        int min = 0;
        int max = 5;
        ScalerThread t = new ScalerThread(governor, analyser, scaler, SERVICE_REF, min, max, 0);
        t.run();
        Mockito.verify(analyser, Mockito.times(0)).analyseWorkload(Mockito.any());
        Mockito.verify(scaler, Mockito.times(0)).scaleUp(Mockito.any(), Mockito.anyInt());
        Mockito.verify(scaler, Mockito.times(0)).scaleDown(Mockito.any(), Mockito.anyInt());
    }


    @Test
    public void testFirstRunScaleUp()
            throws ScalerException
    {
        WorkloadAnalyser analyser = Mockito.mock(WorkloadAnalyser.class);
        ServiceScaler scaler = Mockito.mock(ServiceScaler.class);
        InstanceInfo info = new InstanceInfo(0, 0, new LinkedList<>());
        Mockito.when(scaler.getInstanceInfo(SERVICE_REF)).thenReturn(info);
        Governor governor = Mockito.mock(Governor.class);
        Mockito.when(governor.govern(Mockito.anyString(), Mockito.any())).then(returnsSecondArg());

        int min = 1;
        int max = 5;
        ScalerThread t = new ScalerThread(governor, analyser, scaler, SERVICE_REF, min, max, 0);
        t.run();
        Mockito.verify(analyser, Mockito.times(0)).analyseWorkload(Mockito.any());
        Mockito.verify(scaler, Mockito.times(1)).scaleUp(SERVICE_REF, 1);
        Mockito.verify(scaler, Mockito.times(0)).scaleDown(Mockito.any(), Mockito.anyInt());
    }


    @Test
    public void testFirstRunScaleDown()
            throws ScalerException
    {
        WorkloadAnalyser analyser = Mockito.mock(WorkloadAnalyser.class);
        ServiceScaler scaler = Mockito.mock(ServiceScaler.class);
        InstanceInfo info = new InstanceInfo(7, 0, new LinkedList<>());
        Mockito.when(scaler.getInstanceInfo(SERVICE_REF)).thenReturn(info);
        Governor governor = Mockito.mock(Governor.class);
        Mockito.when(governor.govern(Mockito.anyString(), Mockito.any())).then(returnsSecondArg());

        int min = 1;
        int max = 5;
        ScalerThread t = new ScalerThread(governor, analyser, scaler, SERVICE_REF, min, max, 0);
        t.run();
        Mockito.verify(analyser, Mockito.times(0)).analyseWorkload(Mockito.any());
        Mockito.verify(scaler, Mockito.times(0)).scaleUp(Mockito.any(), Mockito.anyInt());
        Mockito.verify(scaler, Mockito.times(1)).scaleDown(SERVICE_REF, 2);
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
        Mockito.when(governor.govern(Mockito.anyString(), Mockito.any())).then(returnsSecondArg());

        int min = 0;
        int max = 5;
        ScalerThread t = new ScalerThread(governor, analyser, scaler, SERVICE_REF, min, max, 0);
        t.run();
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
        Mockito.when(governor.govern(Mockito.anyString(), Mockito.any())).then(returnsSecondArg());

        int min = 0;
        int max = 5;
        ScalerThread t = new ScalerThread(governor, analyser, scaler, SERVICE_REF, min, max, 0);
        t.run();
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
        Mockito.when(governor.govern(Mockito.anyString(), Mockito.any())).then(returnsSecondArg());

        int min = 0;
        int max = 5;
        ScalerThread t = new ScalerThread(governor, analyser, scaler, SERVICE_REF, min, max, 0);
        t.run();
        Mockito.when(analyser.analyseWorkload(info)).thenReturn(ScalingAction.NO_ACTION);
        t.run();
        Mockito.verify(scaler, Mockito.times(0)).scaleUp(Mockito.any(), Mockito.anyInt());
        Mockito.verify(scaler, Mockito.times(0)).scaleDown(Mockito.any(), Mockito.anyInt());
    }


    @Test
    public void testScaleUpLimit()
            throws ScalerException
    {
        WorkloadAnalyser analyser = Mockito.mock(WorkloadAnalyser.class);
        ServiceScaler scaler = Mockito.mock(ServiceScaler.class);
        InstanceInfo info = new InstanceInfo(4, 0, new LinkedList<>());
        Mockito.when(scaler.getInstanceInfo(SERVICE_REF)).thenReturn(info);
        Governor governor = Mockito.mock(Governor.class);
        Mockito.when(governor.govern(Mockito.anyString(), Mockito.any())).then(returnsSecondArg());

        int min = 0;
        int max = 5;
        ScalerThread t = new ScalerThread(governor, analyser, scaler, SERVICE_REF, min, max, 0);
        t.run();
        Mockito.when(analyser.analyseWorkload(info)).thenReturn(new ScalingAction(ScalingOperation.SCALE_UP, 2));
        t.run();
        Mockito.verify(scaler, Mockito.times(1)).scaleUp(SERVICE_REF, 1);
    }


    @Test
    public void testScaleDownLimit()
            throws ScalerException
    {
        WorkloadAnalyser analyser = Mockito.mock(WorkloadAnalyser.class);
        ServiceScaler scaler = Mockito.mock(ServiceScaler.class);
        InstanceInfo info = new InstanceInfo(1, 0, new LinkedList<>());
        Mockito.when(scaler.getInstanceInfo(SERVICE_REF)).thenReturn(info);
        Governor governor = Mockito.mock(Governor.class);
        Mockito.when(governor.govern(Mockito.anyString(), Mockito.any())).then(returnsSecondArg());

        int min = 0;
        int max = 5;
        ScalerThread t = new ScalerThread(governor, analyser, scaler, SERVICE_REF, min, max, 0);
        t.run();
        Mockito.when(analyser.analyseWorkload(info)).thenReturn(new ScalingAction(ScalingOperation.SCALE_DOWN, 2));
        t.run();
        Mockito.verify(scaler, Mockito.times(1)).scaleDown(SERVICE_REF, 1);
    }
}
