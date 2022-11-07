package com.github.autoscaler.workload.rabbit.loadbalancing.redistribution;

import com.rabbitmq.client.Channel;
import com.rabbitmq.client.ShutdownSignalException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.io.IOException;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class MessageTarget {
    private static final Logger LOGGER = LoggerFactory.getLogger(MessageTarget.class);
    private final Channel channel;
    private Set<String> activeConsumerTags = ConcurrentHashMap.newKeySet();
    private String targetQueue;
    private Set<String> messageSources = ConcurrentHashMap.newKeySet();
    private ShutdownSignalException shutdownSignalException = null;
    
    public MessageTarget(final Channel channel, final String targetQueue) {
        this.channel = channel;
        this.targetQueue = targetQueue;
    }
    
    public String getTargetQueue() {
        return targetQueue;
    }

    public void updateMessageSources(final Set<String> messageSources) {
        for(final String messageSource: messageSources) {
            wireup(messageSource);
            this.messageSources.add(messageSource);
        }
    }
    
    private void wireup(final String messageSource) {
        if(!activeConsumerTags.contains(messageSource)) {
            try {
                channel.basicConsume(messageSource,
                        (consumerTag, message) -> {
                            try {
                                channel.basicPublish("",
                                        getTargetQueue(), message.getProperties(), message.getBody());
                            }
                            catch (final IOException e) {
                                //TODO Consider allowing a retry limit before escalating and stopping this MessageTarget
                                LOGGER.error("Exception publishing to '{}' {}", getTargetQueue(),
                                        e.toString());
                            }
                            try {
                                channel.basicAck(message.getEnvelope().getDeliveryTag(), false);
                            }
                            catch (final IOException e) {
                                //TODO Consider allowing a retry limit before escalating and removing this messageSource
                                LOGGER.error("Exception ack'ing '{}' from '{}' {}",
                                        message.getEnvelope().getDeliveryTag(),
                                        messageSource,
                                        e.toString());
                            }
                        },
                        consumerTag -> {
                            //Stop tracking that we are consuming from the consumerTag queue
                            activeConsumerTags.remove(consumerTag);
                        },
                        (consumerTag, sig) -> {
                            //Connection lost, give up
                            shutdownSignalException = sig;
                        });
                activeConsumerTags.add(messageSource);
            } catch (final IOException e) {
                LOGGER.error("Exception consuming from '{}'", messageSource);
            }
        }

    }

    public ShutdownSignalException getShutdownSignalException() {
        return shutdownSignalException;
    }
}
