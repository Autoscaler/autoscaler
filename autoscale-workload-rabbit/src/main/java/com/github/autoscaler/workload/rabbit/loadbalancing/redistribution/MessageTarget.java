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
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.ShutdownSignalException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.io.IOException;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

public class MessageTarget {
    private static final Logger LOGGER = LoggerFactory.getLogger(MessageTarget.class);
    private final long targetQueueCapacity;
    private final Channel channel;
    private final Set<String> activeQueueConsumers = ConcurrentHashMap.newKeySet();
    private final Queue targetQueue;
    private final ConcurrentHashMap<String, Queue> messageSources = new ConcurrentHashMap<>();
    private ShutdownSignalException shutdownSignalException = null;
    
    public MessageTarget(final long targetQueueCapacity, final Channel channel, final Queue targetQueue) {
        this.targetQueueCapacity = targetQueueCapacity;
        this.channel = channel;
        this.targetQueue = targetQueue;
    }
    
    public String getTargetQueueName() {
        return targetQueue.getName();
    }
    
    public synchronized void start() {
        
        while(true) {

            final long lastKnownTargetQueueLength = targetQueue.getMessages();

            final long totalKnownPendingMessages =
                    messageSources.values().stream().map(Queue::getMessages).mapToLong(Long::longValue).sum();
           
            final long consumptionTarget = targetQueueCapacity - lastKnownTargetQueueLength;
            
            final long sourceQueueConsumptionTarget;
            if(messageSources.isEmpty()) {
                sourceQueueConsumptionTarget = 0;
            }
            else {
                sourceQueueConsumptionTarget = (long) Math.ceil((double)consumptionTarget / messageSources.size());
            }
            
            LOGGER.info("TargetQueue {}, {} messages, SourceQueues {}, {} messages, " +
                            "Overall consumption target: {}, Individual Source Queue consumption target: {}", 
                    targetQueue.getName(), lastKnownTargetQueueLength,
                    (long) messageSources.size(), totalKnownPendingMessages,
                    consumptionTarget, sourceQueueConsumptionTarget);            
            
            if(consumptionTarget <= 0) {
                LOGGER.info("Target queue '{}' consumption target is <= 0, no capacity for new messages, ignoring.", targetQueue.getName());
            }
            else {
                for(final Queue messageSource: messageSources.values()) {
                    wireup(messageSource, sourceQueueConsumptionTarget);
                }
            }
            
            if(shutdownSignalException != null) {
                break;
            }
            
            try {
                //TODO This can be optimised to not wait so long if there is work to do
                this.wait(1000 * 10);
            } catch (final InterruptedException e) {
                LOGGER.warn("Exiting {}", e.getMessage());
                return;
            }
        }
    }
    
    public void updateQueueMetadata(final Queue queue) {
        this.targetQueue.setMessages(queue.getMessages());
    }
    
    public synchronized void updateMessageSources(final Set<Queue> messageSources) {

        boolean newSourcesDetected = false;
        for(final Queue queue: messageSources) {
            if (!this.messageSources.containsKey(queue.getName())) {
                newSourcesDetected = true;
            }
            this.messageSources.put(queue.getName(), queue);
        }
        
        if (newSourcesDetected) {
            LOGGER.info("New message sources have been detected.");
            this.notify();
        }
    }
    
    private void wireup(final Queue messageSource, final long consumptionLimit) {

        if(activeQueueConsumers.contains(messageSource.getName())) {
            LOGGER.info("Source queue '{}' is still active, ignoring wireup request.", messageSource.getName());
            return;
        }

        try {
            final AtomicInteger messageCount = new AtomicInteger(0);
            activeQueueConsumers.add(messageSource.getName());
            channel.basicConsume(messageSource.getName(),
                    (consumerTag, message) -> {

                        try {
                            channel.basicPublish("",
                                    targetQueue.getName(), message.getProperties(), message.getBody());
                        }
                        catch (final IOException e) {
                            //TODO Consider allowing a retry limit before escalating and stopping this MessageTarget
                            LOGGER.error("Exception publishing to '{}' {}", targetQueue.getName(),
                                    e.toString());
                        }
                        try {
                            channel.basicAck(message.getEnvelope().getDeliveryTag(), false);
                            messageCount.incrementAndGet();
                        }
                        catch (final IOException e) {
                            //TODO Consider allowing a retry limit before escalating and removing this messageSource
                            LOGGER.error("Exception ack'ing '{}' from '{}' {}",
                                    message.getEnvelope().getDeliveryTag(),
                                    messageSource,
                                    e.toString());
                        }
                        if(messageCount.get() > consumptionLimit) {
                            LOGGER.trace("Consumption target '{}' reached for '{}'.", consumptionLimit, 
                                    messageSource.getName());
                            channel.basicCancel(consumerTag);
                        }
                    },
                    consumerTag -> {
                        //Stop tracking that we are consuming from the consumerTag queue
                        activeQueueConsumers.remove(messageSource.getName());
                    },
                    (consumerTag, sig) -> {
                        //Connection lost, give up
                        shutdownSignalException = sig;
                    });
        } catch (final IOException e) {
            LOGGER.error("Exception registering consumers from '{}'", messageSource);
            activeQueueConsumers.remove(messageSource.getName());
        }

    }

    public ShutdownSignalException getShutdownSignalException() {
        return shutdownSignalException;
    }
}
