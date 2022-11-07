package com.github.autoscaler.workload.rabbit.loadbalancing.redistribution;

import com.github.autoscaler.workload.rabbit.loadbalancing.redistribution.management.Queue;
import com.github.autoscaler.workload.rabbit.loadbalancing.redistribution.management.QueuesApi;
import com.github.autoscaler.workload.rabbit.loadbalancing.redistribution.management.RabbitManagementApi;
import com.github.autoscaler.workload.rabbit.loadbalancing.rerouting.MessageRouter;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.ShutdownSignalException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

public class RoundRobinMessageDistributor {

    private static final Logger LOGGER = LoggerFactory.getLogger(RoundRobinMessageDistributor.class);

    private ShutdownSignalException shutdownSignalException = null;
    private long messageLimit = 1000;
    private final RabbitManagementApi<QueuesApi> queuesApi;
    private final Channel channel;
    private ConcurrentHashMap<String, MessageTarget> messageTargets = new ConcurrentHashMap<>();

    public RoundRobinMessageDistributor(final String endpoint, final String username, final String password, 
                                        final Channel channel) {
        queuesApi = new RabbitManagementApi<>(QueuesApi.class, endpoint, username, password);
        this.channel = channel;
    }
    
    public void run() {

        if(shutdownSignalException != null) {
            throw shutdownSignalException;
        }
        
        while(true) {
            
            final Set<String> messageTargetQueueNames = getMesssageTargetsQueueNames();
            final List<Queue> queues = queuesApi.getApi().getQueues();
            
            for(final String messageTargetQueueName: messageTargetQueueNames) {
                final MessageTarget messageTarget = 
                        messageTargets.computeIfAbsent(messageTargetQueueName, k -> new MessageTarget(channel, k));
                if(messageTarget.getShutdownSignalException() != null) {
                    messageTargets.remove(messageTargetQueueName);
                    continue;
                }
                final Set<String> messageSources = getMessageSources(messageTarget, queues);
                messageTarget.updateMessageSources(messageSources);
            }
            
            try {
                Thread.sleep(1000 * 60);
            } catch (final InterruptedException e) {
                LOGGER.warn("Exiting {}", e.getMessage());
                return;
            }
        }
        
    }
    
    private Set<String> getMesssageTargetsQueueNames() {
        final List<Queue> queues = queuesApi.getApi().getQueues();

        return queues.stream()
                .filter(q ->
                        !q.getName().contains(MessageRouter.LOAD_BALANCED_INDICATOR)
                                && q.getMessages() < messageLimit
                )
                .map(Queue::getName)
                .collect(Collectors.toSet());
    }
    
    private Set<String> getMessageSources(final MessageTarget messageTarget, final List<Queue> queues) {
        
        final Collection<Queue> sourceQueues = queues.stream()
                .filter(q -> 
                        q.getName().startsWith(messageTarget.getTargetQueue() + MessageRouter.LOAD_BALANCED_INDICATOR) 
                                && q.getMessages() > 0
                )
                .collect(Collectors.toList());
        
        return sourceQueues.stream().map(Queue::getName).collect(Collectors.toSet());
    }

    // tq dataprocessing-worker-entity-extract-in
    // sq dataprocessing-worker-entity-extract-in-t1-ingestion
    // sq dataprocessing-worker-entity-extract-in-t1-enrichment
    

}
