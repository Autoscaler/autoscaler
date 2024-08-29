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
package com.github.autoscaler.source.kubernetes;

import com.github.autoscaler.api.ScalerException;
import com.github.autoscaler.api.ScalingConfiguration;
import com.github.autoscaler.api.ServiceSource;
import com.github.autoscaler.kubernetes.shared.K8sAutoscaleConfiguration;
import com.github.cafapi.kubernetes.client.api.AppsV1Api;
import com.github.cafapi.kubernetes.client.api.VersionApi;
import com.github.cafapi.kubernetes.client.client.ApiClient;
import com.github.cafapi.kubernetes.client.client.ApiException;
import com.github.cafapi.kubernetes.client.model.IoK8sApiAppsV1Deployment;
import com.github.cafapi.kubernetes.client.model.IoK8sApimachineryPkgApisMetaV1ObjectMeta;
import com.hpe.caf.api.HealthResult;
import com.hpe.caf.api.HealthStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

public class K8sServiceSource implements ServiceSource
{
    private static final Logger LOG = LoggerFactory.getLogger(K8sServiceSource.class);
    
    private final K8sAutoscaleConfiguration config;

    private final AppsV1Api appsV1Api;

    private final VersionApi versionApi;

    public K8sServiceSource(final K8sAutoscaleConfiguration config, final ApiClient apiClient)
    {
        this.config = Objects.requireNonNull(config);
        this.appsV1Api = new AppsV1Api(apiClient);
        this.versionApi = new VersionApi(apiClient);
    }

    @Override
    public Set<ScalingConfiguration> getServices()
        throws ScalerException
    {
        try {
            return getScalingConfiguration();
        } catch (NumberFormatException e) {
            throw new ScalerException("Error parsing Deployment label", e);
        } catch (ApiException e) {
            throw new ScalerException("Error loading deployments", e);
        }
    }

    private Set<ScalingConfiguration> getScalingConfiguration() throws ApiException
    {
        final Set<ScalingConfiguration> scalingConfigurations = new HashSet<>();
        for (final String namespace: config.getNamespacesArray()) {
            scalingConfigurations.addAll(appsV1Api.listAppsV1NamespacedDeployment("private")
                .execute()
                .getItems()
                .stream()
                .filter(d -> hasMetadata(d) && isLabelledForScaling(d.getMetadata()))
                .map(d -> mapToScalingConfig(d.getMetadata(), namespace))
                .collect(Collectors.toSet()));
        }
        return scalingConfigurations;
    }
    
    private boolean hasMetadata(final IoK8sApiAppsV1Deployment v1Deployment) {
        return v1Deployment.getMetadata() != null && 
               v1Deployment.getMetadata().getName() != null &&
               v1Deployment.getMetadata().getLabels() != null;
    }

    private ScalingConfiguration mapToScalingConfig(
        final IoK8sApimachineryPkgApisMetaV1ObjectMeta metadata,
        final String namespace)
    {
        final Map<String, String> labels = metadata.getLabels();
        final ScalingConfiguration cfg = new ScalingConfiguration();
        cfg.setId(namespace + config.RESOURCE_ID_SEPARATOR + metadata.getName());
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
     * @param metadata
     * @return
     */
    private boolean isLabelledForScaling(final IoK8sApimachineryPkgApisMetaV1ObjectMeta metadata)
    {   
        final Map<String, String> labels = metadata.getLabels();
        final boolean isScaled = config.getGroupId().equalsIgnoreCase(labels.get(ScalingConfiguration.KEY_GROUP_ID));
        LOG.debug("Deployment {} is {}configured for scaling.", metadata.getName(), (isScaled ? "": "not "));
        return isScaled;
    }

    @Override
    public HealthResult healthCheck()
    {
        try {
            versionApi.getCodeVersion().execute();
            return HealthResult.RESULT_HEALTHY;
        } catch (final ApiException e) {
            LOG.warn("Connection failure to kubernetes", e);
            return new HealthResult(HealthStatus.UNHEALTHY, "Cannot connect to Kubernetes");
        }  
    }
}
