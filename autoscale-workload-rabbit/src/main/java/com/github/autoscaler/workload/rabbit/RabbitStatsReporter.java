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
import com.github.autoscaler.api.QueueNotFoundException;
import com.github.autoscaler.api.ScalerException;
import com.squareup.okhttp.OkHttpClient;
import retrofit.ErrorHandler;
import retrofit.RestAdapter;
import retrofit.RetrofitError;
import retrofit.client.OkClient;
import retrofit.client.Response;
import retrofit.http.GET;
import retrofit.http.Path;
import retrofit.http.Query;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

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
    private static final int RMQ_TIMEOUT = 10;
    private static final int PAGE_SIZE = 100;
    private static final Logger LOG = LoggerFactory.getLogger(RabbitStatsReporter.class);

    public RabbitStatsReporter(final String endpoint, final String user, final String pass, final String vhost)
    {
        this.vhost = Objects.requireNonNull(vhost);
        String credentials = user + ":" + pass;
        OkHttpClient ok = new OkHttpClient();
        ok.setReadTimeout(RMQ_TIMEOUT, TimeUnit.SECONDS);
        ok.setConnectTimeout(RMQ_TIMEOUT, TimeUnit.SECONDS);
        // build up a RestAdapter that will automatically handle authentication for us
        RestAdapter.Builder builder = new RestAdapter.Builder().setEndpoint(endpoint).setClient(new OkClient(ok));
        builder.setRequestInterceptor(requestFacade -> {
            String str = "Basic " + Base64.getEncoder().encodeToString(credentials.getBytes(StandardCharsets.UTF_8));
            requestFacade.addHeader("Accept", "application/json");
            requestFacade.addHeader("Authorization", str);
        });
        builder.setErrorHandler(new RabbitApiErrorHandler());
        RestAdapter adapter = builder.build();
        rabbitApi = adapter.create(RabbitManagementApi.class);
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
            JsonNode root = mapper.readTree(res.getBody().in());
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
                    vhost, stagingQueueNameRegex, currentPage, PAGE_SIZE, "name,messages_ready");

            LOG.debug("Got page {} of queues matching regex {}: {}", currentPage, stagingQueueNameRegex, pagedQueues);

            // Read the queue stats for each queue in this page of queues
            for (final PagedQueues.Item item : pagedQueues.getItems()) {

                // Add the stats for this queue to the list
                final StagingQueueStats stagingQueueStats = new StagingQueueStats(item.getName(), item.getMessagesReady());
                stagingQueueStatsList.add(stagingQueueStats);
            }

            // If we've reached the last page of queues, stop
            if (pagedQueues.getPage() == pagedQueues.getPageCount()) {
                break;
            } else {
                // Else get the next page of queues
                currentPage++;
            }
        }

        return stagingQueueStatsList;
    }

    public interface RabbitManagementApi
    {
        @GET("/api/queues/{vhost}/{queue}")
        Response getQueueStatus(@Path("vhost") final String vhost, @Path("queue") final String queueName)
            throws ScalerException;

        // The `use_regex` query param only works if pagination is used as well.
        // See: https://groups.google.com/g/rabbitmq-users/c/Lgad24orwog/m/E_zoUtB3BQAJ
        @GET("/api/queues/{vhost}?use_regex=true")
        PagedQueues getPagedQueues(@Path("vhost") final String vhost,
                                   @Query(value = "name", encodeValue = true) final String nameRegex,
                                   @Query(value = "page", encodeValue = false) final int page,
                                   @Query(value = "page_size", encodeValue = false) final int pageSize,
                                   @Query(value = "columns", encodeValue = true) final String columnsCsvString)
                throws ScalerException;
    }


    private static class RabbitApiErrorHandler implements ErrorHandler
    {
        @Override
        public Throwable handleError(final RetrofitError retrofitError)
        {
            if (retrofitError.getResponse().getStatus() == 404) {
                return new QueueNotFoundException(retrofitError.getUrl());
            }

            return new ScalerException("Failed to contact RabbitMQ management API using url " + retrofitError.getUrl() 
                + ". RabbitMQ could be unavailable, will retry.", retrofitError);
        }
    }
}
