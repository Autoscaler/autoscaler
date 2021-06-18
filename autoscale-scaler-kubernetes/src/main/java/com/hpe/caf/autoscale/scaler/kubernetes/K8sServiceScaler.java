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
import com.hpe.caf.api.autoscale.InstanceInfo;
import com.hpe.caf.api.autoscale.ScalerException;
import static com.hpe.caf.api.autoscale.ScalingConfiguration.KEY_SHUTDOWN_PRIORITY;
import com.hpe.caf.api.autoscale.ServiceScaler;
import com.hpe.caf.autoscale.K8sAutoscaleConfiguration;
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
    public void scaleUp(final String deploymentName, final int amount) throws ScalerException
    {
        try {
            final V1Deployment deployment = getDeployment(deploymentName);
            final int currentReplicas = deployment.getSpec().getReplicas();
            final int target = Math.min(config.getMaximumInstances(), currentReplicas + amount);
            if (target > currentReplicas) {
                LOG.info("Scaling deployment {} up by {} instances", deploymentName, amount);
                Kubectl.scale(V1Deployment.class)
                    .namespace(config.getNamespace())
                    .name(deploymentName)
                    .replicas(target)
                    .execute();
            }
        } catch (KubectlException e) {
            LOG.error("Error scaling up deployment {}", deploymentName, e);
            throw new ScalerException("Error scaling up deployment " + deploymentName, e);
        }
    }

    @Override
    public void scaleDown(final String deploymentName, final int amount) throws ScalerException
    {
        try {
            final V1Deployment deployment = getDeployment(deploymentName);
            final int currentReplicas = deployment.getSpec().getReplicas();
            final int target = Math.max(0, currentReplicas - amount);
            if (currentReplicas > 0) {
                LOG.info("Scaling deployment {} down by {} instances", deploymentName, amount);
                Kubectl.scale(V1Deployment.class)
                    .namespace(config.getNamespace())
                    .name(deploymentName)
                    .replicas(target)
                    .execute();
            }
        } catch (KubectlException e) {
            LOG.error("Error scaling down deployment {}", deploymentName, e);
            throw new ScalerException("Error scaling down deployment " + deploymentName, e);
        }
    }

    @Override
    public InstanceInfo getInstanceInfo(final String deploymentName) throws ScalerException
    {
        try {
            final V1Deployment deployment = getDeployment(deploymentName);
            final Map<String, String> labels = deployment.getMetadata().getLabels();
            String appName = labels.get("app");
            int running = deployment.getSpec().getReplicas();
            int staging = 0;
            if (appName != null) {
                final KubectlGet kubectlGet = Kubectl.get(V1Pod.class)
                    .namespace(config.getNamespace());
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
                Collections.EMPTY_LIST,
                shutdownPriority,
                running + staging);
            
            LOG.info("Got:{}", instanceInfo);
            return instanceInfo;
        } catch (KubectlException e) {
            LOG.error("Error loading instance info for {}", deploymentName, e);
            throw new ScalerException("Error loading deployment scale " + deploymentName, e);
        }
    }

    @Override
    public HealthResult healthCheck()
    {
        return HealthResult.RESULT_HEALTHY;
    }

    private V1Deployment getDeployment(final String deploymentName) throws KubectlException
    {
        return Kubectl.get(V1Deployment.class)
            .namespace(config.getNamespace())
            .name(deploymentName)
            .execute();
    }
}
