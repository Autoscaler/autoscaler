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

import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.github.autoscaler.api.InstanceInfo;
import com.github.autoscaler.api.ScalerException;
import com.github.autoscaler.kubernetes.shared.K8sAutoscaleConfiguration;
import com.github.cafapi.kubernetes.client.api.AppsV1Api;
import com.github.cafapi.kubernetes.client.api.CoreV1Api;
import com.github.cafapi.kubernetes.client.client.ApiClient;
import com.github.cafapi.kubernetes.client.client.ApiException;
import com.github.cafapi.kubernetes.client.model.IoK8sApiAppsV1Deployment;
import com.github.cafapi.kubernetes.client.model.IoK8sApiAppsV1DeploymentSpec;
import com.github.cafapi.kubernetes.client.model.IoK8sApiCoreV1Pod;
import com.github.cafapi.kubernetes.client.model.IoK8sApiCoreV1PodList;
import com.github.cafapi.kubernetes.client.model.IoK8sApiCoreV1PodStatus;
import com.github.cafapi.kubernetes.client.model.IoK8sApimachineryPkgApisMetaV1LabelSelector;
import com.github.cafapi.kubernetes.client.model.IoK8sApimachineryPkgApisMetaV1ObjectMeta;
import com.google.common.collect.ImmutableMap;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.MockedConstruction;
import org.mockito.Mockito;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class K8sServiceScalerTest {

    private static final String WORKER_CLASSIFICATION_NAME = "classification-worker";
    private static final String NAMESPACE = "private";
    private static final String GROUPID = "managed-queue-workers";
    private static final String METRIC = "rabbitmq";
    private static final String WORKER_CLASSIFICATION_TARGET = "worker-classification-in";
    private static final String PROFILE = "default";
    private static final String INTERVAL = "30";
    private static final String MAX = "4";
    private static final String MIN = "0";
    private static final String BACKOFF = "10";
    private static final String SHUTDOWN_PRIORITY = "10";
    private static final int REPLICAS = 3;

    @Test
    public void scaleUpTest() throws ScalerException, ApiException
    {
        // Arrange
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

        final AppsV1Api.APIreadAppsV1NamespacedDeploymentRequest readRequestMock =
                mock(AppsV1Api.APIreadAppsV1NamespacedDeploymentRequest.class);
        final AppsV1Api.APIpatchAppsV1NamespacedDeploymentRequest patchRequestMock =
                mock(AppsV1Api.APIpatchAppsV1NamespacedDeploymentRequest.class);

        when(readRequestMock.execute()).thenReturn(classificationWorkerDeployment);

        try (MockedConstruction<AppsV1Api> appsV1ApiMock = Mockito.mockConstruction(AppsV1Api.class, (mock, context) -> {
                 when(mock.readAppsV1NamespacedDeployment(anyString(), anyString())).thenReturn(readRequestMock);
                 when(mock.patchAppsV1NamespacedDeployment(anyString(), anyString())).thenReturn(patchRequestMock);
             })
        ) {
            final ApiClient apiClientMock = mock(ApiClient.class);

            final K8sAutoscaleConfiguration configMock = mock(K8sAutoscaleConfiguration.class);
            when(configMock.getMaximumInstances()).thenReturn(Integer.valueOf(MAX));

            final K8sServiceScaler scaler = new K8sServiceScaler(configMock, apiClientMock);

            // Act
            scaler.scaleUp("private:classification-worker", 1);

            // Assert
            ArgumentCaptor<ArrayNode> bodyCaptor = ArgumentCaptor.forClass(ArrayNode.class);
            verify(patchRequestMock).body(bodyCaptor.capture());
            ArrayNode capturedBody = bodyCaptor.getValue();

            assertEquals(1, capturedBody.size());
            ObjectNode patchOperation = (ObjectNode) capturedBody.get(0);
            assertEquals("replace", patchOperation.get("op").asText());
            assertEquals("/spec/replicas", patchOperation.get("path").asText());
            assertEquals(REPLICAS + 1, patchOperation.get("value").asInt());

            verify(patchRequestMock).execute();
        }
    }

    @Test
    @SuppressWarnings("ThrowableResultIgnored")
    public void scaleUpExceptionTest() throws ApiException
    {
        // Arrange
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

        final AppsV1Api.APIreadAppsV1NamespacedDeploymentRequest readRequestMock =
                mock(AppsV1Api.APIreadAppsV1NamespacedDeploymentRequest.class);
        final AppsV1Api.APIpatchAppsV1NamespacedDeploymentRequest patchRequestMock =
                mock(AppsV1Api.APIpatchAppsV1NamespacedDeploymentRequest.class);

        when(readRequestMock.execute()).thenReturn(classificationWorkerDeployment);
        when(patchRequestMock.execute()).thenThrow(ApiException.class);

        try (MockedConstruction<AppsV1Api> appsV1ApiMock = Mockito.mockConstruction(AppsV1Api.class, (mock, context) -> {
            when(mock.readAppsV1NamespacedDeployment(anyString(), anyString())).thenReturn(readRequestMock);
            when(mock.patchAppsV1NamespacedDeployment(anyString(), anyString())).thenReturn(patchRequestMock);
        })
        ) {
            final ApiClient apiClientMock = mock(ApiClient.class);

            final K8sAutoscaleConfiguration configMock = mock(K8sAutoscaleConfiguration.class);
            when(configMock.getMaximumInstances()).thenReturn(Integer.valueOf(MAX));

            final K8sServiceScaler scaler = new K8sServiceScaler(configMock, apiClientMock);

            // Act & Assert
            Assertions.assertThrows(ScalerException.class, () -> scaler.scaleUp("private:classification-worker", 1));
        }
    }

    @Test
    public void scaleDownTest() throws ScalerException, ApiException
    {
        // Arrange
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

        final AppsV1Api.APIreadAppsV1NamespacedDeploymentRequest readRequestMock =
                mock(AppsV1Api.APIreadAppsV1NamespacedDeploymentRequest.class);
        final AppsV1Api.APIpatchAppsV1NamespacedDeploymentRequest patchRequestMock =
                mock(AppsV1Api.APIpatchAppsV1NamespacedDeploymentRequest.class);

        when(readRequestMock.execute()).thenReturn(classificationWorkerDeployment);

        try (MockedConstruction<AppsV1Api> appsV1ApiMock = Mockito.mockConstruction(AppsV1Api.class, (mock, context) -> {
            when(mock.readAppsV1NamespacedDeployment(anyString(), anyString())).thenReturn(readRequestMock);
            when(mock.patchAppsV1NamespacedDeployment(anyString(), anyString())).thenReturn(patchRequestMock);
        })
        ) {
            final ApiClient apiClientMock = mock(ApiClient.class);

            final K8sAutoscaleConfiguration configMock = mock(K8sAutoscaleConfiguration.class);
            when(configMock.getMaximumInstances()).thenReturn(Integer.valueOf(MAX));

            final K8sServiceScaler scaler = new K8sServiceScaler(configMock, apiClientMock);

            // Act
            scaler.scaleDown("private:classification-worker", 1);

            // Assert
            ArgumentCaptor<ArrayNode> bodyCaptor = ArgumentCaptor.forClass(ArrayNode.class);
            verify(patchRequestMock).body(bodyCaptor.capture());
            ArrayNode capturedBody = bodyCaptor.getValue();

            assertEquals(1, capturedBody.size());
            ObjectNode patchOperation = (ObjectNode) capturedBody.get(0);
            assertEquals("replace", patchOperation.get("op").asText());
            assertEquals("/spec/replicas", patchOperation.get("path").asText());
            assertEquals(REPLICAS - 1, patchOperation.get("value").asInt());

            verify(patchRequestMock).execute();
        }
    }

    @Test
    @SuppressWarnings("ThrowableResultIgnored")
    public void scaleDownExceptionTest() throws ApiException
    {
        // Arrange
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

        final AppsV1Api.APIreadAppsV1NamespacedDeploymentRequest readRequestMock =
                mock(AppsV1Api.APIreadAppsV1NamespacedDeploymentRequest.class);
        final AppsV1Api.APIpatchAppsV1NamespacedDeploymentRequest patchRequestMock =
                mock(AppsV1Api.APIpatchAppsV1NamespacedDeploymentRequest.class);

        when(readRequestMock.execute()).thenReturn(classificationWorkerDeployment);
        when(patchRequestMock.execute()).thenThrow(ApiException.class);

        try (MockedConstruction<AppsV1Api> appsV1ApiMock = Mockito.mockConstruction(AppsV1Api.class, (mock, context) -> {
            when(mock.readAppsV1NamespacedDeployment(anyString(), anyString())).thenReturn(readRequestMock);
            when(mock.patchAppsV1NamespacedDeployment(anyString(), anyString())).thenReturn(patchRequestMock);
        })
        ) {
            final ApiClient apiClientMock = mock(ApiClient.class);

            final K8sAutoscaleConfiguration configMock = mock(K8sAutoscaleConfiguration.class);
            when(configMock.getMaximumInstances()).thenReturn(Integer.valueOf(MAX));

            final K8sServiceScaler scaler = new K8sServiceScaler(configMock, apiClientMock);

            // Act & Assert
            Assertions.assertThrows(ScalerException.class, () -> scaler.scaleDown("private:classification-worker", 1));
        }
    }

    @Test
    public void scaleUpMaxTest() throws ScalerException, ApiException
    {
        // Arrange
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
                Integer.parseInt(MAX),
                WORKER_CLASSIFICATION_NAME);

        final AppsV1Api.APIreadAppsV1NamespacedDeploymentRequest readRequestMock =
                mock(AppsV1Api.APIreadAppsV1NamespacedDeploymentRequest.class);
        final AppsV1Api.APIpatchAppsV1NamespacedDeploymentRequest patchRequestMock =
                mock(AppsV1Api.APIpatchAppsV1NamespacedDeploymentRequest.class);

        when(readRequestMock.execute()).thenReturn(classificationWorkerDeployment);

        try (MockedConstruction<AppsV1Api> appsV1ApiMock = Mockito.mockConstruction(AppsV1Api.class, (mock, context) -> {
            when(mock.readAppsV1NamespacedDeployment(anyString(), anyString())).thenReturn(readRequestMock);
            when(mock.patchAppsV1NamespacedDeployment(anyString(), anyString())).thenReturn(patchRequestMock);
        })
        ) {
            final ApiClient apiClientMock = mock(ApiClient.class);

            final K8sAutoscaleConfiguration configMock = mock(K8sAutoscaleConfiguration.class);
            when(configMock.getMaximumInstances()).thenReturn(Integer.valueOf(MAX));

            final K8sServiceScaler scaler = new K8sServiceScaler(configMock, apiClientMock);

            // Act
            scaler.scaleUp("private:classification-worker", 1);

            // Assert
            // Verify that no patch request is made when already at max instances
            verify(patchRequestMock, never()).body(any());
            verify(patchRequestMock, never()).execute();
        }
    }

    @Test
    public void scaleDownMinTest() throws ScalerException, ApiException
    {
        // Arrange
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
                Integer.parseInt(MIN),
                WORKER_CLASSIFICATION_NAME);

        final AppsV1Api.APIreadAppsV1NamespacedDeploymentRequest readRequestMock =
                mock(AppsV1Api.APIreadAppsV1NamespacedDeploymentRequest.class);
        final AppsV1Api.APIpatchAppsV1NamespacedDeploymentRequest patchRequestMock =
                mock(AppsV1Api.APIpatchAppsV1NamespacedDeploymentRequest.class);

        when(readRequestMock.execute()).thenReturn(classificationWorkerDeployment);

        try (MockedConstruction<AppsV1Api> appsV1ApiMock = Mockito.mockConstruction(AppsV1Api.class, (mock, context) -> {
            when(mock.readAppsV1NamespacedDeployment(anyString(), anyString())).thenReturn(readRequestMock);
            when(mock.patchAppsV1NamespacedDeployment(anyString(), anyString())).thenReturn(patchRequestMock);
        })
        ) {
            final ApiClient apiClientMock = mock(ApiClient.class);

            final K8sAutoscaleConfiguration configMock = mock(K8sAutoscaleConfiguration.class);
            when(configMock.getMaximumInstances()).thenReturn(Integer.valueOf(MAX));

            final K8sServiceScaler scaler = new K8sServiceScaler(configMock, apiClientMock);

            // Act
            scaler.scaleDown("private:classification-worker", 1);

            // Assert
            // Verify that no patch request is made when already at min instances
            verify(patchRequestMock, never()).body(any());
            verify(patchRequestMock, never()).execute();
        }
    }

    @Test
    public void getInstanceInfoTest() throws ScalerException, ApiException
    {
        // Arrange
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

        IoK8sApiAppsV1DeploymentSpec spec = new IoK8sApiAppsV1DeploymentSpec();
        IoK8sApimachineryPkgApisMetaV1LabelSelector selector = new IoK8sApimachineryPkgApisMetaV1LabelSelector();
        Map<String, String> matchLabels = new HashMap<>();
        matchLabels.put("app", WORKER_CLASSIFICATION_NAME);
        selector.setMatchLabels(matchLabels);
        spec.setSelector(selector);
        classificationWorkerDeployment.setSpec(spec);

        final AppsV1Api.APIreadAppsV1NamespacedDeploymentRequest readDeploymentRequestMock =
                mock(AppsV1Api.APIreadAppsV1NamespacedDeploymentRequest.class);
        final CoreV1Api.APIlistCoreV1NamespacedPodRequest listPodRequestMock =
                mock(CoreV1Api.APIlistCoreV1NamespacedPodRequest.class);

        when(readDeploymentRequestMock.execute()).thenReturn(classificationWorkerDeployment);

        final IoK8sApiCoreV1PodList podList = new IoK8sApiCoreV1PodList();
        podList.setItems(Arrays.asList(
                createRunningPod(),
                createRunningPod(),
                createStagingPod()
        ));

        when(listPodRequestMock.execute()).thenReturn(podList);
        when(listPodRequestMock.labelSelector(anyString())).thenReturn(listPodRequestMock);

        try (MockedConstruction<AppsV1Api> appsV1ApiMock = Mockito.mockConstruction(AppsV1Api.class, (mock, context) -> {
            when(mock.readAppsV1NamespacedDeployment(anyString(), anyString())).thenReturn(readDeploymentRequestMock);
        });
             MockedConstruction<CoreV1Api> coreV1ApiMock = Mockito.mockConstruction(CoreV1Api.class, (mock, context) -> {
                 when(mock.listCoreV1NamespacedPod(anyString())).thenReturn(listPodRequestMock);
             })
        ) {
            final ApiClient apiClientMock = mock(ApiClient.class);

            final K8sAutoscaleConfiguration configMock = mock(K8sAutoscaleConfiguration.class);

            final K8sServiceScaler scaler = new K8sServiceScaler(configMock, apiClientMock);

            // Act
            final InstanceInfo dpInstanceInfo = scaler.getInstanceInfo("private:worker-classification");

            // Assert
            assertEquals(Integer.valueOf(REPLICAS), dpInstanceInfo.getInstances());
            assertEquals(Integer.valueOf(2), dpInstanceInfo.getInstancesRunning());
            assertEquals(Integer.valueOf(1), dpInstanceInfo.getInstancesStaging());
            assertEquals(Integer.valueOf(SHUTDOWN_PRIORITY), dpInstanceInfo.getShutdownPriority());
        }
    }

    @Test
    public void getInstanceInfoExceptionTest() throws ApiException
    {
        // Arrange
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

        IoK8sApiAppsV1DeploymentSpec spec = new IoK8sApiAppsV1DeploymentSpec();
        IoK8sApimachineryPkgApisMetaV1LabelSelector selector = new IoK8sApimachineryPkgApisMetaV1LabelSelector();
        Map<String, String> matchLabels = new HashMap<>();
        matchLabels.put("app", WORKER_CLASSIFICATION_NAME);
        selector.setMatchLabels(matchLabels);
        spec.setSelector(selector);
        classificationWorkerDeployment.setSpec(spec);

        final AppsV1Api.APIreadAppsV1NamespacedDeploymentRequest readDeploymentRequestMock =
                mock(AppsV1Api.APIreadAppsV1NamespacedDeploymentRequest.class);
        final CoreV1Api.APIlistCoreV1NamespacedPodRequest listPodRequestMock =
                mock(CoreV1Api.APIlistCoreV1NamespacedPodRequest.class);

        when(readDeploymentRequestMock.execute()).thenReturn(classificationWorkerDeployment);
        when(listPodRequestMock.execute()).thenThrow(ApiException.class);
        when(listPodRequestMock.labelSelector(anyString())).thenReturn(listPodRequestMock);

        try (MockedConstruction<AppsV1Api> appsV1ApiMock = Mockito.mockConstruction(AppsV1Api.class, (mock, context) -> {
            when(mock.readAppsV1NamespacedDeployment(anyString(), anyString())).thenReturn(readDeploymentRequestMock);
        });
             MockedConstruction<CoreV1Api> coreV1ApiMock = Mockito.mockConstruction(CoreV1Api.class, (mock, context) -> {
                 when(mock.listCoreV1NamespacedPod(anyString())).thenReturn(listPodRequestMock);
             })
        ) {
            final ApiClient apiClientMock = mock(ApiClient.class);

            final K8sAutoscaleConfiguration configMock = mock(K8sAutoscaleConfiguration.class);

            final K8sServiceScaler scaler = new K8sServiceScaler(configMock, apiClientMock);

            // Act & Assert
            Assertions.assertThrows(ScalerException.class, () -> scaler.getInstanceInfo("private:worker-classification"));
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
                .put("app", podName)
                .build());

        deployment.setSpec(new IoK8sApiAppsV1DeploymentSpec());
        deployment.getSpec().setReplicas(replicas);

        return deployment;
    }

    private static IoK8sApiCoreV1Pod createRunningPod()
    {
        final IoK8sApiCoreV1Pod pod = new IoK8sApiCoreV1Pod();

        pod.setStatus(new IoK8sApiCoreV1PodStatus());
        pod.getStatus().setPhase("Running");

        return pod;
    }

    private static IoK8sApiCoreV1Pod createStagingPod()
    {
        final IoK8sApiCoreV1Pod pod = new IoK8sApiCoreV1Pod();

        pod.setStatus(new IoK8sApiCoreV1PodStatus());
        pod.getStatus().setPhase("pending");

        return pod;
    }
}
