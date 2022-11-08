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
package com.github.autoscaler.workload.rabbit.loadbalancing.redistribution;

import com.github.autoscaler.workload.rabbit.loadbalancing.redistribution.management.Queue;
import com.github.autoscaler.workload.rabbit.loadbalancing.redistribution.management.QueuesApi;
import com.github.autoscaler.workload.rabbit.loadbalancing.redistribution.management.RabbitManagementApi;
import com.github.autoscaler.workload.rabbit.loadbalancing.rerouting.MessageRouter;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import com.rabbitmq.client.ShutdownSignalException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.stream.Collectors;

public class RoundRobinMessageDistributor {

    private static final Logger LOGGER = LoggerFactory.getLogger(RoundRobinMessageDistributor.class);

    private ShutdownSignalException shutdownSignalException = null;
    private final long targetQueueMessageLimit;
    private final RabbitManagementApi<QueuesApi> queuesApi;
    private final Channel channel;
    private final ConcurrentHashMap<String, MessageTarget> messageTargets = new ConcurrentHashMap<>();

    public RoundRobinMessageDistributor(final RabbitManagementApi<QueuesApi> queuesApi, 
                                        final Channel channel, final long targetQueueMessageLimit) {
        this.queuesApi = queuesApi;
        this.channel = channel;
        this.targetQueueMessageLimit = targetQueueMessageLimit;
    }
    
    public static void main(String[] args) throws IOException, TimeoutException {

        final ConnectionFactory connectionFactory = new ConnectionFactory();
        connectionFactory.setHost(args[0]);
        connectionFactory.setUsername(args[1]);
        connectionFactory.setPassword(args[2]);
        connectionFactory.setPort(Integer.parseInt(args[3]));
        connectionFactory.setVirtualHost("/");
        
        final int managementPort = Integer.parseInt(args[4]);
        final long targetQueueMessageLimit = Long.parseLong(args[5]);

        final Connection connection = connectionFactory.newConnection();
        final Channel channel = connection.createChannel();

        //TODO ManagementApi does not necessarily have same host, username and password, nor use http
        final RabbitManagementApi<QueuesApi> queuesApi =
                new RabbitManagementApi<>(QueuesApi.class,
                        "http://" + connectionFactory.getHost() + ":" + managementPort + "/", 
                        connectionFactory.getUsername(), connectionFactory.getPassword());

        final RoundRobinMessageDistributor roundRobinMessageDistributor =
                new RoundRobinMessageDistributor(queuesApi, channel, targetQueueMessageLimit);
        
        roundRobinMessageDistributor.run();
    }
    
    public void run() {
        
        final ExecutorService executorService = Executors.newWorkStealingPool();
        
        //This loop retrieves the current list of queues from RabbitMQ
        //creating MessageTargets, when needed, and registering new MessageSources when encountered
        while(true) {

            final List<Queue> queues = queuesApi.getApi().getQueues();
            
            final Set<Queue> messageTargetQueues = getMesssageTargetsQueues(queues);
            
            for(final Queue messageTargetQueue: messageTargetQueues) {
                final MessageTarget messageTarget;
                if(!messageTargets.containsKey(messageTargetQueue.getName())) {
                    messageTarget = new MessageTarget(targetQueueMessageLimit, channel, messageTargetQueue);
                    messageTarget.updateMessageSources(getMessageSourceQueues(messageTarget, queues));
                    messageTargets.put(messageTargetQueue.getName(), messageTarget);
                    executorService.submit(messageTarget::start);
                }
                else {
                    messageTarget = messageTargets.get(messageTargetQueue.getName());
                    if (messageTarget.getShutdownSignalException() != null) {
                        shutdownSignalException = messageTarget.getShutdownSignalException();
                        messageTargets.remove(messageTargetQueue.getName());
                        continue;
                    }
                    messageTarget.updateQueueMetadata(messageTargetQueue);
                    messageTarget.updateMessageSources(getMessageSourceQueues(messageTarget, queues));
                }
            }
            
            if(shutdownSignalException != null) {
                try {
                    final boolean timedOut = executorService.awaitTermination(1, TimeUnit.MINUTES);
                    if(timedOut) {
                        LOGGER.warn("Timed out while awaiting completion.");
                    }
                } catch (final InterruptedException e) {
                    throw new RuntimeException(e);
                }
                break;
            }
            
            try {
                Thread.sleep(1000 * 10);
            } catch (final InterruptedException e) {
                LOGGER.warn("Exiting {}", e.getMessage());
                return;
            }
        }
        
    }

    private Set<Queue> getMesssageTargetsQueues(final List<Queue> queues) {

        return queues.stream()
                .filter(q ->
                        !q.getName().contains(MessageRouter.LOAD_BALANCED_INDICATOR)
                )
                .collect(Collectors.toSet());
        
    }
    
    private Set<Queue> getMessageSourceQueues(final MessageTarget messageTarget, final List<Queue> queues) {

        return queues.stream()
                .filter(q -> 
                        q.getName().startsWith(messageTarget.getTargetQueueName() + MessageRouter.LOAD_BALANCED_INDICATOR)
                )
                .collect(Collectors.toSet());
    }

    // tq dataprocessing-worker-entity-extract-in
    // sq dataprocessing-worker-entity-extract-in-t1-ingestion
    // sq dataprocessing-worker-entity-extract-in-t1-enrichment
    

}
