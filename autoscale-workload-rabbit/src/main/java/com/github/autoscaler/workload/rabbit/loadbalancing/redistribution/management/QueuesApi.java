package com.github.autoscaler.workload.rabbit.loadbalancing.redistribution.management;

import com.github.autoscaler.api.ScalerException;
import retrofit.http.GET;

import java.util.List;

public interface QueuesApi {
    @GET("/api/queues/")
    List<Queue> getQueues();
}
