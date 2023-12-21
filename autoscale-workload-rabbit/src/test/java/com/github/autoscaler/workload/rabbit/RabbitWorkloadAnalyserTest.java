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
package com.github.autoscaler.workload.rabbit;

import static org.mockito.ArgumentMatchers.anyString;

import com.github.autoscaler.api.InstanceInfo;
import com.github.autoscaler.api.ResourceUtilisation;
import com.github.autoscaler.api.ScalerException;
import com.github.autoscaler.api.ScalingOperation;
import com.google.common.collect.Lists;

import org.junit.Assert;
import org.junit.Test;
import org.mockito.Mockito;

import java.util.LinkedList;
import java.util.Optional;


public class RabbitWorkloadAnalyserTest
{
    private static final String SCALING_TARGET = "testTarget";
    private static final int BACKLOG_GOAL = 1;
    private static final String STAGING_QUEUE_INDICATOR = "»";


    @Test
    public void testInitialScaleup()
            throws ScalerException
    {
        RabbitWorkloadProfile profile = new RabbitWorkloadProfile(3, BACKLOG_GOAL);
        RabbitStatsReporter stats = Mockito.mock(RabbitStatsReporter.class);
        Mockito.when(stats.getQueueStats(SCALING_TARGET)).thenReturn(new QueueStats(1, 0.0, 0.0));
        RabbitSystemResourceMonitor monitor = Mockito.mock(RabbitSystemResourceMonitor.class);
        Mockito.when(monitor.getCurrentResourceUtilisation()).thenReturn(new ResourceUtilisation(15.00, Optional.of(0)));
        RabbitWorkloadAnalyser analyser = new RabbitWorkloadAnalyser(SCALING_TARGET, stats, profile, monitor, null);
        InstanceInfo info = new InstanceInfo(0, 0, new LinkedList<>(), 1, 0);
        Assert.assertEquals(ScalingOperation.SCALE_UP, analyser.analyseWorkload(info).getOperation());
    }

    @Test
    public void testInitialScaleupWithStagingQueues()
            throws ScalerException
    {
        RabbitWorkloadProfile profile = new RabbitWorkloadProfile(3, BACKLOG_GOAL);
        RabbitStatsReporter stats = Mockito.mock(RabbitStatsReporter.class);

        // Target queue has 0 messages on it
        Mockito.when(stats.getQueueStats(SCALING_TARGET)).thenReturn(new QueueStats(0, 0.0, 0.0));

        // Staging queue 1 has 0 messages on it
        // Staging queue 2 has 1 message on it
        final String stagingQueue1Name = SCALING_TARGET + STAGING_QUEUE_INDICATOR + "staging-queue-1";
        final String stagingQueue2Name = SCALING_TARGET + STAGING_QUEUE_INDICATOR + "staging-queue-2";
        Mockito.when(stats.getStagingQueueStats(anyString())).thenReturn(Lists.newArrayList(
                new StagingQueueStats(stagingQueue1Name, 0, 0.0),
                new StagingQueueStats(stagingQueue2Name, 1, 0.0)));

        RabbitSystemResourceMonitor monitor = Mockito.mock(RabbitSystemResourceMonitor.class);
        Mockito.when(monitor.getCurrentResourceUtilisation()).thenReturn(new ResourceUtilisation(15.00, Optional.of(0)));
        RabbitWorkloadAnalyser analyser = new RabbitWorkloadAnalyser(SCALING_TARGET, stats, profile, monitor, STAGING_QUEUE_INDICATOR);

        // 0 instances running
        InstanceInfo info = new InstanceInfo(0, 0, new LinkedList<>(), 1, 0);

        // We should scale up because:
        // 1. There is a backlog of 1 message (on staging queue 2)
        // 2. There are 0 instances running, so we immediately scale up without checking consumption rate, backlog goal etc
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
        Mockito.when(monitor.getCurrentResourceUtilisation()).thenReturn(new ResourceUtilisation(15.00, Optional.of(0)));
        RabbitWorkloadAnalyser analyser = new RabbitWorkloadAnalyser(SCALING_TARGET, stats, profile, monitor, null);
        InstanceInfo info = new InstanceInfo(1, 0, new LinkedList<>(), 1, 1);
        Assert.assertEquals(ScalingOperation.NONE, analyser.analyseWorkload(info).getOperation());
        Assert.assertEquals(ScalingOperation.NONE, analyser.analyseWorkload(info).getOperation());
        Assert.assertEquals(ScalingOperation.SCALE_UP, analyser.analyseWorkload(info).getOperation());
    }

    @Test
    public void testScaleUpWithNonEmptyTargetQueueAndEmptyStagingQueues()
            throws ScalerException
    {
        RabbitWorkloadProfile profile = new RabbitWorkloadProfile(3, BACKLOG_GOAL);
        RabbitStatsReporter stats = Mockito.mock(RabbitStatsReporter.class);

        // Target queue has 1 message on it
        Mockito.when(stats.getQueueStats(SCALING_TARGET)).thenReturn(new QueueStats(1, 4.0, 0.5));

        // Staging queue 1 has 0 messages on it
        // Staging queue 2 has 0 messages on it
        final String stagingQueue1Name = SCALING_TARGET + STAGING_QUEUE_INDICATOR + "staging-queue-1";
        final String stagingQueue2Name = SCALING_TARGET + STAGING_QUEUE_INDICATOR + "staging-queue-2";
        Mockito.when(stats.getStagingQueueStats(anyString())).thenReturn(Lists.newArrayList(
                new StagingQueueStats(stagingQueue1Name, 0, 0.0),
                new StagingQueueStats(stagingQueue2Name, 0, 0.0)));

        RabbitSystemResourceMonitor monitor = Mockito.mock(RabbitSystemResourceMonitor.class);
        Mockito.when(monitor.getCurrentResourceUtilisation()).thenReturn(new ResourceUtilisation(15.00, Optional.of(0)));
        RabbitWorkloadAnalyser analyser = new RabbitWorkloadAnalyser(SCALING_TARGET, stats, profile, monitor, STAGING_QUEUE_INDICATOR);

        // 1 instance running
        InstanceInfo info = new InstanceInfo(1, 0, new LinkedList<>(), 1, 1);

        Assert.assertEquals(ScalingOperation.NONE, analyser.analyseWorkload(info).getOperation());
        Assert.assertEquals(ScalingOperation.NONE, analyser.analyseWorkload(info).getOperation());

        // We should scale up because:
        // 1. There is a backlog of 1 message on the target queue
        // 2. There is 1 instance running
        // 3. The consumption rate for the target queue is 0.5 messages per second
        // 4. The backlog goal is 1 second (i.e. we want to complete the current backlog of 1 message in 1 second)
        // 5. To process the 1 message and hit the backlog goal of 1 second, we need 2 instances
        Assert.assertEquals(ScalingOperation.SCALE_UP, analyser.analyseWorkload(info).getOperation());
    }

    @Test
    public void testScaleUpWithEmptyTargetQueueAndNonEmptyStagingQueues()
            throws ScalerException
    {
        RabbitWorkloadProfile profile = new RabbitWorkloadProfile(3, BACKLOG_GOAL);
        RabbitStatsReporter stats = Mockito.mock(RabbitStatsReporter.class);

        // Target queue has 0 messages on it
        Mockito.when(stats.getQueueStats(SCALING_TARGET)).thenReturn(new QueueStats(0, 4.0, 1.0));

        // Staging queue 1 has 1 message on it
        // Staging queue 2 has 1 message on it
        final String stagingQueue1Name = SCALING_TARGET + STAGING_QUEUE_INDICATOR + "staging-queue-1";
        final String stagingQueue2Name = SCALING_TARGET + STAGING_QUEUE_INDICATOR + "staging-queue-2";
        Mockito.when(stats.getStagingQueueStats(anyString())).thenReturn(Lists.newArrayList(
                new StagingQueueStats(stagingQueue1Name, 1, 0.0),
                new StagingQueueStats(stagingQueue2Name, 1, 0.0)));

        RabbitSystemResourceMonitor monitor = Mockito.mock(RabbitSystemResourceMonitor.class);
        Mockito.when(monitor.getCurrentResourceUtilisation()).thenReturn(new ResourceUtilisation(15.00, Optional.of(0)));
        RabbitWorkloadAnalyser analyser = new RabbitWorkloadAnalyser(SCALING_TARGET, stats, profile, monitor, STAGING_QUEUE_INDICATOR);

        // 1 instance running
        InstanceInfo info = new InstanceInfo(1, 0, new LinkedList<>(), 1, 1);

        Assert.assertEquals(ScalingOperation.NONE, analyser.analyseWorkload(info).getOperation());
        Assert.assertEquals(ScalingOperation.NONE, analyser.analyseWorkload(info).getOperation());

        // We should scale up because:
        // 1. There is a backlog of 2 messages (1 message on staging queue 1 and 1 message on staging queue 2)
        // 2. There is 1 instance running
        // 3. The consumption rate for the target queue is 1.0 message per second
        // 4. The backlog goal is 1 second (i.e. we want to complete the current backlog of 2 messages in 1 second)
        // 5. To process the 2 messages and hit the backlog goal of 1 second, we need 2 instances
        Assert.assertEquals(ScalingOperation.SCALE_UP, analyser.analyseWorkload(info).getOperation());
    }

    @Test
    public void testScaleUpWhileInstanceIsStaging()
            throws ScalerException
    {
        RabbitWorkloadProfile profile = new RabbitWorkloadProfile(3, BACKLOG_GOAL);
        RabbitStatsReporter stats = Mockito.mock(RabbitStatsReporter.class);
        Mockito.when(stats.getQueueStats(SCALING_TARGET)).thenReturn(new QueueStats(1, 4.0, 1.0));
        RabbitSystemResourceMonitor monitor = Mockito.mock(RabbitSystemResourceMonitor.class);
        Mockito.when(monitor.getCurrentResourceUtilisation()).thenReturn(new ResourceUtilisation(15.00, Optional.of(0)));
        RabbitWorkloadAnalyser analyser = new RabbitWorkloadAnalyser(SCALING_TARGET, stats, profile, monitor, null);
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
        Mockito.when(monitor.getCurrentResourceUtilisation()).thenReturn(new ResourceUtilisation(15.00, Optional.of(0)));
        RabbitWorkloadAnalyser analyser = new RabbitWorkloadAnalyser(SCALING_TARGET, stats, profile, monitor, null);
        InstanceInfo info = new InstanceInfo(2, 0, new LinkedList<>(), 1, 1);
        Assert.assertEquals(ScalingOperation.NONE, analyser.analyseWorkload(info).getOperation());
        Assert.assertEquals(ScalingOperation.NONE, analyser.analyseWorkload(info).getOperation());
        Assert.assertEquals(ScalingOperation.SCALE_DOWN, analyser.analyseWorkload(info).getOperation());
    }

    @Test
    public void testScaleDownWithNonEmptyTargetQueueAndEmptyStagingQueues()
            throws ScalerException
    {
        RabbitWorkloadProfile profile = new RabbitWorkloadProfile(3, BACKLOG_GOAL);
        RabbitStatsReporter stats = Mockito.mock(RabbitStatsReporter.class);

        // Target queue has 1 message on it
        Mockito.when(stats.getQueueStats(SCALING_TARGET)).thenReturn(new QueueStats(1, 0.0, 2.0));

        // Staging queue 1 has 0 messages on it
        // Staging queue 2 has 0 messages on it
        final String stagingQueue1Name = SCALING_TARGET + STAGING_QUEUE_INDICATOR + "staging-queue-1";
        final String stagingQueue2Name = SCALING_TARGET + STAGING_QUEUE_INDICATOR + "staging-queue-2";
        Mockito.when(stats.getStagingQueueStats(anyString())).thenReturn(Lists.newArrayList(
                new StagingQueueStats(stagingQueue1Name, 0, 0.0),
                new StagingQueueStats(stagingQueue2Name, 0, 0.0)));

        RabbitSystemResourceMonitor monitor = Mockito.mock(RabbitSystemResourceMonitor.class);
        Mockito.when(monitor.getCurrentResourceUtilisation()).thenReturn(new ResourceUtilisation(15.00, Optional.of(0)));
        RabbitWorkloadAnalyser analyser = new RabbitWorkloadAnalyser(SCALING_TARGET, stats, profile, monitor, STAGING_QUEUE_INDICATOR);

        // 2 instances running
        InstanceInfo info = new InstanceInfo(2, 0, new LinkedList<>(), 1, 1);

        Assert.assertEquals(ScalingOperation.NONE, analyser.analyseWorkload(info).getOperation());
        Assert.assertEquals(ScalingOperation.NONE, analyser.analyseWorkload(info).getOperation());

        // We should scale down because:
        // 1. There is a backlog of 1 message on the target queue
        // 2. There are 2 instances running
        // 3. The consumption rate for the target queue is 2.0 messages per second
        // 4. The backlog goal is 1 second (i.e. we want to complete the current backlog of 1 message in 1 second)
        // 5. We only need 1 instance to hit the backlog goal of 1 second because the target queue is consuming 2.0 messages per second
        Assert.assertEquals(ScalingOperation.SCALE_DOWN, analyser.analyseWorkload(info).getOperation());
    }

    @Test
    public void testScaleDownWithEmptyTargetQueueAndNonEmptyStagingQueues()
            throws ScalerException
    {
        RabbitWorkloadProfile profile = new RabbitWorkloadProfile(3, BACKLOG_GOAL);
        RabbitStatsReporter stats = Mockito.mock(RabbitStatsReporter.class);

        // Target queue has 0 messages on it
        Mockito.when(stats.getQueueStats(SCALING_TARGET)).thenReturn(new QueueStats(0, 0.0, 2.0));

        // Staging queue 1 has 0 messages on it
        // Staging queue 2 has 1 message on it
        final String stagingQueue1Name = SCALING_TARGET + STAGING_QUEUE_INDICATOR + "staging-queue-1";
        final String stagingQueue2Name = SCALING_TARGET + STAGING_QUEUE_INDICATOR + "staging-queue-2";
        Mockito.when(stats.getStagingQueueStats(anyString())).thenReturn(Lists.newArrayList(
                new StagingQueueStats(stagingQueue1Name, 0, 0.0),
                new StagingQueueStats(stagingQueue2Name, 1, 0.0)));

        RabbitSystemResourceMonitor monitor = Mockito.mock(RabbitSystemResourceMonitor.class);
        Mockito.when(monitor.getCurrentResourceUtilisation()).thenReturn(new ResourceUtilisation(15.00, Optional.of(0)));
        RabbitWorkloadAnalyser analyser = new RabbitWorkloadAnalyser(SCALING_TARGET, stats, profile, monitor, STAGING_QUEUE_INDICATOR);

        // 2 instances running
        InstanceInfo info = new InstanceInfo(2, 0, new LinkedList<>(), 1, 1);

        Assert.assertEquals(ScalingOperation.NONE, analyser.analyseWorkload(info).getOperation());
        Assert.assertEquals(ScalingOperation.NONE, analyser.analyseWorkload(info).getOperation());

        // We should scale down because:
        // 1. There is a backlog of 1 message (on staging queue 2)
        // 2. There are 2 instances running
        // 3. The consumption rate for the target queue is 2.0 messages per second
        // 4. The backlog goal is 1 second (i.e. we want to complete the current backlog of 1 message in 1 second)
        // 5. We only need 1 instance to hit the backlog goal of 1 second because the target queue is consuming 2.0 messages per second
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
        Mockito.when(monitor.getCurrentResourceUtilisation()).thenReturn(new ResourceUtilisation(15.00, Optional.of(0)));
        RabbitWorkloadAnalyser analyser = new RabbitWorkloadAnalyser(SCALING_TARGET, stats, profile, monitor, null);
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
        Mockito.when(monitor.getCurrentResourceUtilisation()).thenReturn(new ResourceUtilisation(15.00, Optional.of(0)));
        RabbitWorkloadAnalyser analyser = new RabbitWorkloadAnalyser(SCALING_TARGET, stats, profile, monitor, null);
        InstanceInfo info = new InstanceInfo(1, 0, new LinkedList<>(), 1, 1);
        Mockito.when(stats.getQueueStats(SCALING_TARGET)).thenReturn(new QueueStats(100, 4.0, 1.0));
        Assert.assertEquals(ScalingOperation.NONE, analyser.analyseWorkload(info).getOperation());
        Mockito.when(stats.getQueueStats(SCALING_TARGET)).thenReturn(new QueueStats(100, 1.0, 4.0));
        Assert.assertEquals(ScalingOperation.NONE, analyser.analyseWorkload(info).getOperation());
        Mockito.when(stats.getQueueStats(SCALING_TARGET)).thenReturn(new QueueStats(100, 4.0, 1.0));
        Assert.assertEquals(ScalingOperation.SCALE_UP, analyser.analyseWorkload(info).getOperation());
    }

    @Test
    public void testScaleUpOnAverageWithStagingQueues()
            throws ScalerException
    {
        RabbitWorkloadProfile profile = new RabbitWorkloadProfile(3, BACKLOG_GOAL);
        RabbitStatsReporter stats = Mockito.mock(RabbitStatsReporter.class);
        RabbitSystemResourceMonitor monitor = Mockito.mock(RabbitSystemResourceMonitor.class);
        Mockito.when(monitor.getCurrentResourceUtilisation()).thenReturn(new ResourceUtilisation(15.00, Optional.of(0)));
        RabbitWorkloadAnalyser analyser = new RabbitWorkloadAnalyser(SCALING_TARGET, stats, profile, monitor, STAGING_QUEUE_INDICATOR);

        // 1 instance running
        InstanceInfo info = new InstanceInfo(1, 0, new LinkedList<>(), 1, 1);

        final String stagingQueue1Name = SCALING_TARGET + STAGING_QUEUE_INDICATOR + "staging-queue-1";
        final String stagingQueue2Name = SCALING_TARGET + STAGING_QUEUE_INDICATOR + "staging-queue-2";

        // Consumption rate is 1.0 messages per second
        Mockito.when(stats.getQueueStats(SCALING_TARGET)).thenReturn(new QueueStats(1, 4.0, 1.0));
        Mockito.when(stats.getStagingQueueStats(anyString())).thenReturn(Lists.newArrayList(
                new StagingQueueStats(stagingQueue1Name, 1, 0.0),
                new StagingQueueStats(stagingQueue2Name, 1, 0.0)));
        Assert.assertEquals(ScalingOperation.NONE, analyser.analyseWorkload(info).getOperation());

        // Consumption rate is 4.0 messages per second
        Mockito.when(stats.getQueueStats(SCALING_TARGET)).thenReturn(new QueueStats(1, 1.0, 4.0));
        Mockito.when(stats.getStagingQueueStats(anyString())).thenReturn(Lists.newArrayList(
                new StagingQueueStats(stagingQueue1Name, 1, 0.0),
                new StagingQueueStats(stagingQueue2Name, 1, 0.0)));
        Assert.assertEquals(ScalingOperation.NONE, analyser.analyseWorkload(info).getOperation());

        // Consumption rate is 1.0 messages per second
        Mockito.when(stats.getQueueStats(SCALING_TARGET)).thenReturn(new QueueStats(1, 4.0, 1.0));
        Mockito.when(stats.getStagingQueueStats(anyString())).thenReturn(Lists.newArrayList(
                new StagingQueueStats(stagingQueue1Name, 1, 0.0),
                new StagingQueueStats(stagingQueue2Name, 1, 0.0)));

        // We should scale up because:
        // 1. There is a backlog of 3 messages (1 message on the target queue, and 1 message on each staging queue)
        // 2. There is 1 instance running
        // 3. The average consumption rate for the target queue over the past 3 times is 2.0 messages per second
        // 4. The backlog goal is 1 second (i.e. we want to complete the current backlog of 3 messages in 1 second)
        // 5. To process the 3 messages and hit the backlog goal of 1 second, we need 2 instances
        Assert.assertEquals(ScalingOperation.SCALE_UP, analyser.analyseWorkload(info).getOperation());
    }

    @Test
    public void testScaleDownOnAverage()
            throws ScalerException
    {
        RabbitWorkloadProfile profile = new RabbitWorkloadProfile(3, BACKLOG_GOAL);
        RabbitStatsReporter stats = Mockito.mock(RabbitStatsReporter.class);
        RabbitSystemResourceMonitor monitor = Mockito.mock(RabbitSystemResourceMonitor.class);
        Mockito.when(monitor.getCurrentResourceUtilisation()).thenReturn(new ResourceUtilisation(15.00, Optional.of(0)));
        RabbitWorkloadAnalyser analyser = new RabbitWorkloadAnalyser(SCALING_TARGET, stats, profile, monitor, null);
        InstanceInfo info = new InstanceInfo(2, 0, new LinkedList<>(), 1, 1);
        Mockito.when(stats.getQueueStats(SCALING_TARGET)).thenReturn(new QueueStats(1, 1.0, 3.0));
        Assert.assertEquals(ScalingOperation.NONE, analyser.analyseWorkload(info).getOperation());
        Mockito.when(stats.getQueueStats(SCALING_TARGET)).thenReturn(new QueueStats(1, 4.0, 2.0));
        Assert.assertEquals(ScalingOperation.NONE, analyser.analyseWorkload(info).getOperation());
        Mockito.when(stats.getQueueStats(SCALING_TARGET)).thenReturn(new QueueStats(1, 1.0, 1.0));
        Assert.assertEquals(ScalingOperation.SCALE_DOWN, analyser.analyseWorkload(info).getOperation());
    }

    @Test
    public void testScaleDownOnAverageWithStagingQueues()
            throws ScalerException
    {
        RabbitWorkloadProfile profile = new RabbitWorkloadProfile(3, BACKLOG_GOAL);
        RabbitStatsReporter stats = Mockito.mock(RabbitStatsReporter.class);
        RabbitSystemResourceMonitor monitor = Mockito.mock(RabbitSystemResourceMonitor.class);
        Mockito.when(monitor.getCurrentResourceUtilisation()).thenReturn(new ResourceUtilisation(15.00, Optional.of(0)));
        RabbitWorkloadAnalyser analyser = new RabbitWorkloadAnalyser(SCALING_TARGET, stats, profile, monitor, STAGING_QUEUE_INDICATOR);

        // 2 instances running
        InstanceInfo info = new InstanceInfo(2, 0, new LinkedList<>(), 1, 1);

        final String stagingQueue1Name = SCALING_TARGET + STAGING_QUEUE_INDICATOR + "staging-queue-1";
        final String stagingQueue2Name = SCALING_TARGET + STAGING_QUEUE_INDICATOR + "staging-queue-2";

        // Consumption rate is 3.0 messages per second
        Mockito.when(stats.getQueueStats(SCALING_TARGET)).thenReturn(new QueueStats(0, 1.0, 3.0));
        Mockito.when(stats.getStagingQueueStats(SCALING_TARGET)).thenReturn(Lists.newArrayList(
                new StagingQueueStats(stagingQueue1Name, 0, 0.0),
                new StagingQueueStats(stagingQueue2Name, 1, 0.0)));
        Assert.assertEquals(ScalingOperation.NONE, analyser.analyseWorkload(info).getOperation());

        // Consumption rate is 2.0 messages per second
        Mockito.when(stats.getQueueStats(SCALING_TARGET)).thenReturn(new QueueStats(0, 4.0, 2.0));
        Mockito.when(stats.getStagingQueueStats(SCALING_TARGET)).thenReturn(Lists.newArrayList(
                new StagingQueueStats(stagingQueue1Name, 0, 0.0),
                new StagingQueueStats(stagingQueue2Name, 1, 0.0)));
        Assert.assertEquals(ScalingOperation.NONE, analyser.analyseWorkload(info).getOperation());

        // Consumption rate is 1.0 messages per second
        Mockito.when(stats.getQueueStats(SCALING_TARGET)).thenReturn(new QueueStats(0, 1.0, 1.0));
        Mockito.when(stats.getStagingQueueStats(SCALING_TARGET)).thenReturn(Lists.newArrayList(
                new StagingQueueStats(stagingQueue1Name, 0, 0.0),
                new StagingQueueStats(stagingQueue2Name, 1, 0.0)));

        // We should scale down because:
        // 1. There is a backlog of 1 message (on staging queue 2)
        // 2. There are 2 instances running
        // 3. The average consumption rate for the target queue over the past 3 times is 2.0 messages per second
        // 4. The backlog goal is 1 second (i.e. we want to complete the current backlog of 1 message in 1 second)
        // 5. We only need 1 instance to hit the backlog goal of 1 second because the target queue is consuming 2.0 messages per second
        Assert.assertEquals(ScalingOperation.SCALE_DOWN, analyser.analyseWorkload(info).getOperation());
    }

    @Test
    public void testSteadyState()
            throws ScalerException
    {
        RabbitWorkloadProfile profile = new RabbitWorkloadProfile(3, BACKLOG_GOAL);
        RabbitStatsReporter stats = Mockito.mock(RabbitStatsReporter.class);
        RabbitSystemResourceMonitor monitor = Mockito.mock(RabbitSystemResourceMonitor.class);
        Mockito.when(monitor.getCurrentResourceUtilisation()).thenReturn(new ResourceUtilisation(15.00, Optional.of(0)));
        RabbitWorkloadAnalyser analyser = new RabbitWorkloadAnalyser(SCALING_TARGET, stats, profile, monitor, STAGING_QUEUE_INDICATOR);
        InstanceInfo info = new InstanceInfo(1, 0, new LinkedList<>(), 1, 1);
        Mockito.when(stats.getQueueStats(SCALING_TARGET)).thenReturn(new QueueStats(1, 1.0, 1.0));
        Assert.assertEquals(ScalingOperation.NONE, analyser.analyseWorkload(info).getOperation());
        Mockito.when(stats.getQueueStats(SCALING_TARGET)).thenReturn(new QueueStats(1, 1.0, 1.0));
        Assert.assertEquals(ScalingOperation.NONE, analyser.analyseWorkload(info).getOperation());
        Mockito.when(stats.getQueueStats(SCALING_TARGET)).thenReturn(new QueueStats(1, 1.0, 1.0));
        Assert.assertEquals(ScalingOperation.NONE, analyser.analyseWorkload(info).getOperation());
    }
}
