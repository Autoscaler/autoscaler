package com.github.autoscaler.workload.rabbit.loadbalancing.redistribution;

import com.github.autoscaler.workload.rabbit.loadbalancing.redistribution.management.Queue;
import com.github.autoscaler.workload.rabbit.loadbalancing.redistribution.management.QueuesApi;
import com.github.autoscaler.workload.rabbit.loadbalancing.redistribution.management.RabbitManagementApi;
import com.github.autoscaler.workload.rabbit.loadbalancing.rerouting.MessageRouter;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.ShutdownSignalException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

public class RoundRobinMessageDistributor {

    private static final Logger LOGGER = LoggerFactory.getLogger(RoundRobinMessageDistributor.class);

    private final RabbitManagementApi<QueuesApi> queuesApi;
    private final Channel channel;
    private Set<String> activeConsumerTags = ConcurrentHashMap.newKeySet();
    private ShutdownSignalException shutdownSignalException = null;

    public RoundRobinMessageDistributor(final String endpoint, final String username, final String password, 
                                        final Channel channel) {
        queuesApi = new RabbitManagementApi<>(QueuesApi.class, endpoint, username, password);
        this.channel = channel;
    }
    
    public void run() {

        if(shutdownSignalException != null) {
            throw shutdownSignalException;
        }
        
        final List<MessageTarget> messageTargets = getMesssageTargets();
        for(final MessageTarget messageTarget: messageTargets) {
            wireup(messageTarget);
        }

//        while(true) {
//            try {
//                Thread.sleep(1000 * 60);
//            } catch (final InterruptedException e) {
//                LOGGER.warn("Exiting {}", e.getMessage());
//                return;
//            }
//        }
        
    }
    
    private List<MessageTarget> getMesssageTargets() {
        final List<MessageTarget> messageTargets = new ArrayList<>();
        final List<Queue> queues = queuesApi.getApi().getQueues();

        for(final Queue targetQueue: queues.stream()
                .filter(q -> !q.getName().contains(MessageRouter.LOAD_BALANCED_INDICATOR))
                .collect(Collectors.toList())) {

            final MessageTarget messageTarget = new MessageTarget(targetQueue.getName());
            addMessageSources(messageTarget, queues);
            messageTargets.add(messageTarget);
        }
        return messageTargets;
    }
    
    private void addMessageSources(final MessageTarget messageTarget, final List<Queue> queues) {
        
        final Collection<Queue> sourceQueues = queues.stream()
                .filter(q -> 
                        q.getName().startsWith(messageTarget.getTargetQueue() + MessageRouter.LOAD_BALANCED_INDICATOR))
                .collect(Collectors.toList());
        
        messageTarget.getMessageSources()
                .addAll(sourceQueues.stream().map(Queue::getName).collect(Collectors.toList()));
    }

    // tq dataprocessing-worker-entity-extract-in
    // sq dataprocessing-worker-entity-extract-in-t1-ingestion
    // sq dataprocessing-worker-entity-extract-in-t1-enrichment
    
    private void wireup(final MessageTarget messageTarget) {
        
        for(final String messageSource: messageTarget.getMessageSources()) {
            if(!activeConsumerTags.contains(messageSource)) {
                try {
                    channel.basicConsume(messageSource,
                            (consumerTag, message) -> {
                                try {
                                    channel.basicPublish("",
                                            messageTarget.getTargetQueue(), message.getProperties(), message.getBody());
                                }
                                catch (final IOException e) {
                                    LOGGER.error("Exception publishing to '{}' {}", messageTarget.getTargetQueue(), 
                                            e.toString());
                                }
                                try {
                                    channel.basicAck(message.getEnvelope().getDeliveryTag(), false);
                                }
                                catch (final IOException e) {
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

    }
}
