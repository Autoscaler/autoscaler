package com.hpe.caf.autoscale.workload.rabbit;


import com.hpe.caf.api.autoscale.InstanceInfo;
import com.hpe.caf.api.autoscale.ScalerException;
import com.hpe.caf.api.autoscale.ScalingOperation;
import org.junit.Assert;
import org.junit.Test;
import org.mockito.Mockito;

import java.util.LinkedList;


public class RabbitWorkloadAnalyserTest
{
    private static final String SCALING_TARGET = "testTarget";
    private static final int BACKLOG_GOAL = 1;


    @Test
    public void testInitialScaleup()
            throws ScalerException
    {
        RabbitWorkloadProfile profile = new RabbitWorkloadProfile(3, BACKLOG_GOAL);
        RabbitStatsReporter stats = Mockito.mock(RabbitStatsReporter.class);
        Mockito.when(stats.getQueueStats(SCALING_TARGET)).thenReturn(new QueueStats(1, 0.0, 0.0));
        RabbitWorkloadAnalyser analyser = new RabbitWorkloadAnalyser(SCALING_TARGET, stats, profile);
        InstanceInfo info = new InstanceInfo(0, 0, new LinkedList<>());
        Assert.assertEquals(ScalingOperation.SCALE_UP, analyser.analyseWorkload(info).getOperation());
    }


    @Test
    public void testScaleUp()
            throws ScalerException
    {
        RabbitWorkloadProfile profile = new RabbitWorkloadProfile(3, BACKLOG_GOAL);
        RabbitStatsReporter stats = Mockito.mock(RabbitStatsReporter.class);
        Mockito.when(stats.getQueueStats(SCALING_TARGET)).thenReturn(new QueueStats(1, 4.0, 0.1));
        RabbitWorkloadAnalyser analyser = new RabbitWorkloadAnalyser(SCALING_TARGET, stats, profile);
        InstanceInfo info = new InstanceInfo(1, 0, new LinkedList<>());
        Assert.assertEquals(ScalingOperation.NONE, analyser.analyseWorkload(info).getOperation());
        Assert.assertEquals(ScalingOperation.NONE, analyser.analyseWorkload(info).getOperation());
        Assert.assertEquals(ScalingOperation.SCALE_UP, analyser.analyseWorkload(info).getOperation());
    }


    @Test
    public void testScaleUpWithStaging()
            throws ScalerException
    {
        RabbitWorkloadProfile profile = new RabbitWorkloadProfile(3, BACKLOG_GOAL);
        RabbitStatsReporter stats = Mockito.mock(RabbitStatsReporter.class);
        Mockito.when(stats.getQueueStats(SCALING_TARGET)).thenReturn(new QueueStats(1, 4.0, 1.0));
        RabbitWorkloadAnalyser analyser = new RabbitWorkloadAnalyser(SCALING_TARGET, stats, profile);
        InstanceInfo info = new InstanceInfo(1, 1, new LinkedList<>());
        Assert.assertEquals(ScalingOperation.NONE, analyser.analyseWorkload(info).getOperation());
        Assert.assertEquals(ScalingOperation.NONE, analyser.analyseWorkload(info).getOperation());
        Assert.assertEquals(ScalingOperation.NONE, analyser.analyseWorkload(info).getOperation());
    }


    @Test
    public void testScaleDown()
            throws ScalerException
    {
        RabbitWorkloadProfile profile = new RabbitWorkloadProfile(3, BACKLOG_GOAL);
        RabbitStatsReporter stats = Mockito.mock(RabbitStatsReporter.class);
        Mockito.when(stats.getQueueStats(SCALING_TARGET)).thenReturn(new QueueStats(1, 0.0, 4.0));
        RabbitWorkloadAnalyser analyser = new RabbitWorkloadAnalyser(SCALING_TARGET, stats, profile);
        InstanceInfo info = new InstanceInfo(2, 0, new LinkedList<>());
        Assert.assertEquals(ScalingOperation.NONE, analyser.analyseWorkload(info).getOperation());
        Assert.assertEquals(ScalingOperation.NONE, analyser.analyseWorkload(info).getOperation());
        Assert.assertEquals(ScalingOperation.SCALE_DOWN, analyser.analyseWorkload(info).getOperation());
    }


    @Test
    public void testScaleDownNoRate()
            throws ScalerException
    {
        RabbitWorkloadProfile profile = new RabbitWorkloadProfile(3, BACKLOG_GOAL);
        RabbitStatsReporter stats = Mockito.mock(RabbitStatsReporter.class);
        Mockito.when(stats.getQueueStats(SCALING_TARGET)).thenReturn(new QueueStats(0, 0.0, 0.0));
        RabbitWorkloadAnalyser analyser = new RabbitWorkloadAnalyser(SCALING_TARGET, stats, profile);
        InstanceInfo info = new InstanceInfo(1, 0, new LinkedList<>());
        Assert.assertEquals(ScalingOperation.NONE, analyser.analyseWorkload(info).getOperation());
        Assert.assertEquals(ScalingOperation.NONE, analyser.analyseWorkload(info).getOperation());
        Assert.assertEquals(ScalingOperation.SCALE_DOWN, analyser.analyseWorkload(info).getOperation());
    }


    @Test
    public void testScaleUpOnAverage()
            throws ScalerException
    {
        RabbitWorkloadProfile profile = new RabbitWorkloadProfile(3, BACKLOG_GOAL);
        RabbitStatsReporter stats = Mockito.mock(RabbitStatsReporter.class);
        RabbitWorkloadAnalyser analyser = new RabbitWorkloadAnalyser(SCALING_TARGET, stats, profile);
        InstanceInfo info = new InstanceInfo(1, 0, new LinkedList<>());
        Mockito.when(stats.getQueueStats(SCALING_TARGET)).thenReturn(new QueueStats(100, 4.0, 1.0));
        Assert.assertEquals(ScalingOperation.NONE, analyser.analyseWorkload(info).getOperation());
        Mockito.when(stats.getQueueStats(SCALING_TARGET)).thenReturn(new QueueStats(100, 1.0, 4.0));
        Assert.assertEquals(ScalingOperation.NONE, analyser.analyseWorkload(info).getOperation());
        Mockito.when(stats.getQueueStats(SCALING_TARGET)).thenReturn(new QueueStats(100, 4.0, 1.0));
        Assert.assertEquals(ScalingOperation.SCALE_UP, analyser.analyseWorkload(info).getOperation());
    }


    @Test
    public void testScaleDownOnAverage()
            throws ScalerException
    {
        RabbitWorkloadProfile profile = new RabbitWorkloadProfile(3, BACKLOG_GOAL);
        RabbitStatsReporter stats = Mockito.mock(RabbitStatsReporter.class);
        RabbitWorkloadAnalyser analyser = new RabbitWorkloadAnalyser(SCALING_TARGET, stats, profile);
        InstanceInfo info = new InstanceInfo(1, 0, new LinkedList<>());
        Mockito.when(stats.getQueueStats(SCALING_TARGET)).thenReturn(new QueueStats(100, 1.0, 4.0));
        Assert.assertEquals(ScalingOperation.NONE, analyser.analyseWorkload(info).getOperation());
        Mockito.when(stats.getQueueStats(SCALING_TARGET)).thenReturn(new QueueStats(100, 4.0, 1.0));
        Assert.assertEquals(ScalingOperation.NONE, analyser.analyseWorkload(info).getOperation());
        Mockito.when(stats.getQueueStats(SCALING_TARGET)).thenReturn(new QueueStats(100, 1.0, 4.0));
        Assert.assertEquals(ScalingOperation.SCALE_UP, analyser.analyseWorkload(info).getOperation());
    }


    @Test
    public void testSteadyState()
            throws ScalerException
    {
        RabbitWorkloadProfile profile = new RabbitWorkloadProfile(3, BACKLOG_GOAL);
        RabbitStatsReporter stats = Mockito.mock(RabbitStatsReporter.class);
        RabbitWorkloadAnalyser analyser = new RabbitWorkloadAnalyser(SCALING_TARGET, stats, profile);
        InstanceInfo info = new InstanceInfo(1, 0, new LinkedList<>());
        Mockito.when(stats.getQueueStats(SCALING_TARGET)).thenReturn(new QueueStats(1, 1.0, 1.0));
        Assert.assertEquals(ScalingOperation.NONE, analyser.analyseWorkload(info).getOperation());
        Mockito.when(stats.getQueueStats(SCALING_TARGET)).thenReturn(new QueueStats(1, 1.0, 1.0));
        Assert.assertEquals(ScalingOperation.NONE, analyser.analyseWorkload(info).getOperation());
        Mockito.when(stats.getQueueStats(SCALING_TARGET)).thenReturn(new QueueStats(1, 1.0, 1.0));
        Assert.assertEquals(ScalingOperation.NONE, analyser.analyseWorkload(info).getOperation());
    }
}
