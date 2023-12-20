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


import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.autoscaler.api.ScalerException;
import com.github.autoscaler.api.WorkloadAnalyser;
import com.github.autoscaler.api.WorkloadAnalyserFactory;
import com.hpe.caf.api.HealthResult;
import com.hpe.caf.api.HealthStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.util.Iterator;
import java.util.Objects;

import javax.ws.rs.core.Response;

public class RabbitWorkloadAnalyserFactory implements WorkloadAnalyserFactory
{
    private final RabbitWorkloadAnalyserConfiguration config;
    private final RabbitStatsReporter provider;
    private final RabbitManagementApi rabbitManagementApi;
    private final RabbitSystemResourceMonitor rabbitResourceMonitor;
    private final RabbitWorkloadProfile defaultProfile;
    private final ObjectMapper objectMapper;
    private final String nodeStatusEndpoint;
    private final String stagingQueueIndicator;
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
        this.rabbitResourceMonitor = new RabbitSystemResourceMonitor(rabbitManagementApi, config.getResourceQueryRequestFrequency());
        this.defaultProfile = config.getProfiles().get(RabbitWorkloadAnalyserConfiguration.DEFAULT_PROFILE_NAME);
        this.objectMapper = new ObjectMapper();
        this.nodeStatusEndpoint = config.getRabbitManagementEndpoint() + "/api/nodes/";
        final String stagingQueueIndicatorFromConfig = config.getStagingQueueIndicator();
        if (stagingQueueIndicatorFromConfig != null && stagingQueueIndicatorFromConfig.isEmpty()) {
            throw new RuntimeException("Providing a staging queue indicator is optional, but if provided, it cannot be empty");
        }
        this.stagingQueueIndicator = stagingQueueIndicatorFromConfig;
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
        return new RabbitWorkloadAnalyser(scalingTarget, provider, profile, rabbitResourceMonitor, stagingQueueIndicator);
    }

    @Override
    public HealthResult healthCheck()
    {
        try {
            if (atLeastOneNodeRunning()) {
                return HealthResult.RESULT_HEALTHY;
            } else {
                final String message = "RabbitMQ management API reports 0 nodes are running: " + nodeStatusEndpoint;
                LOG.warn(message);
                return new HealthResult(HealthStatus.UNHEALTHY, message);
            }
        } catch (final IOException | ScalerException e) {
            final String message = "Failed to contact RabbitMQ management API: " + nodeStatusEndpoint;
            LOG.warn(message, e);
            return new HealthResult(HealthStatus.UNHEALTHY, message);
        }
    }

    private boolean atLeastOneNodeRunning() throws ScalerException, IOException
    {
        final Response nodeStatusResponse = rabbitManagementApi.getNodeStatus();
        final JsonNode nodeArray = objectMapper.readTree(nodeStatusResponse.readEntity(InputStream.class));
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
