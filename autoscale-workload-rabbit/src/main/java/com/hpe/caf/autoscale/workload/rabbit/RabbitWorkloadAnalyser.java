/*
 * Copyright 2015-2018 Micro Focus or one of its affiliates.
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
package com.hpe.caf.autoscale.workload.rabbit;


import com.google.common.collect.EvictingQueue;
import com.hpe.caf.api.autoscale.InstanceInfo;
import com.hpe.caf.api.autoscale.ScalerException;
import com.hpe.caf.api.autoscale.ScalingAction;
import com.hpe.caf.api.autoscale.ScalingOperation;
import com.hpe.caf.api.autoscale.WorkloadAnalyser;
import com.hpe.caf.autoscale.core.EmailDispatcher;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Calendar;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Objects;
import java.util.Set;


/**
 * Performs analysis over time on results returned from a RabbitStatsReporter and
 * makes scaling recommendations based upon it.
 */
public class RabbitWorkloadAnalyser implements WorkloadAnalyser
{
    private long counter = 0;
    private final double stage1ResouceLimit;
    private final double stage2ResouceLimit;
    private final double stage3ResouceLimit;
    private final RabbitWorkloadProfile profile;
    private final String scalingTarget;
    private final RabbitStatsReporter rabbitStats;
    private final RabbitSystemResourceMonitor rabbitResourceMonitor;
    private final EvictingQueue<QueueStats> statsQueue;
    private final int stage1PriorityThreashold;
    private final int stage2PriorityThreashold;
    private final int stage3PriorityThreashold;
    private final EmailDispatcher emailDispatcher = new EmailDispatcher();
    private static final int MAX_SCALE = 5;
    private static final Logger LOG = LoggerFactory.getLogger(RabbitWorkloadAnalyser.class);


    public RabbitWorkloadAnalyser(final String scalingTarget, final RabbitStatsReporter reporter, final RabbitWorkloadProfile profile,
                                  final RabbitSystemResourceMonitor rabbitResourceMonitor)
    {
        final String customStage1ResourceLimit = System.getenv("CAF_RABBITMQ_RESOURCE_LIMIT_STAGE_1");
        final String customStage2ResourceLimit = System.getenv("CAF_RABBITMQ_RESOURCE_LIMIT_STAGE_2");
        final String customStage3ResourceLimit = System.getenv("CAF_RABBITMQ_RESOURCE_LIMIT_STAGE_3");
        final String stage1PriorityThreashold = System.getenv("CAF_RABBITMQ_STAGE_1_SHUTDOWN_THRESHOLD");
        final String stage2PriorityThreashold = System.getenv("CAF_RABBITMQ_STAGE_2_SHUTDOWN_THRESHOLD");
        final String stage3PriorityThreashold = System.getenv("CAF_RABBITMQ_STAGE_3_SHUTDOWN_THRESHOLD");
        this.stage1PriorityThreashold = stage1PriorityThreashold != null ? Integer.parseInt(stage1PriorityThreashold) : -1;
        this.stage2PriorityThreashold = stage2PriorityThreashold != null ? Integer.parseInt(stage2PriorityThreashold) : -1;
        this.stage3PriorityThreashold = stage3PriorityThreashold != null ? Integer.parseInt(stage3PriorityThreashold) : -1;
        
        

        this.scalingTarget = Objects.requireNonNull(scalingTarget);
        this.rabbitStats = Objects.requireNonNull(reporter);
        this.profile = Objects.requireNonNull(profile);
        this.statsQueue = EvictingQueue.create(profile.getScalingDelay());
        this.rabbitResourceMonitor = rabbitResourceMonitor;
        this.stage1ResouceLimit =
            customStage1ResourceLimit != null ? Double.parseDouble(customStage1ResourceLimit) : 70;
        this.stage2ResouceLimit =
            customStage2ResourceLimit != null ? Double.parseDouble(customStage2ResourceLimit) : 80;
        this.stage3ResouceLimit =
            customStage3ResourceLimit != null ? Double.parseDouble(customStage3ResourceLimit) : 90;
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
        final double memoryConsumption = rabbitResourceMonitor.getCurrentMemoryComsumption();
        LOG.debug("Current memory consumption {}% of total available memory.", memoryConsumption);
        if (instanceInfo.getPriority() != null) {
            final int applicationPriority = Integer.parseInt(instanceInfo.getPriority());
            if (memoryConsumption > stage1ResouceLimit
                && stage1PriorityThreashold != -1 && applicationPriority <= stage1PriorityThreashold) {
                emailDispatcher.dispatchEmail(getEmailContent(stage1ResouceLimit));
                return getScalingAction(ScalingOperation.SCALE_DOWN_EMERGENCY, instanceInfo.getTotalInstances());
            } else if (memoryConsumption > stage2ResouceLimit
                && stage2PriorityThreashold != -1 && applicationPriority <= stage2PriorityThreashold) {
                emailDispatcher.dispatchEmail(getEmailContent(stage2ResouceLimit));
                return getScalingAction(ScalingOperation.SCALE_DOWN_EMERGENCY, instanceInfo.getTotalInstances());
            } else if (memoryConsumption > stage3ResouceLimit) {
                emailDispatcher.dispatchEmail(getEmailContent(stage3ResouceLimit));
                if (stage3PriorityThreashold != -1 && applicationPriority <= stage3PriorityThreashold) {
                    return getScalingAction(ScalingOperation.SCALE_DOWN_EMERGENCY, instanceInfo.getTotalInstances());
                }
            }
        }

        if ( instanceInfo.getInstancesStaging() == 0 ) {
            QueueStats stats = rabbitStats.getQueueStats(scalingTarget);
            LOG.debug("Stats for target {}: {}", scalingTarget, stats);
            // if we have any messages and no instances, immediately trigger scale up
            if ( stats.getMessages() > 0 && instanceInfo.getTotalInstances() == 0 ) {
                return ScalingAction.SCALE_UP;
            }
            statsQueue.add(stats);
            counter++;
            // don't scale every time to avoid erratic behaviour, average and analyse over a period defined by the ScalingDelay
            if ( counter >= profile.getScalingDelay() ) {
                counter = 0;
                int workersNeeded = getWorkersNeeded(stats.getMessages(), profile.getBacklogGoal(), instanceInfo);
                LOG.debug("Workers needed to meet backlog goal: {}", workersNeeded);
                if ( workersNeeded > instanceInfo.getTotalInstances() ) {
                    int scale = Math.min(MAX_SCALE, workersNeeded - instanceInfo.getTotalInstances());
                    return getScalingAction(ScalingOperation.SCALE_UP, scale);
                } else if ( workersNeeded < instanceInfo.getTotalInstances() ) {
                    return getScalingAction(ScalingOperation.SCALE_DOWN, instanceInfo.getTotalInstances() - workersNeeded);
                }
            }
        }
        return ScalingAction.NO_ACTION;
    }


    private int getWorkersNeeded(final long messages, final int backlogGoal, final InstanceInfo instanceInfo)
    {
        double consume = statsQueue.stream().mapToDouble(QueueStats::getConsumeRate).average().getAsDouble();
        double publish = statsQueue.stream().mapToDouble(QueueStats::getPublishRate).average().getAsDouble();
        double avgMsgs = statsQueue.stream().mapToDouble(QueueStats::getMessages).average().getAsDouble();
        // if we have some consumption rate, figure out how many workers we need to meet the goal given
        if ( Double.compare(consume, 0.0) > 0 ) {
            double perWorkerEstimate = consume / instanceInfo.getInstancesRunning();
            // if the average of messages over time is greater than zero, then we need at minimum one worker
            return (int) Math.max( Double.compare(0.0, avgMsgs) == 0 ? 0 : 1, Math.round((double)messages / backlogGoal / perWorkerEstimate));
        } else if ( Double.compare(0.0, consume) == 0 && Double.compare(0.0, publish) == 0 && Double.compare(0.0, avgMsgs) == 0 ) {
            // if we have no consumption rate, no publish rate, and no messages then we don't need any workers
            return 0;
        } else {
            // otherwise we have no consumption rate but stuff to do - since we have no idea about rate yet, just stay the same
            return Math.max(1, instanceInfo.getTotalInstances());
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

    private String getEmailContent(final double percentage)
    {
                final DateFormat dateFormat = new SimpleDateFormat("HH:mm:ss dd/MM/yyyy");
        final Calendar cal = Calendar.getInstance();
        final String emailContenet
            = "To whom it may concern, \n"
            + "The RabbitMQ instance running on system " + System.getenv("CAF_RABBITMQ_MGMT_URL") + " is experiencing issues.\n"
            + "\n"
            + "RabbitMQ has used " + percentage + "% of its high watermark memory allowance.\n" 
            + "\n"
            + "From: Autoscaler\n"
            + "Date: " + dateFormat.format(cal);
        return emailContenet;
    }
}
