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
package com.github.autoscaler.scaler.kubernetes;

import com.github.autoscaler.api.InstanceInfo;
import com.github.autoscaler.api.ScalerException;
import static com.github.autoscaler.api.ScalingConfiguration.KEY_SHUTDOWN_PRIORITY;
import com.github.autoscaler.api.ServiceScaler;
import com.github.autoscaler.kubernetes.shared.K8sAutoscaleConfiguration;
import com.hpe.caf.api.HealthResult;
import io.kubernetes.client.extended.kubectl.Kubectl;
import io.kubernetes.client.extended.kubectl.KubectlGet;
import io.kubernetes.client.extended.kubectl.exception.KubectlException;
import io.kubernetes.client.openapi.models.V1Deployment;
import io.kubernetes.client.openapi.models.V1Pod;
import io.kubernetes.client.util.generic.options.ListOptions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class K8sServiceScaler implements ServiceScaler
{
    private static final Logger LOG = LoggerFactory.getLogger(K8sServiceScaler.class);

    private final K8sAutoscaleConfiguration config;
    public K8sServiceScaler(final K8sAutoscaleConfiguration config)
    {
        this.config = config;
    }

    @Override
    public void scaleUp(final String resourceId, final int amount) throws ScalerException
    {
        final DeploymentId deploymentId = new DeploymentId(resourceId);
        try {
            final V1Deployment v1Deployment = getDeployment(deploymentId);
            final int currentReplicas = v1Deployment.getSpec().getReplicas();
            final int target = Math.min(config.getMaximumInstances(), currentReplicas + amount);
            if (target > currentReplicas) {
                LOG.info("Scaling deployment {} up by {} instances", deploymentId.id, amount);
                Kubectl.scale(V1Deployment.class)
                    .namespace(deploymentId.namespace)
                    .name(deploymentId.id)
                    .replicas(target)
                    .execute();
            }
        } catch (KubectlException e) {
            LOG.error("Error scaling up deployment {}", deploymentId.id, e);
            throw new ScalerException("Error scaling up deployment " + deploymentId.id, e);
        }
    }

    @Override
    public void scaleDown(final String resourceId, final int amount) throws ScalerException
    {
        final DeploymentId deploymentId = new DeploymentId(resourceId);
        try {
            
            final V1Deployment v1Deployment = getDeployment(deploymentId);
            final int currentReplicas = v1Deployment.getSpec().getReplicas();
            final int target = Math.max(0, currentReplicas - amount);
            if (currentReplicas > 0) {
                LOG.info("Scaling deployment {} down by {} instances", deploymentId.id, amount);
                Kubectl.scale(V1Deployment.class)
                    .namespace(deploymentId.namespace)
                    .name(deploymentId.id)
                    .replicas(target)
                    .execute();
            }
        } catch (KubectlException e) {
            LOG.error("Error scaling down deployment {}", deploymentId.id, e);
            throw new ScalerException("Error scaling down deployment " + deploymentId.id, e);
        }
    }

    @Override
    public InstanceInfo getInstanceInfo(final String resourceId) throws ScalerException
    {
        final DeploymentId deploymentId = new DeploymentId(resourceId);
        try {            
            final V1Deployment v1Deployment = getDeployment(deploymentId);
            final Map<String, String> labels = v1Deployment.getMetadata().getLabels();
            final String appName = labels.get("app");
            int running = v1Deployment.getSpec().getReplicas();
            int staging = 0;
            if (appName != null) {
                final KubectlGet<V1Pod> kubectlGet = Kubectl.get(V1Pod.class)
                    .namespace(deploymentId.namespace);
                final ListOptions listOptions = new ListOptions();
                listOptions.setLabelSelector(String.format("app=%s", appName));
                kubectlGet.options(listOptions);
                final List<V1Pod> pods = kubectlGet.execute();
                running = pods.stream()
                    .filter(p -> p.getStatus().getPhase().equalsIgnoreCase("running"))
                    .collect(Collectors.toList()).size();
                staging = pods.stream()
                    .filter(p -> p.getStatus().getPhase().equalsIgnoreCase("pending"))
                    .collect(Collectors.toList()).size();
            }
            
            int shutdownPriority = labels.containsKey(KEY_SHUTDOWN_PRIORITY) ? Integer.parseInt(labels.get(KEY_SHUTDOWN_PRIORITY)) : -1;
            final InstanceInfo instanceInfo = new InstanceInfo(
                running,
                staging,
                Collections.emptyList(),
                shutdownPriority,
                running + staging);
            
            return instanceInfo;
        } catch (final KubectlException e) {
            LOG.error("Error loading instance info for {}", deploymentId.id, e);
            throw new ScalerException("Error loading deployment scale " + deploymentId.id, e);
        }
    }

    @Override
    public HealthResult healthCheck()
    {
        return K8sHealthCheck.healthCheck(config);
    }

    private V1Deployment getDeployment(final DeploymentId deploymentId) throws KubectlException
    {
        return Kubectl.get(V1Deployment.class)
            .namespace(deploymentId.namespace)
            .name(deploymentId.id)
            .execute();
    }
    
    private class DeploymentId
    {
        final String id;
        final String namespace;
        DeploymentId(final String resourceId) throws ScalerException
        {
            final String[] data = resourceId.split(config.RESOURCE_ID_SEPARATOR);
            if (data.length != 2) {
                throw new ScalerException("Error in resource id, expected " + config.RESOURCE_ID_SEPARATOR + 
                                              " as a separator: " + resourceId);
            }
            namespace = data[0];
            id = data[1];
        }
    }
}
