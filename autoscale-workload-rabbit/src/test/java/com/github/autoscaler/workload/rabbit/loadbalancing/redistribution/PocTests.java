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

import com.github.autoscaler.workload.rabbit.loadbalancing.redistribution.management.QueuesApi;
import com.github.autoscaler.workload.rabbit.loadbalancing.redistribution.management.RabbitManagementApi;
import com.github.autoscaler.workload.rabbit.loadbalancing.rerouting.MessageRouter;
import com.hpe.caf.worker.document.model.Application;
import com.hpe.caf.worker.document.model.Document;
import com.hpe.caf.worker.document.model.Response;
import com.hpe.caf.worker.document.model.ResponseQueue;
import com.hpe.caf.worker.document.model.Task;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import org.jetbrains.annotations.NotNull;
import org.junit.Test;
import org.mockito.Mock;

import java.io.IOException;
import java.util.concurrent.TimeoutException;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class PocTests {

    @Test
    public void runRoundRobin() throws IOException, TimeoutException {
        final ConnectionFactory connectionFactory = new ConnectionFactory();
        connectionFactory.setUsername("guest");
        connectionFactory.setPassword("guest");
        connectionFactory.setHost("david-cent01.swinfra.net");
        connectionFactory.setPort(5672);
        connectionFactory.setVirtualHost("/");
        
        final Connection connection = connectionFactory.newConnection();
        final Channel channel = connection.createChannel();

        final RabbitManagementApi<QueuesApi> queuesApi = 
                new RabbitManagementApi<>(QueuesApi.class, 
                        "http://david-cent01.swinfra.net:15672/", "guest", "guest");
        
        final RoundRobinMessageDistributor roundRobinMessageDistributor = 
                new RoundRobinMessageDistributor(queuesApi, channel, 1000);
        
        roundRobinMessageDistributor.run();
    }


    @Test
    public void processDocumentMessageRouter() throws IOException, TimeoutException {

        final ConnectionFactory connectionFactory = new ConnectionFactory();
        connectionFactory.setUsername("guest");
        connectionFactory.setPassword("guest");
        connectionFactory.setHost("david-cent01.swinfra.net");
        connectionFactory.setPort(5672);
        connectionFactory.setVirtualHost("/");

        final Connection connection = connectionFactory.newConnection();
        final Channel channel = connection.createChannel();

        final RabbitManagementApi<QueuesApi> queuesApi =
                new RabbitManagementApi<>(QueuesApi.class,
                        "http://david-cent01.swinfra.net:15672/", "guest", "guest");
        
        final MessageRouter messageRouter = new MessageRouter(queuesApi,  "/", channel, -1);

        final Document document = mock(Document.class);
        when(document.getCustomData("tenantId")).thenReturn("poc-tenant");
        when(document.getCustomData("workflowName")).thenReturn("enrichment");
        final Task task = mock(Task.class);
        when(document.getTask()).thenReturn(task);
        final Response response = mock(Response.class);
        when(task.getResponse()).thenReturn(response);
        final ResponseQueue responseQueue = new MockResponseQueue();
        responseQueue.set("dataprocessing-entity-extract-in");
        when(response.getSuccessQueue()).thenReturn(responseQueue);
        
        messageRouter.route(document);

    }
    
    private class MockResponseQueue implements ResponseQueue {
        
        private String name;

        @Override
        public void disable() {
            
        }

        @NotNull
        @Override
        public String getName() {
            return name;
        }

        @NotNull
        @Override
        public Response getResponse() {
            return null;
        }

        @Override
        public boolean isEnabled() {
            return false;
        }

        @Override
        public void reset() {

        }

        @Override
        public void set(String s) {
            name = s;
        }

        @NotNull
        @Override
        public Application getApplication() {
            return null;
        }
    }
    
}