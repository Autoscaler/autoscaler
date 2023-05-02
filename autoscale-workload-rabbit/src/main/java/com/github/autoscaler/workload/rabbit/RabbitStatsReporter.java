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
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.TimeUnit;


/**
 * A RabbitStatsReporter is an object that actually makes HTTP calls to a RabbitMQ management server,
 * and interprets the results to return QueueStats objects which are used by the RabbitWorkloadAnalyser.
 */
public class RabbitStatsReporter
{
    public final RabbitManagementApi rabbitApi;
    private final ObjectMapper mapper = new ObjectMapper();
    private final String vhost;
    private static final String RMQ_MESSAGES_READY = "messages_ready";
    private static final String RMQ_MESSAGE_STATS = "message_stats";
    private static final String RMQ_DELIVER_DETAILS = "deliver_get_details";
    private static final String RMQ_PUBLISH_DETAILS = "publish_details";
    private static final String RMQ_RATE = "rate";
    private static final int RMQ_TIMEOUT = 10;

    // add main method
    public static void main(String[] args) throws ScalerException
    {
        final RabbitStatsReporter rsr = new RabbitStatsReporter("https://larry-cent01.swinfra.net:15672", "darwin_user", "nextgen", "/");

        final List<QueueStats> statsForAllQueuesMatching = rsr.getStatsForAllQueuesMatching("^dataprocessing-entity-extract-in».+$");

        System.out.println(statsForAllQueuesMatching);

        final PagedQueues pagedQueues1 = rsr.rabbitApi.getPagedQueues("/", "^dataprocessing-entity-extract-in(?>».*)?$", 1, 2);
        System.out.println(pagedQueues1);

        final PagedQueues pagedQueues2 = rsr.rabbitApi.getPagedQueues("/", "^dataprocessing-entity-extract-in(?>».*)?$", 2, 2);
        System.out.println(pagedQueues2);

        final PagedQueues pagedQueues3 = rsr.rabbitApi.getPagedQueues("/", "^dataprocessing-entity-extract-in(?>».*)?$", 3, 2);
        System.out.println(pagedQueues3);
    }


    public RabbitStatsReporter(final String endpoint, final String user, final String pass, final String vhost)
    {
        this.vhost = Objects.requireNonNull(vhost);
        String credentials = user + ":" + pass;
        final OkHttpClient ok = new OkHttpClient();

        final TrustManager[] trustAllCerts = new TrustManager[] {new X509TrustManager() {
            @Override
            public void checkClientTrusted(
                    java.security.cert.X509Certificate[] chain,
                    String authType) throws CertificateException
            {
            }

            @Override
            public void checkServerTrusted(
                    java.security.cert.X509Certificate[] chain,
                    String authType) throws CertificateException {
            }

            @Override
            public java.security.cert.X509Certificate[] getAcceptedIssuers() {
                return null;
            }
        } };


        final SSLContext sslContext;
        try {
            sslContext = SSLContext.getInstance("SSL");
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }
        try {
            sslContext.init(null, trustAllCerts, new java.security.SecureRandom());
        } catch (KeyManagementException e) {
            throw new RuntimeException(e);
        }
        // Create an ssl socket factory with our all-trusting manager
        final SSLSocketFactory sslSocketFactory = sslContext.getSocketFactory();

        ok.setSslSocketFactory(sslSocketFactory);
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
     * Get statistics for all RabbitMQ queues whose names match the supplied queueNameRegex regular expression.
     * @param queueNameRegex A regular expression describing the pattern of queue names to match
     * @return a list of statistics for the requested queue names
     * @throws ScalerException if the statistics cannot be acquired
     */
    public List<QueueStats> getStatsForAllQueuesMatching(final String queueNameRegex)
            throws ScalerException
    {
        final List<QueueStats> queueStatsList = new ArrayList<>();

        int currentPage = 1;
        int pageSize = 100;

        while (true) {
            // Get next page of queues
            final PagedQueues pagedQueues = rabbitApi.getPagedQueues(vhost, queueNameRegex, currentPage, pageSize);

            // Read the queue stats for each queue in this page of queues
            for (final PagedQueues.Item item : pagedQueues.getItems()) {

                double publishRate;
                double consumeRate;

                final PagedQueues.MessageStats messageStats = item.getMessageStats();

                if (messageStats != null) {
                    final PagedQueues.Rate publishDetails = messageStats.getPublishDetails();
                    publishRate = publishDetails == null ? 0.0 : publishDetails.getRate();

                    final PagedQueues.Rate deliverGetDetails = messageStats.getDeliverGetDetails();
                    consumeRate = deliverGetDetails == null ? 0.0 : deliverGetDetails.getRate();
                } else {
                    // this queue hasn't had any messages yet
                    publishRate = 0.0;
                    consumeRate = 0.0;
                }

                // Add the stats for this queue to the list
                queueStatsList.add(new QueueStats(item.getMessagesReady(), publishRate, consumeRate));
            }

            // If we've reached the last page of queues, stop
            if (pagedQueues.getPage() == pagedQueues.getPageCount()) {
                break;
            } else {
                // Else get the next page of queues
                currentPage++;
            }
        }

        return queueStatsList;
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
                                   @Query(value = "page_size", encodeValue = false) final int pageSize)
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
