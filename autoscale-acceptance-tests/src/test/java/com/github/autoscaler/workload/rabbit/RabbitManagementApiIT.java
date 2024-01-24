/*
 * Copyright 2015-2024 Open Text.
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
package com.github.autoscaler.workload.rabbit;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.TimeoutException;

import javax.ws.rs.core.Response;

import org.junit.Assert;
import org.junit.Test;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.autoscaler.api.ScalerException;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;

public final class RabbitManagementApiIT {
    private final RabbitManagementApi rabbitManagementApi;
    private final ObjectMapper mapper = new ObjectMapper();
    private final ConnectionFactory connectionFactory;

    public RabbitManagementApiIT() {
        final int port = Integer.parseInt(System.getProperty("rabbitmq.ctrl.port", "15672"));
        final String host = System.getProperty("docker.host.address", "localhost");
        final String endpoint = "http://" + host + ":" + port;
        final String userid = System.getProperty("rabbitmq.username", "guest");
        final String password = System.getProperty("rabbitmq.password", "guest");

        connectionFactory = new ConnectionFactory();
        connectionFactory.setHost(host);
        connectionFactory.setUsername(userid);
        connectionFactory.setPassword(password);
        connectionFactory.setPort(Integer.parseInt(System.getProperty("rabbitmq.node.port", "5672")));
        connectionFactory.setVirtualHost("/");

        rabbitManagementApi = RabbitManagementApiFactory.create(endpoint, userid, password);
    }

    @Test
    public void getNodeStatusTest() throws ScalerException, IOException {
        final Response nodeStatus = rabbitManagementApi.getNodeStatus();
        final JsonNode nodeArray = mapper.readTree(nodeStatus.readEntity(InputStream.class));

        final Iterator<JsonNode> iterator = nodeArray.elements();
        while (iterator.hasNext()) {
            final JsonNode jsonNode = iterator.next();
            final JsonNode runningJsonNode = jsonNode.get("running");
            if (runningJsonNode != null && runningJsonNode.asBoolean()) {
                Assert.assertEquals("Found running node", true, runningJsonNode.asBoolean());
                return;
            }
        }
        Assert.fail("No running nodes");
    }

    @Test
    public void getPagedQueuesTest() throws ScalerException, TimeoutException, IOException {
        final String targetQueueName = "target" + System.currentTimeMillis();
        createQueue(targetQueueName);
        final String nameRegex = targetQueueName;
        final int page = 1;
        final int pageSize = 10;
        final String columnsCsvString = "name,messages_ready,message_stats";
        final PagedQueues pQueues = rabbitManagementApi.getPagedQueues("/", nameRegex, page, pageSize, columnsCsvString);
        Assert.assertTrue("Queues not found", pQueues.getItems().length > 0);
    }

    private void createQueue(final String targetQueueName) throws TimeoutException, IOException {
        try (final Connection connection = connectionFactory.newConnection()) {

            try (final Channel channel = connection.createChannel()) {

                // Create a target queue
                final boolean targetQueueDurable = true;
                final boolean targetQueueExclusive = false;
                final boolean targetQueueAutoDelete = false;
                final Map<String, Object> targetQueueArguments = new HashMap<>();
                targetQueueArguments.put("x-queue-type", "quorum");
                channel.queueDeclare(
                    targetQueueName, targetQueueDurable, targetQueueExclusive, targetQueueAutoDelete, targetQueueArguments);
            }
        }
    }
}
