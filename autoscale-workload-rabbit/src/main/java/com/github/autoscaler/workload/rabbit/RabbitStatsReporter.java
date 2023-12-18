/*
 * Copyright 2015-2023 Open Text.
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


import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.autoscaler.api.ScalerException;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.core.Response;

import org.glassfish.jersey.client.ClientProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * A RabbitStatsReporter is an object that actually makes HTTP calls to a RabbitMQ management server,
 * and interprets the results to return QueueStats objects which are used by the RabbitWorkloadAnalyser.
 */
public class RabbitStatsReporter
{
    private final RabbitManagementApi rabbitApi;
    private final ObjectMapper mapper = new ObjectMapper();
    private final String vhost;
    private static final String RMQ_MESSAGES_READY = "messages_ready";
    private static final String RMQ_MESSAGE_STATS = "message_stats";
    private static final String RMQ_DELIVER_DETAILS = "deliver_get_details";
    private static final String RMQ_PUBLISH_DETAILS = "publish_details";
    private static final String RMQ_RATE = "rate";
    private static final int RMQ_TIMEOUT_MILLISECONDS = 10000;
    private static final int PAGE_SIZE = 100;
    private static final Logger LOG = LoggerFactory.getLogger(RabbitStatsReporter.class);

    public RabbitStatsReporter(final String endpoint, final String user, final String pass, final String vhost)
    {
        this.vhost = Objects.requireNonNull(vhost);

        final Client client = ClientBuilder.newClient();
        client.property(ClientProperties.CONNECT_TIMEOUT, RMQ_TIMEOUT_MILLISECONDS);
        client.property(ClientProperties.READ_TIMEOUT, RMQ_TIMEOUT_MILLISECONDS);
        final String credentials = user + ":" + pass;
        final String authorizationHeaderValue
            = "Basic " + Base64.getEncoder().encodeToString(credentials.getBytes(StandardCharsets.UTF_8));
        rabbitApi = new RabbitManagementApi(client, endpoint, authorizationHeaderValue);
    }


    /**
     * Get statistics for a particular RabbitMQ queue.
     * @param queueReference the named queue
     * @return statistics for the requested queue
     * @throws ScalerException if the statistics cannot be acquired
     */
    public QueueStats getQueueStats(final String queueReference)
            throws ScalerException
    {
        try {
            Response res = rabbitApi.getQueueStatus(vhost, queueReference);
            JsonNode root = mapper.readTree(res.readEntity(InputStream.class));
            int msgs = root.get(RMQ_MESSAGES_READY).asInt();
            JsonNode stats = root.get(RMQ_MESSAGE_STATS);
            double pubrate;
            double conrate;
            if ( stats != null ) {
                JsonNode ack = stats.get(RMQ_DELIVER_DETAILS);
                JsonNode pub = stats.get(RMQ_PUBLISH_DETAILS);
                pubrate = pub == null ? 0.0 : pub.get(RMQ_RATE).asDouble();
                conrate = ack == null ? 0.0 : ack.get(RMQ_RATE).asDouble();
            } else {
                // this queue hasn't had any messages yet
                pubrate = 0.0;
                conrate = 0.0;
            }
            return new QueueStats(msgs, pubrate, conrate);
        } catch (IOException e) {
            throw new ScalerException("Failed to get queue size", e);
        }
    }

    /**
     * Get statistics for all RabbitMQ staging queues whose names match the supplied stagingQueueNameRegex regular expression.
     * @param stagingQueueNameRegex A regular expression describing the pattern of staging queue names to match
     * @return a list of statistics for the requested staging queues
     * @throws ScalerException if the statistics cannot be acquired
     */
    public List<StagingQueueStats> getStagingQueueStats(final String stagingQueueNameRegex)
            throws ScalerException
    {
        if (stagingQueueNameRegex == null) {
            return Collections.emptyList();
        }

        final List<StagingQueueStats> stagingQueueStatsList = new ArrayList<>();

        int currentPage = 1;

        while (true) {

            LOG.debug("Getting page {} of queues matching regex {}", currentPage, stagingQueueNameRegex);

            // Get next page of queues
            final PagedQueues pagedQueues = rabbitApi.getPagedQueues(
                    vhost, stagingQueueNameRegex, currentPage, PAGE_SIZE, "name,messages_ready,message_stats");

            LOG.debug("Got page {} of queues matching regex {}: {}", currentPage, stagingQueueNameRegex, pagedQueues);

            // Read the queue stats for each queue in this page of queues
            for (final PagedQueues.Item item : pagedQueues.getItems()) {

                final double publishRate;
                final PagedQueues.MessageStats messageStats = item.getMessageStats();
                if (messageStats != null) {
                    final PagedQueues.Rate publishDetails = messageStats.getPublishDetails();
                    publishRate = publishDetails != null ? publishDetails.getRate() : 0.0;
                } else {
                    publishRate = 0.0;
                }

                // Add the stats for this queue to the list
                final StagingQueueStats stagingQueueStats = new StagingQueueStats(item.getName(), item.getMessagesReady(), publishRate);
                stagingQueueStatsList.add(stagingQueueStats);
            }

            // If we've reached the last page of queues, stop
            // Using >= rather than == because if there are no queues (items) in the response, page = 1 and page_count = 0
            if (pagedQueues.getPage() >= pagedQueues.getPageCount()) {
                break;
            } else {
                // Else get the next page of queues
                currentPage++;
            }
        }

        return stagingQueueStatsList;
    }

}
