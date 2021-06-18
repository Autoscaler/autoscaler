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
package com.hpe.caf.autoscale.scaler.kubernetes;

import com.hpe.caf.api.HealthResult;
import com.hpe.caf.api.HealthStatus;
import com.hpe.caf.api.autoscale.InstanceInfo;
import com.hpe.caf.api.autoscale.ScalerException;
import static com.hpe.caf.api.autoscale.ScalingConfiguration.KEY_SHUTDOWN_PRIORITY;
import com.hpe.caf.api.autoscale.ServiceScaler;
import com.hpe.caf.autoscale.K8sAutoscaleConfiguration;
import io.kubernetes.client.openapi.ApiException;
import io.kubernetes.client.openapi.apis.AppsV1Api;
import io.kubernetes.client.openapi.models.V1Deployment;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collections;
import java.util.Map;

public class K8sServiceScaler implements ServiceScaler
{
    private static final Logger LOG = LoggerFactory.getLogger(K8sServiceScaler.class);

    private final AppsV1Api api;
    private final K8sAutoscaleConfiguration config;
    public K8sServiceScaler(final AppsV1Api api, final K8sAutoscaleConfiguration config)
    {
        this.api = api;
        this.config = config;
    }

    @Override
    public void scaleUp(final String deploymentName, final int amount) throws ScalerException
    {
        try {
            final V1Deployment scale = getDeployment(deploymentName);
            final int currentReplicas = scale.getSpec().getReplicas();
            final int target = Math.min(config.getMaximumInstances(), currentReplicas + amount);
            if (target > currentReplicas) {
                scale.getSpec().setReplicas(Math.min(config.getMaximumInstances(), currentReplicas + amount));
                LOG.info("Scaling deployment {} up by {} instances", deploymentName, amount);
                api.replaceNamespacedDeployment(deploymentName, config.getNamespace(), scale, null, null, null);
            }
        } catch (ApiException e) {
            LOG.error("Error scaling up deployment {}", deploymentName, e);
            throw new ScalerException("Error scaling up deployment " + deploymentName, e);
        }
    }

    @Override
    public void scaleDown(final String deploymentName, final int amount) throws ScalerException
    {
        try {
            final V1Deployment scale = getDeployment(deploymentName);
            final int currentReplicas = scale.getSpec().getReplicas();
            final int target = Math.max(0, currentReplicas - amount);
            if (currentReplicas > 0) {
                scale.getSpec().setReplicas(target);
                LOG.info("Scaling deployment {} down by {} instances", deploymentName, amount);
                api.replaceNamespacedDeployment(deploymentName, config.getNamespace(), scale, null, null, null);
            }
        } catch (ApiException e) {
            LOG.error("Error scaling down deployment {}", deploymentName, e);
            throw new ScalerException("Error scaling down deployment " + deploymentName, e);
        }
    }

    @Override
    public InstanceInfo getInstanceInfo(final String deploymentName) throws ScalerException
    {
        try {
            final V1Deployment scale = getDeployment(deploymentName);
            final Map<String, String> labels = scale.getMetadata().getLabels();
            int shutdownPriority = labels.containsKey(KEY_SHUTDOWN_PRIORITY) ? Integer.parseInt(labels.get(KEY_SHUTDOWN_PRIORITY)) : -1;
            final InstanceInfo instanceInfo = new InstanceInfo(
                scale.getSpec().getReplicas(),
                0,
                Collections.EMPTY_LIST,
                shutdownPriority,
                scale.getSpec().getReplicas());
            return instanceInfo;
        } catch (ApiException e) {
            LOG.error("Error loading instance info for {}", deploymentName, e);
            throw new ScalerException("Error loading deployment scale " + deploymentName, e);
        }
    }

    @Override
    public HealthResult healthCheck()
    {
        try {
            api.listNamespacedDeployment(
                config.getNamespace(),
                "false",
                false, null, null, null, null, null, null,
                false);
            return HealthResult.RESULT_HEALTHY;
        } catch (ApiException e) {
            LOG.warn("Cannot load deployments in namespace: " + config.getNamespace(), e);
            return new HealthResult(HealthStatus.UNHEALTHY, "Cannot load deployments in namespace: " + config.getNamespace());
        }
    }

    private V1Deployment getDeployment(final String deploymentName) throws ApiException
    {
        return api.readNamespacedDeployment(deploymentName, config.getNamespace(), "false", false, false);
    }
}
