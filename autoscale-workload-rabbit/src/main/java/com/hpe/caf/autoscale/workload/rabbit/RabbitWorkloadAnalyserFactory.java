/*
 * Copyright 2015-2020 Micro Focus or one of its affiliates.
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
package com.hpe.caf.autoscale.workload.rabbit;


import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.hpe.caf.api.HealthResult;
import com.hpe.caf.api.HealthStatus;
import com.hpe.caf.api.autoscale.WorkloadAnalyser;
import com.hpe.caf.api.autoscale.WorkloadAnalyserFactory;
import com.hpe.caf.autoscale.workload.rabbit.RabbitManagementApiFactory.RabbitManagementApi;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.Iterator;
import java.util.Objects;
import retrofit.client.Response;

public class RabbitWorkloadAnalyserFactory implements WorkloadAnalyserFactory
{
    private final RabbitWorkloadAnalyserConfiguration config;
    private final RabbitStatsReporter provider;
    private final RabbitManagementApi rabbitManagementApi;
    private final RabbitSystemResourceMonitor memoryMonitor;
    private final RabbitWorkloadProfile defaultProfile;
    private final ObjectMapper objectMapper;
    private final String nodeStatusEndpoint;
    private static final Logger LOG = LoggerFactory.getLogger(RabbitWorkloadAnalyserFactory.class);

    public RabbitWorkloadAnalyserFactory(final RabbitWorkloadAnalyserConfiguration config)
    {
        this.config = Objects.requireNonNull(config);
        this.provider = new RabbitStatsReporter(
            config.getRabbitManagementEndpoint(),
            config.getRabbitManagementUser(),
            config.getRabbitManagementPassword(),
            config.getVhost());
        this.rabbitManagementApi = RabbitManagementApiFactory.create(
            config.getRabbitManagementEndpoint(),
            config.getRabbitManagementUser(),
            config.getRabbitManagementPassword());
        this.memoryMonitor = new RabbitSystemResourceMonitor(rabbitManagementApi, config.getMemoryQueryRequestFrequency());
        this.defaultProfile = config.getProfiles().get(RabbitWorkloadAnalyserConfiguration.DEFAULT_PROFILE_NAME);
        this.objectMapper = new ObjectMapper();
        this.nodeStatusEndpoint = config.getRabbitManagementEndpoint() + "/api/nodes/";
    }

    @Override
    public WorkloadAnalyser getAnalyser(final String scalingTarget, final String scalingProfile)
    {
        RabbitWorkloadProfile profile;
        if ( scalingProfile == null || !config.getProfiles().containsKey(scalingProfile) ) {
            profile = defaultProfile;
        } else {
            profile = config.getProfiles().get(scalingProfile);
        }
        return new RabbitWorkloadAnalyser(scalingTarget, provider, profile, memoryMonitor);
    }

    @Override
    public HealthResult healthCheck()
    {
        try {
            if (atLeastOneNodeRunning()) {
                return HealthResult.RESULT_HEALTHY;
            } else {
                final String message
                    = "At least 1 RabbitMQ node must be running, found 0 after checking " + nodeStatusEndpoint;
                LOG.warn(message);
                return new HealthResult(HealthStatus.UNHEALTHY, message);
            }
        } catch (final IOException e) {
            final String message = "IOException when trying to query " + nodeStatusEndpoint + " during healthcheck";
            LOG.warn(message, e);
            return new HealthResult(HealthStatus.UNHEALTHY, message);
        }
    }

    private boolean atLeastOneNodeRunning() throws IOException
    {
        final Response nodeStatusResponse = rabbitManagementApi.getNodeStatus();
        final JsonNode nodeArray = objectMapper.readTree(nodeStatusResponse.getBody().in());
        final Iterator<JsonNode> iterator = nodeArray.elements();
        while (iterator.hasNext()) {
            final JsonNode jsonNode = iterator.next();
            final JsonNode runningJsonNode = jsonNode.get("running");
            if (runningJsonNode != null && runningJsonNode.asBoolean()) {
                return true;
            }
        }
        return false;
    }
}
