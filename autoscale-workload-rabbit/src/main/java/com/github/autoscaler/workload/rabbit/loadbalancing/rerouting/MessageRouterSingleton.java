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
package com.github.autoscaler.workload.rabbit.loadbalancing.rerouting;

import com.github.autoscaler.workload.rabbit.loadbalancing.redistribution.management.QueuesApi;
import com.github.autoscaler.workload.rabbit.loadbalancing.redistribution.management.RabbitManagementApi;
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

            final RabbitManagementApi<QueuesApi> queuesApi =
                    new RabbitManagementApi<>(QueuesApi.class, mgmtEndpoint, mgmtUsername, mgmtPassword);

            messageRouter = new MessageRouter(queuesApi, "/", connection.createChannel(), 1000);
        } catch (final IOException | TimeoutException e) {
            LOGGER.error("Failed to initialise - {}", e.toString());
            throw new RuntimeException(e);
        }
    }
    
    public static void route(final Document document) {
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
