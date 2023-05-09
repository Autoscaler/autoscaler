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


import static java.util.stream.Collectors.toList;

import com.github.autoscaler.api.InstanceInfo;
import com.github.autoscaler.api.ScalerException;
import com.github.autoscaler.api.ScalingAction;
import com.github.autoscaler.api.ScalingOperation;
import com.github.autoscaler.api.WorkloadAnalyser;
import com.google.common.collect.EvictingQueue;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Objects;


/**
 * Performs analysis over time on results returned from a RabbitStatsReporter and
 * makes scaling recommendations based upon it.
 */
public class RabbitWorkloadAnalyser implements WorkloadAnalyser
{
    private long counter = 0;
    private final RabbitWorkloadProfile profile;
    private final String scalingTarget;
    private final RabbitStatsReporter rabbitStats;
    private final RabbitSystemResourceMonitor rabbitResourceMonitor;
    private final EvictingQueue<QueueStats> targetQueueStatsQueue;
    private final EvictingQueue<List<StagingQueueStats>> stagingQueuesStatsQueue;
    private final String stagingQueueNameRegex;
    private static final int MAX_SCALE = 5;
    private static final Logger LOG = LoggerFactory.getLogger(RabbitWorkloadAnalyser.class);


    public RabbitWorkloadAnalyser(final String scalingTarget, final RabbitStatsReporter reporter, final RabbitWorkloadProfile profile,
                                  final RabbitSystemResourceMonitor rabbitResourceMonitor, final String stagingQueueIndicator)
    {
        this.scalingTarget = Objects.requireNonNull(scalingTarget);
        this.rabbitStats = Objects.requireNonNull(reporter);
        this.profile = Objects.requireNonNull(profile);
        this.targetQueueStatsQueue = EvictingQueue.create(profile.getScalingDelay());
        this.stagingQueuesStatsQueue = EvictingQueue.create(profile.getScalingDelay());
        this.rabbitResourceMonitor = rabbitResourceMonitor;
        this.stagingQueueNameRegex = stagingQueueIndicator != null
                ? String.format("^%s%s.+$", scalingTarget, stagingQueueIndicator)
                : null;
    }

    /**
     * This method will determine and return the percentage of the high watermark memory allowance being utilised at present by RabbitMQ
     *
     * @return The percentage being utilised
     */
    @Override
    public double getCurrentMemoryLoad() throws ScalerException
    {
        final double memoryConsumption = rabbitResourceMonitor.getCurrentMemoryComsumption();
        LOG.debug("Current memory consumption {}% of total available memory.", memoryConsumption);
        return memoryConsumption;
    }


    /**
     * {@inheritDoc}
     *
     * The logic for the rabbit workload analysis is as follows.
     * Determine the percentage of overall memory utilization at present, if the percentage is over the limit for the priority of the 
     * current application it will be scaled to zero.
     * Otherwise if there is any instances in staging, do nothing, because any statistics acquired
     * would be inaccurate. Firstly, if there are any messages in the queue and no instances,
     * scale up by 1. After that, wait until we have run a number of iterations dictated by
     * the scalingDelay in the profile, and then determine the average of the consumption
     * rates over time, and the rate provided by each worker assuming they are equal. Given
     * the backlogGoal from the profile, which is the number of seconds in which it should
     * attempt to finish the current backlog of messages, determine how many workers are
     * required and scale up or down as appropriate. The average of messages over time
     * must be zero for the number of workers to scale all the way down to zero.
     */
    @Override
    public ScalingAction analyseWorkload(final InstanceInfo instanceInfo)
            throws ScalerException
    {

        if ( instanceInfo.getInstancesStaging() == 0 ) {
            final QueueStats targetQueueStats = rabbitStats.getQueueStats(scalingTarget);
            LOG.debug("Stats for target queue {}: {}", scalingTarget, targetQueueStats);

            final List<StagingQueueStats> stagingQueuesStats = rabbitStats.getStagingQueueStats(stagingQueueNameRegex);
            final List<String> stagingQueueNames = stagingQueuesStats.stream().map(StagingQueueStats::getName).collect(toList());
            LOG.debug("Stats for staging queues {}: {}", stagingQueueNames, stagingQueuesStats);

            final int messagesInTargetQueue = targetQueueStats.getMessages();
            final int messagesInStagingQueues = stagingQueuesStats.stream().mapToInt(StagingQueueStats::getMessages).sum();
            final long messagesInTargetQueueAndStagingQueues = messagesInTargetQueue + messagesInStagingQueues;

            // if we have any messages and no instances, immediately trigger scale up
            if ( messagesInTargetQueueAndStagingQueues > 0 && instanceInfo.getTotalRunningAndStageInstances() == 0 ) {
                return ScalingAction.SCALE_UP;
            }
            targetQueueStatsQueue.add(targetQueueStats);
            stagingQueuesStatsQueue.add(stagingQueuesStats);
            counter++;
            // don't scale every time to avoid erratic behaviour, average and analyse over a period defined by the ScalingDelay
            if ( counter >= profile.getScalingDelay() ) {
                counter = 0;
                int workersNeeded = getWorkersNeeded(
                        messagesInTargetQueue, messagesInStagingQueues, messagesInTargetQueueAndStagingQueues, profile.getBacklogGoal(),
                        instanceInfo, stagingQueueNames);
                LOG.debug("Workers needed to meet backlog goal: {}", workersNeeded);
                if ( workersNeeded > instanceInfo.getTotalRunningAndStageInstances() ) {
                    int scale = Math.min(MAX_SCALE, workersNeeded - instanceInfo.getTotalRunningAndStageInstances());
                    return getScalingAction(ScalingOperation.SCALE_UP, scale);
                } else if ( workersNeeded < instanceInfo.getTotalRunningAndStageInstances() ) {
                    return getScalingAction(ScalingOperation.SCALE_DOWN, instanceInfo.getTotalRunningAndStageInstances() - workersNeeded);
                }
            }
        }
        return ScalingAction.NO_ACTION;
    }

    private int getWorkersNeeded(
            final int messagesInTargetQueue,
            final int messagesInStagingQueues,
            final long messagesInTargetQueueAndStagingQueues,
            final int backlogGoal,
            final InstanceInfo instanceInfo,
            final List<String> stagingQueueNames)
    {
        double consume = targetQueueStatsQueue.stream().mapToDouble(QueueStats::getConsumeRate).average().getAsDouble();
        double publish = targetQueueStatsQueue.stream().mapToDouble(QueueStats::getPublishRate).average().getAsDouble();
        double targetQueueAvgMsgs = targetQueueStatsQueue.stream().mapToDouble(QueueStats::getMessages).average().getAsDouble();
        double stagingQueuesAvgMsgs = stagingQueuesStatsQueue
                .stream()
                .flatMap(List::stream)
                .mapToDouble(StagingQueueStats::getMessages)
                .average()
                .orElse(0.0);
        double targetQueueAndStagingQueuesAvgMsgs = targetQueueAvgMsgs + stagingQueuesAvgMsgs;
        final int instancesRunning = instanceInfo.getInstancesRunning();

        LOG.debug("Target queue: {}. " +
                        "Staging queues: {}. " +
                        "Current number of messages in target queue: {}. " +
                        "Current number of messages in staging queues: {}. " +
                        "Current number of messages in target queue and staging queues: {}. " +
                        "Average number of messages in target queue: {}. " +
                        "Average number of messages in staging queues: {}. " +
                        "Average number of messages in target queue and staging queues: {}. " +
                        "Average consumption rate of target queue: {}. " +
                        "Average publishing rate of target queue: {}. " +
                        "Number of instances currently running: {}. " +
                        "Backlog goal: {}. ",
                scalingTarget,
                stagingQueueNames,
                messagesInTargetQueue,
                messagesInStagingQueues,
                messagesInTargetQueueAndStagingQueues,
                targetQueueAvgMsgs,
                stagingQueuesAvgMsgs,
                targetQueueAndStagingQueuesAvgMsgs,
                consume,
                publish,
                instancesRunning,
                backlogGoal);

        // if we have some consumption rate, figure out how many workers we need to meet the goal given
        if ( Double.compare(consume, 0.0) > 0 ) {
            double perWorkerEstimate = consume / instancesRunning;
            // if the average of messages over time is greater than zero, then we need at minimum one worker
            return (int) Math.max(
                    Double.compare(0.0, targetQueueAndStagingQueuesAvgMsgs) == 0 ? 0 : 1,
                    Math.round((double)messagesInTargetQueueAndStagingQueues / backlogGoal / perWorkerEstimate));
        } else if ( Double.compare(0.0, consume) == 0 &&
                Double.compare(0.0, publish) == 0 &&
                Double.compare(0.0, targetQueueAndStagingQueuesAvgMsgs) == 0 ) {
            // if we have no consumption rate, no publish rate, and no messages then we don't need any workers
            return 0;
        } else {
            // otherwise we have no consumption rate but stuff to do - since we have no idea about rate yet, just stay the same
            return Math.max(1, instanceInfo.getTotalRunningAndStageInstances());
        }
    }


    private ScalingAction getScalingAction(final ScalingOperation op, final int amount)
    {
        if ( amount > 0 ) {
            LOG.debug("Scale with operation {} by {} instances", op, amount);
            return new ScalingAction(op, amount);
        } else {
            return ScalingAction.NO_ACTION;
        }
    }

    @Override
    public String getMemoryOverloadWarning(final String percentageMem)
    {
        return "To whom it may concern, \n"
            + "The RabbitMQ instance running on system " + System.getenv("CAF_RABBITMQ_MGMT_URL") + " is experiencing issues.\n"
            + "RabbitMQ has used " + percentageMem + "% of its high watermark memory allowance.\n";
    }
}
