package com.github.autoscaler.workload.rabbit.loadbalancing;

import com.github.autoscaler.workload.rabbit.RabbitStatsReporter;
import com.hpe.caf.worker.document.model.Document;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.concurrent.TimeoutException;

public class MessageRouterSingleton {

    private static final Logger LOGGER = LoggerFactory.getLogger(MessageRouterSingleton.class);

    private static final Connection connection;    
    private static final MessageRouter messageRouter;
    
    static {
        
        final ConnectionFactory connectionFactory = new ConnectionFactory();
        connectionFactory.setUsername(System.getenv("CAF_RABBITMQ_USERNAME"));
        connectionFactory.setPassword(System.getenv("CAF_RABBITMQ_PASSWORD"));
        connectionFactory.setHost(System.getenv("CAF_RABBITMQ_HOST"));
        connectionFactory.setPort(Integer.parseInt(System.getenv("CAF_RABBITMQ_PORT")));
        connectionFactory.setVirtualHost("/");

        final String mgmtEndpoint = System.getenv("CAF_RABBITMQ_MGMT_URL");
        final String mgmtUsername = System.getenv("CAF_RABBITMQ_MGMT_USERNAME");
        final String mgmtPassword = System.getenv("CAF_RABBITMQ_MGMT_PASSWORD");
        
        try {
            connection = connectionFactory.newConnection();
            messageRouter = new MessageRouter(
                    new RabbitStatsReporter(mgmtEndpoint, mgmtUsername, mgmtPassword, 
                            "/"), 
                    connection.createChannel());
        } catch (final IOException | TimeoutException e) {
            LOGGER.error("Failed to initialise - {}", e);
            throw new RuntimeException(e);
        }
    }
    
    public static void routeMessage(final Document document) {
        if(messageRouter != null) {
            messageRouter.route(document);
        }
    }
    
    public static void close() throws IOException {
        if(connection != null) {
            connection.close();
        }
    }

}
