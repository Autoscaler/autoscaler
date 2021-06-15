/*
 * Copyright 2015-2021 Micro Focus or one of its affiliates.
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
package com.hpe.caf.autoscale.source.kubernetes;

import com.hpe.caf.api.HealthResult;
import com.hpe.caf.api.autoscale.ScalerException;
import com.hpe.caf.api.autoscale.ScalingConfiguration;
import com.hpe.caf.api.autoscale.ServiceSource;
import com.hpe.caf.autoscale.K8sAutoscaleConfiguration;
import io.kubernetes.client.openapi.ApiException;
import io.kubernetes.client.openapi.apis.AppsV1Api;
import io.kubernetes.client.openapi.models.V1ObjectMeta;
import io.kubernetes.client.openapi.models.V1ReplicaSet;
import io.kubernetes.client.openapi.models.V1ReplicaSetList;

import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

// DDD complete header
public class K8sServiceSource implements ServiceSource
{
    private final K8sAutoscaleConfiguration config;
    private final AppsV1Api api;
    
    private final String RABBITMQ_METRIC = "rabbitmq";

    public K8sServiceSource(final AppsV1Api api, final K8sAutoscaleConfiguration config)
    {
        this.config = Objects.requireNonNull(config);
        this.api = api;
    }
        
    /**
     * Needs to talk to K8S and load replica sets with autoscaling labels
     */
    @Override
    public Set<ScalingConfiguration> getServices()
        throws ScalerException
    {
        try {
            return getScalingConfiguration();
        } catch (NumberFormatException e) {
            throw new ScalerException("Error parsing ReplicaSet label", e);
        } catch (ApiException e) {
            throw new ScalerException("Error loading services", e);
        }
    }
    
    private Set<ScalingConfiguration> getScalingConfiguration() throws ApiException
    {
        final V1ReplicaSetList replicaSets = api.listNamespacedReplicaSet(
            config.getNamespace(), 
            "false", 
            false, null, null, null, null, null, null, 
            false);
        return replicaSets.getItems()
            .stream()
            .filter(rs -> isLabelledForScaling(rs))
            .map(rs -> mapToScalingConfig(rs))
            .collect(Collectors.toSet());
    }

    private ScalingConfiguration mapToScalingConfig(final V1ReplicaSet rs)
    {
        final ScalingConfiguration cfg = new ScalingConfiguration();
        final V1ObjectMeta metadata = rs.getMetadata();
        final Map<String, String> labels = metadata.getLabels();
        cfg.setId(metadata.getName());
        if (labels.containsKey(ScalingConfiguration.KEY_WORKLOAD_METRIC)) {
            cfg.setWorkloadMetric(labels.get(ScalingConfiguration.KEY_WORKLOAD_METRIC));
        }
        if (labels.containsKey(ScalingConfiguration.KEY_BACKOFF_AMOUNT)) {
            cfg.setBackoffAmount(Integer.parseInt(labels.get(ScalingConfiguration.KEY_BACKOFF_AMOUNT)));
        }
        if (labels.containsKey(ScalingConfiguration.KEY_INTERVAL)) {
            cfg.setInterval(Integer.parseInt(labels.get(ScalingConfiguration.KEY_INTERVAL)));
        }
        if (labels.containsKey(ScalingConfiguration.KEY_MAX_INSTANCES)) {
            cfg.setMaxInstances(Integer.parseInt(labels.get(ScalingConfiguration.KEY_MAX_INSTANCES)));
        }
        if (labels.containsKey(ScalingConfiguration.KEY_MIN_INSTANCES)) {
            cfg.setMinInstances(Integer.parseInt(labels.get(ScalingConfiguration.KEY_MIN_INSTANCES)));
        }
        if (labels.containsKey(ScalingConfiguration.KEY_SCALE_DOWN_BACKOFF_AMOUNT)) {
            cfg.setScaleDownBackoffAmount(Integer.parseInt(labels.get(ScalingConfiguration.KEY_SCALE_DOWN_BACKOFF_AMOUNT)));
        }
        if (labels.containsKey(ScalingConfiguration.KEY_SCALE_UP_BACKOFF_AMOUNT)) {
            cfg.setScaleUpBackoffAmount(Integer.parseInt(labels.get(ScalingConfiguration.KEY_SCALE_UP_BACKOFF_AMOUNT)));
        }
        if (labels.containsKey(ScalingConfiguration.KEY_SCALING_PROFILE)) {
            cfg.setScalingProfile(labels.get(ScalingConfiguration.KEY_SCALING_PROFILE));
        }
        if (labels.containsKey(ScalingConfiguration.KEY_SCALING_TARGET)) {
            cfg.setScalingTarget(labels.get(ScalingConfiguration.KEY_SCALING_TARGET));
        }
        return cfg;
    }

    /**
     * // DDD Still to work out ho we determine this
     * @param rs
     * @return
     */
    private boolean isLabelledForScaling(final V1ReplicaSet rs)
    {
        return RABBITMQ_METRIC.equalsIgnoreCase(rs.getMetadata().getLabels().get(ScalingConfiguration.KEY_WORKLOAD_METRIC));
    }

    /**
     * // DDD Add kubectl healthcheck 
     */
    @Override
    public HealthResult healthCheck()
    {
        return HealthResult.RESULT_HEALTHY;
    }
}
