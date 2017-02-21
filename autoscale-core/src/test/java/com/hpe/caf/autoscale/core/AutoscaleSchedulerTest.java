package com.hpe.caf.autoscale.core;


import com.hpe.caf.api.autoscale.ScalingConfiguration;
import com.hpe.caf.api.autoscale.ServiceScaler;
import com.hpe.caf.api.autoscale.WorkloadAnalyser;
import com.hpe.caf.api.autoscale.WorkloadAnalyserFactory;
import org.junit.Assert;
import org.junit.Test;
import org.mockito.Mockito;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;


public class AutoscaleSchedulerTest
{
    private static final String FACTORY_A = "A";
    private static final String APP_ID_A = "A";
    private static final String APP_ID_B = "B";


    /**
     * Simply make sure that new services get added to the internal monitor.
     * Need to mock the validator to return our test set of services.
     */
    @Test
    public void testAddNewServices()
    {
        Map<String, WorkloadAnalyserFactory> factories = getTestFactories();
        ServiceScaler scaler = Mockito.mock(ServiceScaler.class);
        ScheduledExecutorService scheduler = getTestScheduler();
        Set<ScalingConfiguration> out = new HashSet<>();
        out.add(getConfigA());
        out.add(getConfigB());
        ServiceValidator validator = Mockito.mock(ServiceValidator.class);
        Mockito.when(validator.getValidatedServices(Mockito.any())).thenReturn(out);
        AutoscaleScheduler autoscale = new AutoscaleScheduler(factories, scaler, scheduler, validator);
        autoscale.updateServices(out);
        Map<String, ScheduledScalingService> ret = autoscale.getScheduledServices();
        Assert.assertTrue(ret.containsKey(APP_ID_A));
        Assert.assertTrue(ret.containsKey(APP_ID_B));
        Mockito.verify(scheduler, Mockito.times(2)).scheduleWithFixedDelay(Mockito.any(), Mockito.anyLong(), Mockito.anyLong(), Mockito.any());
    }


    /**
     * Ensure that services no longer provided by the source are removed from the internal monitor.
     */
    @Test
    public void testRemoveServices()
    {
        Map<String, WorkloadAnalyserFactory> factories = getTestFactories();
        ServiceScaler scaler = Mockito.mock(ServiceScaler.class);
        ScheduledExecutorService scheduler = getTestScheduler();
        Set<ScalingConfiguration> out = new HashSet<>();
        out.add(getConfigA());
        out.add(getConfigB());
        ServiceValidator validator = Mockito.mock(ServiceValidator.class);
        Mockito.when(validator.getValidatedServices(Mockito.any())).thenReturn(out);
        AutoscaleScheduler autoscale = new AutoscaleScheduler(factories, scaler, scheduler, validator);
        autoscale.updateServices(out);
        Map<String, ScheduledScalingService> ret = autoscale.getScheduledServices();
        Assert.assertTrue(ret.containsKey(APP_ID_A));
        Assert.assertTrue(ret.containsKey(APP_ID_B));
        Set<ScalingConfiguration> out2 = new HashSet<>();
        out2.add(getConfigA());
        Mockito.when(validator.getValidatedServices(Mockito.any())).thenReturn(out2);
        autoscale.updateServices(out2);
        Assert.assertTrue(ret.containsKey(APP_ID_A));
        Assert.assertFalse(ret.containsKey(APP_ID_B));
        Mockito.verify(scheduler, Mockito.times(2)).scheduleWithFixedDelay(Mockito.any(), Mockito.anyLong(), Mockito.anyLong(), Mockito.any());
    }


    /**
     * Ensure that services with the same id but that are not a matching object get updated.
     */
    @Test
    public void testUpdateServices()
    {
        Map<String, WorkloadAnalyserFactory> factories = getTestFactories();
        ServiceScaler scaler = Mockito.mock(ServiceScaler.class);
        ScheduledExecutorService scheduler = getTestScheduler();
        Set<ScalingConfiguration> out = new HashSet<>();
        out.add(getConfigA());
        out.add(getConfigB());
        ServiceValidator validator = Mockito.mock(ServiceValidator.class);
        Mockito.when(validator.getValidatedServices(Mockito.any())).thenReturn(out);
        AutoscaleScheduler autoscale = new AutoscaleScheduler(factories, scaler, scheduler, validator);
        autoscale.updateServices(out);
        Map<String, ScheduledScalingService> ret = autoscale.getScheduledServices();
        Assert.assertTrue(ret.containsKey(APP_ID_A));
        Assert.assertTrue(ret.containsKey(APP_ID_B));
        Assert.assertEquals(getConfigA(), ret.get(APP_ID_A).getConfig());
        Assert.assertEquals(getConfigB(), ret.get(APP_ID_B).getConfig());
        Set<ScalingConfiguration> out2 = new HashSet<>();
        out2.add(getConfigA());
        out2.add(getConfigC());
        Mockito.when(validator.getValidatedServices(Mockito.any())).thenReturn(out2);
        autoscale.updateServices(out2);
        Assert.assertTrue(ret.containsKey(APP_ID_A));
        Assert.assertTrue(ret.containsKey(APP_ID_B));
        Assert.assertEquals(getConfigA(), ret.get(APP_ID_A).getConfig());
        Assert.assertEquals(getConfigC(), ret.get(APP_ID_B).getConfig());
        Mockito.verify(scheduler, Mockito.times(3)).scheduleWithFixedDelay(Mockito.any(), Mockito.anyLong(), Mockito.anyLong(), Mockito.any());
    }


    private Map<String, WorkloadAnalyserFactory> getTestFactories()
    {
        Map<String, WorkloadAnalyserFactory> ret = new HashMap<>();
        WorkloadAnalyserFactory factory = Mockito.mock(WorkloadAnalyserFactory.class);
        WorkloadAnalyser analyser = Mockito.mock(WorkloadAnalyser.class);
        Mockito.when(factory.getAnalyser(Mockito.any(), Mockito.any())).thenReturn(analyser);
        ret.put(FACTORY_A, factory);
        return ret;
    }


    private ScheduledExecutorService getTestScheduler()
    {
        ScheduledExecutorService scheduler = Mockito.mock(ScheduledExecutorService.class);
        ScheduledFuture future = Mockito.mock(ScheduledFuture.class);
        Mockito.when(scheduler.scheduleWithFixedDelay(Mockito.any(), Mockito.anyLong(), Mockito.anyLong(), Mockito.any())).thenReturn(future);
        return scheduler;
    }


    private ScalingConfiguration getConfigA()
    {
        ScalingConfiguration ret = new ScalingConfiguration();
        ret.setId(APP_ID_A);
        ret.setWorkloadMetric(FACTORY_A);
        ret.setMaxInstances(5);
        return ret;
    }


    private ScalingConfiguration getConfigB()
    {
        ScalingConfiguration ret = new ScalingConfiguration();
        ret.setId(APP_ID_B);
        ret.setWorkloadMetric(FACTORY_A);
        ret.setMaxInstances(5);
        return ret;
    }


    private ScalingConfiguration getConfigC()
    {
        ScalingConfiguration ret = new ScalingConfiguration();
        ret.setId(APP_ID_B);
        ret.setWorkloadMetric(FACTORY_A);
        ret.setMaxInstances(10);
        return ret;
    }
}
