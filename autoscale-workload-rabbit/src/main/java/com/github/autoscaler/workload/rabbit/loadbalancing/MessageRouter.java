/*
 * Copyright 2015-2022 Micro Focus or one of its affiliates.
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
package com.github.autoscaler.workload.rabbit.loadbalancing;

import com.github.autoscaler.workload.rabbit.QueueStats;
import com.github.autoscaler.workload.rabbit.RabbitStatsReporter;
import com.github.autoscaler.workload.rabbit.loadbalancing.mutators.QueueNameMutator;
import com.github.autoscaler.workload.rabbit.loadbalancing.mutators.TenantQueueNameMutator;
import com.github.autoscaler.workload.rabbit.loadbalancing.mutators.WorkflowQueueNameMutator;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.hpe.caf.worker.document.model.Document;
import com.hpe.caf.worker.document.model.Response;
import com.hpe.caf.worker.document.model.ResponseQueue;
import com.rabbitmq.client.Channel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nonnull;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

public class MessageRouter {
    
    private static final Logger LOGGER = LoggerFactory.getLogger(MessageRouter.class);

    private static final Map<String, Object>arguments = Map.of("queue-mode", "lazy");
    
    private final List<QueueNameMutator> queueNameMutators = List.of(
            new TenantQueueNameMutator(), new WorkflowQueueNameMutator());
    
    private final LoadingCache<String, QueueStats> queueStatsCache;
    private final Channel channel;

    public MessageRouter(final RabbitStatsReporter rabbitStatsReporter, final Channel channel) {

        this.queueStatsCache = CacheBuilder.newBuilder()
                .expireAfterWrite(1, TimeUnit.MINUTES)
                .build(new CacheLoader<String, QueueStats>() {
                    @Override
                    public QueueStats load(@Nonnull final String queueName) throws Exception {
                        return rabbitStatsReporter.getQueueStats(queueName);
                    }
                });

        this.channel = channel;
    }
    
    public void route(final Document document) {
        final Response response = document.getTask().getResponse();
        final String originalQueueName = response.getSuccessQueue().getName();
        
        if(shouldReroute(response.getSuccessQueue())) {
            for(final QueueNameMutator queueNameMutator: queueNameMutators) {
                queueNameMutator.mutateSuccessQueueName(document);
            }
        }

        try {
            ensureQueueExists(originalQueueName, response.getSuccessQueue().getName());
        } catch (final IOException e) {
            LOGGER.error("Unable to verify the new target queue '{}' exists, reverting to original queue.", 
                    response.getSuccessQueue().getName());

            response.getSuccessQueue().set(originalQueueName);
        }
    }
    
    private boolean shouldReroute(final ResponseQueue successQueue) {
        final QueueStats queueStats;
        try {
            queueStats = queueStatsCache.get(successQueue.getName());
        } catch (final ExecutionException e) {
            //e.getCause could be the ScalerException thrown by getQueueStats
            LOGGER.error("Could not retrieve queue stats for {} to determine need for reroute.\n{}", 
                    successQueue.getName(), e.getCause().toString());
            
            return false;
        }

        return queueStats.getMessages() > 1000 || (queueStats.getConsumeRate() < queueStats.getConsumeRate());
    }
    
    private void ensureQueueExists(final String originalQueueName, final String reroutedQueueName) 
            throws IOException {
        
        if(reroutedQueueName.equals(originalQueueName)) {
            return;
        }
        
        //Durable lazy queue
        channel.queueDeclare(reroutedQueueName, true, false, false, arguments);
    }
}
