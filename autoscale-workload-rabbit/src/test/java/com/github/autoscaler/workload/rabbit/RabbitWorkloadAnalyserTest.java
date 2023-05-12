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
package com.github.autoscaler.workload.rabbit;

import com.github.autoscaler.api.InstanceInfo;
import com.github.autoscaler.api.ResourceUtilisation;
import com.github.autoscaler.api.ScalerException;
import com.github.autoscaler.api.ScalingOperation;
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
        RabbitSystemResourceMonitor monitor = Mockito.mock(RabbitSystemResourceMonitor.class);
        Mockito.when(monitor.getCurrentResourceUtilisation()).thenReturn(new ResourceUtilisation(15.00));
        RabbitWorkloadAnalyser analyser = new RabbitWorkloadAnalyser(SCALING_TARGET, stats, profile, monitor);
        InstanceInfo info = new InstanceInfo(0, 0, new LinkedList<>(), 1, 0);
        Assert.assertEquals(ScalingOperation.SCALE_UP, analyser.analyseWorkload(info).getOperation());
    }


    @Test
    public void testScaleUp()
            throws ScalerException
    {
        RabbitWorkloadProfile profile = new RabbitWorkloadProfile(3, BACKLOG_GOAL);
        RabbitStatsReporter stats = Mockito.mock(RabbitStatsReporter.class);
        Mockito.when(stats.getQueueStats(SCALING_TARGET)).thenReturn(new QueueStats(1, 4.0, 0.1));
        RabbitSystemResourceMonitor monitor = Mockito.mock(RabbitSystemResourceMonitor.class);
        Mockito.when(monitor.getCurrentResourceUtilisation()).thenReturn(new ResourceUtilisation(15.00));
        RabbitWorkloadAnalyser analyser = new RabbitWorkloadAnalyser(SCALING_TARGET, stats, profile, monitor);
        InstanceInfo info = new InstanceInfo(1, 0, new LinkedList<>(), 1, 1);
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
        RabbitSystemResourceMonitor monitor = Mockito.mock(RabbitSystemResourceMonitor.class);
        Mockito.when(monitor.getCurrentResourceUtilisation()).thenReturn(new ResourceUtilisation(15.00));
        RabbitWorkloadAnalyser analyser = new RabbitWorkloadAnalyser(SCALING_TARGET, stats, profile, monitor);
        InstanceInfo info = new InstanceInfo(1, 1, new LinkedList<>(), 1, 2);
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
        RabbitSystemResourceMonitor monitor = Mockito.mock(RabbitSystemResourceMonitor.class);
        Mockito.when(monitor.getCurrentResourceUtilisation()).thenReturn(new ResourceUtilisation(15.00));
        RabbitWorkloadAnalyser analyser = new RabbitWorkloadAnalyser(SCALING_TARGET, stats, profile, monitor);
        InstanceInfo info = new InstanceInfo(2, 0, new LinkedList<>(), 1, 1);
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
        RabbitSystemResourceMonitor monitor = Mockito.mock(RabbitSystemResourceMonitor.class);
        Mockito.when(monitor.getCurrentResourceUtilisation()).thenReturn(new ResourceUtilisation(15.00));
        RabbitWorkloadAnalyser analyser = new RabbitWorkloadAnalyser(SCALING_TARGET, stats, profile, monitor);
        InstanceInfo info = new InstanceInfo(1, 0, new LinkedList<>(), 1, 1);
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
        RabbitSystemResourceMonitor monitor = Mockito.mock(RabbitSystemResourceMonitor.class);
        Mockito.when(monitor.getCurrentResourceUtilisation()).thenReturn(new ResourceUtilisation(15.00));
        RabbitWorkloadAnalyser analyser = new RabbitWorkloadAnalyser(SCALING_TARGET, stats, profile, monitor);
        InstanceInfo info = new InstanceInfo(1, 0, new LinkedList<>(), 1, 1);
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
        RabbitSystemResourceMonitor monitor = Mockito.mock(RabbitSystemResourceMonitor.class);
        Mockito.when(monitor.getCurrentResourceUtilisation()).thenReturn(new ResourceUtilisation(15.00));
        RabbitWorkloadAnalyser analyser = new RabbitWorkloadAnalyser(SCALING_TARGET, stats, profile, monitor);
        InstanceInfo info = new InstanceInfo(1, 0, new LinkedList<>(), 1, 1);
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
        RabbitSystemResourceMonitor monitor = Mockito.mock(RabbitSystemResourceMonitor.class);
        Mockito.when(monitor.getCurrentResourceUtilisation()).thenReturn(new ResourceUtilisation(15.00));
        RabbitWorkloadAnalyser analyser = new RabbitWorkloadAnalyser(SCALING_TARGET, stats, profile, monitor);
        InstanceInfo info = new InstanceInfo(1, 0, new LinkedList<>(), 1, 1);
        Mockito.when(stats.getQueueStats(SCALING_TARGET)).thenReturn(new QueueStats(1, 1.0, 1.0));
        Assert.assertEquals(ScalingOperation.NONE, analyser.analyseWorkload(info).getOperation());
        Mockito.when(stats.getQueueStats(SCALING_TARGET)).thenReturn(new QueueStats(1, 1.0, 1.0));
        Assert.assertEquals(ScalingOperation.NONE, analyser.analyseWorkload(info).getOperation());
        Mockito.when(stats.getQueueStats(SCALING_TARGET)).thenReturn(new QueueStats(1, 1.0, 1.0));
        Assert.assertEquals(ScalingOperation.NONE, analyser.analyseWorkload(info).getOperation());
    }
}
