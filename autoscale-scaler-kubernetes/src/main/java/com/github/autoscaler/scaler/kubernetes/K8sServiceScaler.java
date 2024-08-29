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

import static com.github.autoscaler.api.ScalingConfiguration.KEY_SHUTDOWN_PRIORITY;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.github.autoscaler.api.InstanceInfo;
import com.github.autoscaler.api.ScalerException;
import com.github.autoscaler.api.ServiceScaler;
import com.github.autoscaler.kubernetes.shared.K8sAutoscaleConfiguration;
import com.github.cafapi.kubernetes.client.api.AppsV1Api;
import com.github.cafapi.kubernetes.client.api.CoreV1Api;
import com.github.cafapi.kubernetes.client.client.ApiClient;
import com.github.cafapi.kubernetes.client.client.ApiException;
import com.github.cafapi.kubernetes.client.model.IoK8sApiAppsV1Deployment;
import com.github.cafapi.kubernetes.client.model.IoK8sApiAppsV1DeploymentSpec;
import com.github.cafapi.kubernetes.client.model.IoK8sApiCoreV1Pod;
import com.github.cafapi.kubernetes.client.model.IoK8sApiCoreV1PodStatus;
import com.github.cafapi.kubernetes.client.model.IoK8sApiCoreV1PodTemplateSpec;
import com.github.cafapi.kubernetes.client.model.IoK8sApimachineryPkgApisMetaV1LabelSelector;
import com.github.cafapi.kubernetes.client.model.IoK8sApimachineryPkgApisMetaV1ObjectMeta;
import com.hpe.caf.api.HealthResult;

public class K8sServiceScaler implements ServiceScaler
{
    private static final Logger LOG = LoggerFactory.getLogger(K8sServiceScaler.class);

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    private final ApiClient apiClient;

    private final AppsV1Api appsV1Api;

    private final CoreV1Api coreV1Api;

    private final K8sAutoscaleConfiguration config;
    public K8sServiceScaler(final K8sAutoscaleConfiguration config, final ApiClient apiClient)
    {
        this.config = config;
        this.apiClient = apiClient;
        this.appsV1Api = new AppsV1Api(apiClient);
        this.coreV1Api = new CoreV1Api(apiClient);
    }

    @Override
    public void scaleUp(final String resourceId, final int amount) throws ScalerException
    {
        final DeploymentId deploymentId = new DeploymentId(resourceId);
        try {
            final IoK8sApiAppsV1Deployment v1Deployment = getDeployment(deploymentId);
            final int currentReplicas = getNumberOfReplicas(v1Deployment);
            final int target = Math.min(config.getMaximumInstances(), currentReplicas + amount);
            if (target > currentReplicas) {
                LOG.info("Scaling deployment {} up by {} instances", deploymentId.id, amount);
                executeScaleRequest(deploymentId.namespace, deploymentId.id, target);
            }
        } catch (final ApiException e) {
            LOG.error("Error scaling up deployment {}", deploymentId.id, e);
            throw new ScalerException("Error scaling up deployment " + deploymentId.id, e);
        }
    }

    @Override
    public void scaleDown(final String resourceId, final int amount) throws ScalerException
    {
        final DeploymentId deploymentId = new DeploymentId(resourceId);
        try {
            
            final IoK8sApiAppsV1Deployment v1Deployment = getDeployment(deploymentId);
            final int currentReplicas = getNumberOfReplicas(v1Deployment);
            final int target = Math.max(0, currentReplicas - amount);
            if (currentReplicas > 0) {
                LOG.info("Scaling deployment {} down by {} instances", deploymentId.id, amount);
                executeScaleRequest(deploymentId.namespace, deploymentId.id, target);
            }
        } catch (final ApiException e) {
            LOG.error("Error scaling down deployment {}", deploymentId.id, e);
            throw new ScalerException("Error scaling down deployment " + deploymentId.id, e);
        }
    }

    @Override
    public InstanceInfo getInstanceInfo(final String resourceId) throws ScalerException
    {
        final DeploymentId deploymentId = new DeploymentId(resourceId);
        try {            
            final IoK8sApiAppsV1Deployment v1Deployment = getDeployment(deploymentId);
            final String appName = getAppName(v1Deployment);
            int running = getNumberOfReplicas(v1Deployment);
            int staging = 0;
            if (appName != null) {
                final CoreV1Api.APIlistCoreV1NamespacedPodRequest request = coreV1Api.listCoreV1NamespacedPod("private");
                request.labelSelector(String.format("app=%s", appName));
                final List<IoK8sApiCoreV1Pod> pods = request.execute().getItems();
                running = pods.stream()
                        .filter(p -> isPodInPhase(p, "running"))
                        .toList()
                        .size();
                staging = pods.stream()
                        .filter(p -> isPodInPhase(p, "pending"))
                        .toList()
                        .size();

                LOG.debug("The deployment named {} in namespace {} has {} pod(s) running and {} pod(s) pending/staging",
                        deploymentId.id, deploymentId.namespace, running, staging);
            } else {
                LOG.warn("The deployment named {} in namespace {} does not have a label named 'app', so unable to query pods to find the " +
                                "number in running and pending/staging phases. Falling back to default values. " +
                                "Running (defaulting to spec.replicas) : {}. Pending/staging (defaulting to 0): {}",
                        deploymentId.id, deploymentId.namespace, running, staging);
            }

            final IoK8sApimachineryPkgApisMetaV1ObjectMeta metadata = v1Deployment.getMetadata();
            final Map<String, String> labels =  metadata != null
                    ? Optional.ofNullable(metadata.getLabels()).orElse(Collections.emptyMap())
                    : Collections.emptyMap();
            int shutdownPriority = labels.containsKey(KEY_SHUTDOWN_PRIORITY) ? Integer.parseInt(labels.get(KEY_SHUTDOWN_PRIORITY)) : -1;
            final InstanceInfo instanceInfo = new InstanceInfo(
                running,
                staging,
                Collections.emptyList(),
                shutdownPriority,
                running + staging);
            
            return instanceInfo;
        } catch (final ApiException e) {
            LOG.error("Error loading instance info for {}", deploymentId.id, e);
            throw new ScalerException("Error loading deployment scale " + deploymentId.id, e);
        }
    }

    private static String getAppName(final IoK8sApiAppsV1Deployment v1Deployment) {
        if (v1Deployment == null) {
            return null;
        }

        final IoK8sApiAppsV1DeploymentSpec spec = v1Deployment.getSpec();
        if (spec == null) {
            return null;
        }

        // Try to get label named 'app' from spec.selector.matchLabels first
        final IoK8sApimachineryPkgApisMetaV1LabelSelector selector = spec.getSelector();
        final Map<String,String> matchLabels = selector.getMatchLabels();
        if (matchLabels != null) {
            final String appLabel = matchLabels.get("app");
            if (appLabel != null) {
                return appLabel;
            }
        }

        // If label named 'app' is not found in spec.selector.matchLabels, try spec.template.metadata.labels
        final IoK8sApiCoreV1PodTemplateSpec template = spec.getTemplate();
        final IoK8sApimachineryPkgApisMetaV1ObjectMeta metadata = template.getMetadata();
        if (metadata != null) {
            final Map<String, String> labels = metadata.getLabels();
            if (labels != null) {
                return labels.get("app");
            }
        }

        // No label named 'app' found
        return null;
    }

    private static int getNumberOfReplicas(final IoK8sApiAppsV1Deployment v1Deployment)
    {
        final IoK8sApiAppsV1DeploymentSpec spec = v1Deployment.getSpec();
        if (spec == null) {
            return 0;
        }

        final Integer replicas = spec.getReplicas();
        if (replicas == null) {
            return 0;
        }

        return replicas;
    }

    private static boolean isPodInPhase(final IoK8sApiCoreV1Pod pod, final String phase)
    {
        if (pod == null) {
            return false;
        }

        final IoK8sApiCoreV1PodStatus status = pod.getStatus();
        if (status == null) {
            return false;
        }

        final String currentPhase = status.getPhase();
        if (currentPhase == null) {
            return false;
        }

        return currentPhase.equalsIgnoreCase(phase);
    }

    @Override
    public HealthResult healthCheck()
    {
        return K8sHealthCheck.healthCheck(config, apiClient);
    }

    private IoK8sApiAppsV1Deployment getDeployment(final DeploymentId deploymentId) throws ApiException
    {
        return appsV1Api.readAppsV1NamespacedDeployment(deploymentId.id, deploymentId.namespace).execute();
    }

    private void executeScaleRequest(final String namespace, final String name, final int target) throws ApiException
    {
        final ObjectNode objectNode = OBJECT_MAPPER.createObjectNode();
        objectNode.put("op", "replace");
        objectNode.put("path", "/spec/replicas");
        objectNode.put("value", target);

        final ArrayNode arrayNode = OBJECT_MAPPER.createArrayNode();
        arrayNode.add(objectNode);

        final AppsV1Api.APIpatchAppsV1NamespacedDeploymentRequest request = appsV1Api.patchAppsV1NamespacedDeployment(name, namespace);
        request.body(arrayNode);

        request.execute();
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
