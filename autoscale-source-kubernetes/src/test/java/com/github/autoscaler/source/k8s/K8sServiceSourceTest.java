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
package com.github.autoscaler.source.k8s;

import com.github.autoscaler.api.ScalerException;
import com.github.autoscaler.api.ScalingConfiguration;
import com.github.autoscaler.kubernetes.shared.K8sAutoscaleConfiguration;
import com.github.autoscaler.source.kubernetes.K8sServiceSource;
import com.github.cafapi.kubernetes.client.api.AppsV1Api;
import com.github.cafapi.kubernetes.client.client.ApiClient;
import com.github.cafapi.kubernetes.client.client.ApiException;
import com.github.cafapi.kubernetes.client.model.IoK8sApiAppsV1Deployment;
import com.github.cafapi.kubernetes.client.model.IoK8sApiAppsV1DeploymentList;
import com.github.cafapi.kubernetes.client.model.IoK8sApiAppsV1DeploymentSpec;
import com.github.cafapi.kubernetes.client.model.IoK8sApimachineryPkgApisMetaV1LabelSelector;
import com.github.cafapi.kubernetes.client.model.IoK8sApimachineryPkgApisMetaV1ObjectMeta;
import com.google.common.collect.ImmutableMap;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.MockedConstruction;
import org.mockito.Mockito;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;

import static java.util.stream.Collectors.toList;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class K8sServiceSourceTest {

    private static final String WORKER_CLASSIFICATION_NAME = "worker-classification";
    private static final String WORKER_ENTITYEXTRACT_NAME = "worker-entityextract";
    private static final String NAMESPACE = "private";
    private static final String GROUPID = "managed-queue-workers";
    private static final String METRIC = "rabbitmq";
    private static final String WORKER_CLASSIFICATION_TARGET = "worker-classification-in";
    private static final String WORKER_ENTITYEXTRACT_TARGET = "worker-entityextract-in";
    private static final String PROFILE = "default";
    private static final String INTERVAL = "30";
    private static final String MAX = "4";
    private static final String MIN = "1";
    private static final String BACKOFF = "10";
    private static final String SHUTDOWN_PRIORITY = "10";
    private static final int REPLICAS = 3;

    @Test
    public void getServicesTest() throws ScalerException, ApiException {
        final IoK8sApiAppsV1Deployment classificationWorkerDeployment = createDeploymentWithLabels(
                WORKER_CLASSIFICATION_NAME,
                NAMESPACE,
                MIN,
                MAX,
                WORKER_CLASSIFICATION_TARGET,
                GROUPID,
                INTERVAL,
                BACKOFF,
                PROFILE,
                WORKER_CLASSIFICATION_TARGET,
                METRIC,
                SHUTDOWN_PRIORITY,
                REPLICAS,
                WORKER_CLASSIFICATION_NAME);

        final List<IoK8sApiAppsV1Deployment> deployments = new ArrayList<>();
        deployments.add(classificationWorkerDeployment);
        final IoK8sApiAppsV1DeploymentList deploymentList = new IoK8sApiAppsV1DeploymentList();
        deploymentList.setItems(deployments);

        final AppsV1Api.APIlistAppsV1NamespacedDeploymentRequest apilistAppsV1NamespacedDeploymentRequestMock =
                mock(AppsV1Api.APIlistAppsV1NamespacedDeploymentRequest.class);

        when(apilistAppsV1NamespacedDeploymentRequestMock.execute()).thenReturn(deploymentList);

        final K8sAutoscaleConfiguration config = Mockito.mock(K8sAutoscaleConfiguration.class);
        final String namespaces = "private";
        final List<String> namespacesArray = Stream.of(namespaces.split(","))
                .map(String::trim)
                .collect(toList());
        when(config.getNamespacesArray()).thenReturn(namespacesArray);
        when(config.getMaximumInstances()).thenReturn(4);
        when(config.getGroupId()).thenReturn("managed-queue-workers");

        try (MockedConstruction<AppsV1Api> appsV1ApiMock = Mockito.mockConstruction(AppsV1Api.class, (mock, context) -> {
            when(mock.listAppsV1NamespacedDeployment("private")).thenReturn(apilistAppsV1NamespacedDeploymentRequestMock);
        })) {
            final ApiClient apiClientMock = mock(ApiClient.class);

            final K8sServiceSource source = new K8sServiceSource(config, apiClientMock);
            final Set<ScalingConfiguration> services = source.getServices();

            assertEquals(1, services.size());

            assertEquals(NAMESPACE + ":" + WORKER_CLASSIFICATION_NAME, services.iterator().next().getId());
            assertEquals(Integer.valueOf(MAX), services.iterator().next().getMaxInstances());
            assertEquals(Integer.valueOf(MIN), services.iterator().next().getMinInstances());
            assertEquals(Integer.valueOf(INTERVAL), services.iterator().next().getInterval());
            assertEquals(Integer.valueOf(BACKOFF), services.iterator().next().getBackoffAmount());
            assertEquals(WORKER_CLASSIFICATION_TARGET, services.iterator().next().getScalingTarget());
            assertEquals(METRIC, services.iterator().next().getWorkloadMetric());
        }
    }

    @Test
    public void multipleGroupPathTest() throws ScalerException, ApiException {
        final IoK8sApiAppsV1Deployment classificationWorkerDeployment = createDeploymentWithLabels(
                WORKER_CLASSIFICATION_NAME,
                NAMESPACE,
                MIN,
                MAX,
                WORKER_CLASSIFICATION_TARGET,
                GROUPID,
                INTERVAL,
                BACKOFF,
                PROFILE,
                WORKER_CLASSIFICATION_TARGET,
                METRIC,
                SHUTDOWN_PRIORITY,
                REPLICAS,
                WORKER_CLASSIFICATION_NAME);

        final IoK8sApiAppsV1Deployment entityextractWorkerDeployment = createDeploymentWithLabels(
                WORKER_ENTITYEXTRACT_NAME,
                NAMESPACE,
                MIN,
                MAX,
                WORKER_ENTITYEXTRACT_TARGET,
                GROUPID,
                INTERVAL,
                BACKOFF,
                PROFILE,
                WORKER_ENTITYEXTRACT_TARGET,
                METRIC,
                SHUTDOWN_PRIORITY,
                REPLICAS,
                WORKER_ENTITYEXTRACT_NAME);

        final List<IoK8sApiAppsV1Deployment> deployments = new ArrayList<>();
        deployments.add(classificationWorkerDeployment);
        deployments.add(entityextractWorkerDeployment);

        final IoK8sApiAppsV1DeploymentList deploymentList = new IoK8sApiAppsV1DeploymentList();
        deploymentList.setItems(deployments);

        final AppsV1Api.APIlistAppsV1NamespacedDeploymentRequest apilistAppsV1NamespacedDeploymentRequestMock =
                mock(AppsV1Api.APIlistAppsV1NamespacedDeploymentRequest.class);

        when(apilistAppsV1NamespacedDeploymentRequestMock.execute()).thenReturn(deploymentList);

        final K8sAutoscaleConfiguration config = Mockito.mock(K8sAutoscaleConfiguration.class);
        final String namespaces = "private";
        final List<String> namespacesArray = Stream.of(namespaces.split(","))
                .map(String::trim)
                .collect(toList());
        when(config.getNamespacesArray()).thenReturn(namespacesArray);
        when(config.getMaximumInstances()).thenReturn(4);
        when(config.getGroupId()).thenReturn("managed-queue-workers");

        try (MockedConstruction<AppsV1Api> appsV1ApiMock = Mockito.mockConstruction(AppsV1Api.class, (mock, context) -> {
            when(mock.listAppsV1NamespacedDeployment("private")).thenReturn(apilistAppsV1NamespacedDeploymentRequestMock);
        })) {
            final ApiClient apiClientMock = mock(ApiClient.class);

            final K8sServiceSource source = new K8sServiceSource(config, apiClientMock);
            final Set<ScalingConfiguration> services = source.getServices();

            assertEquals(2, services.size());
        }
    }

    @Test
    public void getServicesExceptionTest() {
        final K8sAutoscaleConfiguration config = Mockito.mock(K8sAutoscaleConfiguration.class);
        final String namespaces = "private";
        final List<String> namespacesArray = Stream.of(namespaces.split(","))
                .map(String::trim)
                .collect(toList());
        when(config.getNamespacesArray()).thenReturn(namespacesArray);
        when(config.getMaximumInstances()).thenReturn(4);
        when(config.getGroupId()).thenReturn("managed-queue-workers");

        try (MockedConstruction<AppsV1Api> appsV1ApiMock = Mockito.mockConstruction(AppsV1Api.class, (mock, context) -> {
            when(mock.listAppsV1NamespacedDeployment("private")).thenThrow(new ApiException("Test exception"));
        })) {
            final ApiClient apiClientMock = mock(ApiClient.class);

            final K8sServiceSource source = new K8sServiceSource(config, apiClientMock);
            Assertions.assertThrows(ScalerException.class, source::getServices);
        }
    }

    private static IoK8sApiAppsV1Deployment createDeploymentWithLabels(
            final String name,
            final String namespace,
            final String minInstances,
            final String maxInstances,
            final String targetQueueName,
            final String autoscaleGroupid,
            final String autoscaleInterval,
            final String autoscaleBackoff,
            final String autoscaleScalingProfile,
            final String autoscaleScalingTarget,
            final String autoscaleMetric,
            final String autoscaleShutdownPriority,
            final int replicas,
            final String podName)
    {
        final IoK8sApiAppsV1Deployment deployment = new IoK8sApiAppsV1Deployment();

        deployment.setMetadata(new IoK8sApimachineryPkgApisMetaV1ObjectMeta());
        deployment.getMetadata().setName(name);
        deployment.getMetadata().setNamespace(namespace);
        deployment.getMetadata().setLabels(ImmutableMap.<String,String>builder()
                .put("autoscale.mininstances", minInstances)
                .put("autoscale.maxinstances", maxInstances)
                .put("messageprioritization.targetqueuename", targetQueueName)
                .put("autoscale.groupid", autoscaleGroupid)
                .put("autoscale.interval", autoscaleInterval)
                .put("autoscale.backoff", autoscaleBackoff)
                .put("autoscale.scalingprofile", autoscaleScalingProfile)
                .put("autoscale.scalingtarget", autoscaleScalingTarget)
                .put("autoscale.metric", autoscaleMetric)
                .put("autoscale.shutdownpriority", autoscaleShutdownPriority)
                .build());

        final IoK8sApiAppsV1DeploymentSpec spec = new IoK8sApiAppsV1DeploymentSpec();
        final IoK8sApimachineryPkgApisMetaV1LabelSelector selector = new IoK8sApimachineryPkgApisMetaV1LabelSelector();
        final Map<String, String> matchLabels = new HashMap<>();
        matchLabels.put("app", podName);
        selector.setMatchLabels(matchLabels);
        spec.setSelector(selector);
        deployment.setSpec(spec);

        deployment.getSpec().setReplicas(replicas);

        return deployment;
    }
}
