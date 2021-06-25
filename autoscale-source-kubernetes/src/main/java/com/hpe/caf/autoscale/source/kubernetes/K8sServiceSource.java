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
import com.hpe.caf.api.HealthStatus;
import com.hpe.caf.api.autoscale.ScalerException;
import com.hpe.caf.api.autoscale.ScalingConfiguration;
import com.hpe.caf.api.autoscale.ServiceSource;
import com.hpe.caf.autoscale.K8sAutoscaleConfiguration;
import io.kubernetes.client.extended.kubectl.Kubectl;
import io.kubernetes.client.extended.kubectl.exception.KubectlException;
import io.kubernetes.client.openapi.models.V1Deployment;
import io.kubernetes.client.openapi.models.V1ObjectMeta;
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

    public K8sServiceSource(final K8sAutoscaleConfiguration config)
    {
        this.config = Objects.requireNonNull(config);
    }

    @Override
    public Set<ScalingConfiguration> getServices()
        throws ScalerException
    {
        try {
            return getScalingConfiguration();
        } catch (NumberFormatException e) {
            throw new ScalerException("Error parsing Deployment label", e);
        } catch (KubectlException e) {
            throw new ScalerException("Error loading deployments", e);
        }
    }

    private Set<ScalingConfiguration> getScalingConfiguration() throws KubectlException
    {
        final Set<ScalingConfiguration> scalingConfigurations = new HashSet<>();
        for (final String namespace: config.getNamespacesArray()) {
            scalingConfigurations.addAll(Kubectl.get(V1Deployment.class)
                .namespace(namespace)
                .execute()
                .stream()
                .filter(d -> hasMetadata(d) && isLabelledForScaling(d.getMetadata()))
                .map(d -> mapToScalingConfig(d.getMetadata(), namespace))
                .collect(Collectors.toSet()));
        }
        return scalingConfigurations;
    }
    
    private boolean hasMetadata(final V1Deployment v1Deployment) {
        return v1Deployment.getMetadata() != null && 
               v1Deployment.getMetadata().getName() != null &&
               v1Deployment.getMetadata().getLabels() != null;
    }

    private ScalingConfiguration mapToScalingConfig(
        final V1ObjectMeta metadata, 
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
    private boolean isLabelledForScaling(final V1ObjectMeta metadata)
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
            Kubectl.version().execute();
            return HealthResult.RESULT_HEALTHY;
        } catch (KubectlException e) {
            LOG.warn("Connection failure to kubernetes", e);
            return new HealthResult(HealthStatus.UNHEALTHY, "Cannot connect to Kubernetes");
        }  
    }
}
