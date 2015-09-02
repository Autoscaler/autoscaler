package com.hpe.caf.autoscale.workload.rabbit;


import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.hpe.caf.api.autoscale.ScalerException;
import com.squareup.okhttp.OkHttpClient;
import retrofit.RestAdapter;
import retrofit.client.OkClient;
import retrofit.client.Response;
import retrofit.http.GET;
import retrofit.http.Path;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Objects;


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


    public RabbitStatsReporter(final String endpoint, final String user, final String pass, final String vhost)
    {
        this.vhost = Objects.requireNonNull(vhost);
        String credentials = user + ":" + pass;
        // build up a RestAdapter that will automatically handle authentication for us
        RestAdapter.Builder builder = new RestAdapter.Builder().setEndpoint(endpoint).setClient(new OkClient(new OkHttpClient()));
        builder.setRequestInterceptor(requestFacade -> {
            String str = "Basic " + Base64.getEncoder().encodeToString(credentials.getBytes(StandardCharsets.UTF_8));
            requestFacade.addHeader("Accept", "application/json");
            requestFacade.addHeader("Authorization", str);
        });
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


    public interface RabbitManagementApi
    {
        @GET("/api/queues/{vhost}/{queue}")
        Response getQueueStatus(@Path("vhost") final String vhost, @Path("queue") final String queueName);
    }
}
