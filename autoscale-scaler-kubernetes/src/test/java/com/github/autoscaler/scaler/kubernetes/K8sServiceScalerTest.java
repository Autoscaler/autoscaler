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
import com.github.autoscaler.kubernetes.shared.K8sAutoscaleConfiguration;
import com.google.common.collect.ImmutableMap;
import io.kubernetes.client.extended.kubectl.Kubectl;
import io.kubernetes.client.extended.kubectl.KubectlGet;
import io.kubernetes.client.extended.kubectl.KubectlScale;
import io.kubernetes.client.extended.kubectl.exception.KubectlException;
import io.kubernetes.client.openapi.models.V1Deployment;
import io.kubernetes.client.openapi.models.V1Pod;
import io.kubernetes.client.openapi.models.V1DeploymentSpec;
import io.kubernetes.client.openapi.models.V1PodStatus;
import io.kubernetes.client.openapi.models.V1ObjectMeta;
import org.apache.commons.compress.utils.Lists;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.times;

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
    public void scaleUpTest()
            throws ScalerException, KubectlException
    {

        final V1Deployment classificationWorkerDeployment = createDeploymentWithLabels(
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

        final List<V1Deployment> deployments = Lists.newArrayList();
        deployments.add(classificationWorkerDeployment);

        try (MockedStatic<Kubectl> kubectlStaticMock = Mockito.mockStatic(Kubectl.class)) {

            setupMocks(kubectlStaticMock, classificationWorkerDeployment);

            final KubectlScale<V1Deployment> scaleMock = mock(KubectlScale.class);

            when(Kubectl.scale(V1Deployment.class)).thenReturn(scaleMock);
            when(scaleMock.namespace(any())).thenReturn(scaleMock);
            when(scaleMock.namespace(any()).name(any())).thenReturn(scaleMock);
            when(scaleMock.namespace(any()).name(any()).replicas(anyInt())).thenReturn(scaleMock);
            when(scaleMock.namespace(any()).name(any()).replicas(anyInt()).execute()).thenReturn(classificationWorkerDeployment);

            final K8sAutoscaleConfiguration config = Mockito.mock(K8sAutoscaleConfiguration.class);
            final K8sServiceScaler scaler = new K8sServiceScaler(config);
            when(config.getMaximumInstances()).thenReturn(Integer.valueOf(MAX));

            scaler.scaleUp("private:classification-worker", 1);

            Mockito.verify(scaleMock).replicas(REPLICAS + 1);
        }
    }

    @Test
    @SuppressWarnings("ThrowableResultIgnored")
    public void scaleUpExceptionTest()
            throws ScalerException, KubectlException
    {

        final V1Deployment classificationWorkerDeployment = createDeploymentWithLabels(
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

        final List<V1Deployment> deployments = Lists.newArrayList();
        deployments.add(classificationWorkerDeployment);

        try (MockedStatic<Kubectl> kubectlStaticMock = Mockito.mockStatic(Kubectl.class)) {

            setupMocks(kubectlStaticMock, classificationWorkerDeployment);

            final KubectlScale<V1Deployment> scaleMock = mock(KubectlScale.class);

            when(Kubectl.scale(V1Deployment.class)).thenReturn(scaleMock);
            when(scaleMock.namespace(any())).thenReturn(scaleMock);
            when(scaleMock.namespace(any()).name(any())).thenReturn(scaleMock);
            when(scaleMock.namespace(any()).name(any()).replicas(anyInt())).thenReturn(scaleMock);
            when(scaleMock.execute()).thenThrow(KubectlException.class);

            final K8sAutoscaleConfiguration config = Mockito.mock(K8sAutoscaleConfiguration.class);
            final K8sServiceScaler scaler = new K8sServiceScaler(config);
            when(config.getMaximumInstances()).thenReturn(Integer.valueOf(MAX));

            Assertions.assertThrows(ScalerException.class, () -> scaler.scaleUp("private:classification-worker", 1));
        }
    }

    @Test
    public void scaleDownTest()
            throws ScalerException, KubectlException
    {

        final V1Deployment classificationWorkerDeployment = createDeploymentWithLabels(
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

        final List<V1Deployment> deployments = Lists.newArrayList();
        deployments.add(classificationWorkerDeployment);

        try (MockedStatic<Kubectl> kubectlStaticMock = Mockito.mockStatic(Kubectl.class)) {

            setupMocks(kubectlStaticMock, classificationWorkerDeployment);

            final KubectlScale<V1Deployment> scaleMock = mock(KubectlScale.class);

            when(Kubectl.scale(V1Deployment.class)).thenReturn(scaleMock);
            when(scaleMock.namespace(any())).thenReturn(scaleMock);
            when(scaleMock.namespace(any()).name(any())).thenReturn(scaleMock);
            when(scaleMock.namespace(any()).name(any()).replicas(anyInt())).thenReturn(scaleMock);
            when(scaleMock.namespace(any()).name(any()).replicas(anyInt()).execute()).thenReturn(classificationWorkerDeployment);

            final K8sAutoscaleConfiguration config = Mockito.mock(K8sAutoscaleConfiguration.class);
            final K8sServiceScaler scaler = new K8sServiceScaler(config);
            when(config.getMaximumInstances()).thenReturn(Integer.valueOf(MAX));

            scaler.scaleDown("private:classification-worker", 1);

            Mockito.verify(scaleMock).replicas(REPLICAS - 1);
        }
    }

    @Test
    @SuppressWarnings("ThrowableResultIgnored")
    public void scaleDownExceptionTest()
            throws ScalerException, KubectlException
    {

        final V1Deployment classificationWorkerDeployment = createDeploymentWithLabels(
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

        final List<V1Deployment> deployments = Lists.newArrayList();
        deployments.add(classificationWorkerDeployment);

        try (MockedStatic<Kubectl> kubectlStaticMock = Mockito.mockStatic(Kubectl.class)) {

            setupMocks(kubectlStaticMock, classificationWorkerDeployment);

            final KubectlScale<V1Deployment> scaleMock = mock(KubectlScale.class);

            when(Kubectl.scale(V1Deployment.class)).thenReturn(scaleMock);
            when(scaleMock.namespace(any())).thenReturn(scaleMock);
            when(scaleMock.namespace(any()).name(any())).thenReturn(scaleMock);
            when(scaleMock.namespace(any()).name(any()).replicas(anyInt())).thenReturn(scaleMock);
            when(scaleMock.execute()).thenThrow(KubectlException.class);

            final K8sAutoscaleConfiguration config = Mockito.mock(K8sAutoscaleConfiguration.class);
            final K8sServiceScaler scaler = new K8sServiceScaler(config);
            when(config.getMaximumInstances()).thenReturn(Integer.valueOf(MAX));

            Assertions.assertThrows(ScalerException.class, () -> scaler.scaleDown("private:classification-worker", 1));
        }
    }

    @Test
    public void scaleUpMaxTest()
            throws ScalerException, KubectlException
    {

        final V1Deployment classificationWorkerDeployment = createDeploymentWithLabels(
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

        final List<V1Deployment> deployments = Lists.newArrayList();
        deployments.add(classificationWorkerDeployment);

        try (MockedStatic<Kubectl> kubectlStaticMock = Mockito.mockStatic(Kubectl.class)) {

            setupMocks(kubectlStaticMock, classificationWorkerDeployment);

            final KubectlScale<V1Deployment> scaleMock = mock(KubectlScale.class);

            when(Kubectl.scale(V1Deployment.class)).thenReturn(scaleMock);
            when(scaleMock.namespace(any())).thenReturn(scaleMock);
            when(scaleMock.namespace(any()).name(any())).thenReturn(scaleMock);
            when(scaleMock.namespace(any()).name(any()).replicas(anyInt())).thenReturn(scaleMock);

            final K8sAutoscaleConfiguration config = Mockito.mock(K8sAutoscaleConfiguration.class);
            final K8sServiceScaler scaler = new K8sServiceScaler(config);
            when(config.getMaximumInstances()).thenReturn(Integer.valueOf(MAX));

            scaler.scaleUp("private:classification-worker", 1);

            Mockito.verify(scaleMock, times(0)).replicas(anyInt());
        }
    }

    @Test
    public void scaleDownMinTest()
            throws ScalerException, KubectlException
    {

        final V1Deployment classificationWorkerDeployment = createDeploymentWithLabels(
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

        final List<V1Deployment> deployments = Lists.newArrayList();
        deployments.add(classificationWorkerDeployment);

        try (MockedStatic<Kubectl> kubectlStaticMock = Mockito.mockStatic(Kubectl.class)) {

            setupMocks(kubectlStaticMock, classificationWorkerDeployment);

            final KubectlScale<V1Deployment> scaleMock = mock(KubectlScale.class);

            when(Kubectl.scale(V1Deployment.class)).thenReturn(scaleMock);
            when(scaleMock.namespace(any())).thenReturn(scaleMock);
            when(scaleMock.namespace(any()).name(any())).thenReturn(scaleMock);
            when(scaleMock.namespace(any()).name(any()).replicas(anyInt())).thenReturn(scaleMock);

            final K8sAutoscaleConfiguration config = Mockito.mock(K8sAutoscaleConfiguration.class);
            final K8sServiceScaler scaler = new K8sServiceScaler(config);
            when(config.getMaximumInstances()).thenReturn(Integer.valueOf(MAX));

            scaler.scaleDown("private:classification-worker", 1);

            Mockito.verify(scaleMock, times(0)).replicas(anyInt());
        }
    }


    @Test
    public void getInstanceInfoTest() throws ScalerException, KubectlException
    {
        final V1Deployment classificationWorkerDeployment = createDeploymentWithLabels(
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

        final List<V1Deployment> deployments = Lists.newArrayList();
        deployments.add(classificationWorkerDeployment);
        try (MockedStatic<Kubectl> kubectlStaticMock = Mockito.mockStatic(Kubectl.class)) {

            setupMocks(kubectlStaticMock, classificationWorkerDeployment);

            final KubectlGet<V1Pod> getPodMock = mock(KubectlGet.class);
            kubectlStaticMock.when(() -> Kubectl.get(eq(V1Pod.class)))
                    .thenReturn(getPodMock);
            when(getPodMock.namespace(any())).thenReturn(getPodMock);

            final List<V1Pod> pods = new ArrayList<>();
            pods.add(createRunningPod());
            pods.add(createRunningPod());
            pods.add(createStagingPod());

            when(getPodMock.execute()).thenReturn(pods);

            final K8sAutoscaleConfiguration config = Mockito.mock(K8sAutoscaleConfiguration.class);
            final K8sServiceScaler scaler = new K8sServiceScaler(config);
            final InstanceInfo dpInstanceInfo = scaler.getInstanceInfo("private:worker-classification");

            assertEquals(Integer.valueOf(REPLICAS), dpInstanceInfo.getInstances());
            assertEquals(Integer.valueOf(2), dpInstanceInfo.getInstancesRunning());
            assertEquals(Integer.valueOf(1), dpInstanceInfo.getInstancesStaging());
            assertEquals(Integer.valueOf(SHUTDOWN_PRIORITY), dpInstanceInfo.getShutdownPriority());
        }
    }

    @Test
    public void getInstanceInfoExceptionTest() throws ScalerException, KubectlException
    {
        final V1Deployment classificationWorkerDeployment = createDeploymentWithLabels(
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

        final List<V1Deployment> deployments = Lists.newArrayList();
        deployments.add(classificationWorkerDeployment);
        try (MockedStatic<Kubectl> kubectlStaticMock = Mockito.mockStatic(Kubectl.class)) {

            setupMocks(kubectlStaticMock, classificationWorkerDeployment);

            final KubectlGet<V1Pod> getPodMock = mock(KubectlGet.class);
            kubectlStaticMock.when(() -> Kubectl.get(eq(V1Pod.class)))
                    .thenReturn(getPodMock);
            when(getPodMock.namespace(any())).thenReturn(getPodMock);

            when(getPodMock.execute()).thenThrow(KubectlException.class);

            final K8sAutoscaleConfiguration config = Mockito.mock(K8sAutoscaleConfiguration.class);
            final K8sServiceScaler scaler = new K8sServiceScaler(config);

            Assertions.assertThrows(ScalerException.class, () -> scaler.getInstanceInfo("private:worker-classification"));
        }
    }

    private static V1Deployment createDeploymentWithLabels(
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
        final V1Deployment deployment = new V1Deployment();

        deployment.setMetadata(new V1ObjectMeta());
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

        deployment.setSpec(new V1DeploymentSpec());
        deployment.getSpec().setReplicas(replicas);

        return deployment;
    }

    private static V1Pod createRunningPod()
    {
        final V1Pod pod = new V1Pod();

        pod.setStatus(new V1PodStatus());
        pod.getStatus().setPhase("Running");

        return pod;
    }

    private static V1Pod createStagingPod()
    {
        final V1Pod pod = new V1Pod();

        pod.setStatus(new V1PodStatus());
        pod.getStatus().setPhase("pending");

        return pod;
    }

    private static void setupMocks(
            final MockedStatic<Kubectl> kubectlStaticMock,
            final V1Deployment deployment) throws KubectlException
    {

        final KubectlGet<V1Deployment> getDeploymentMock = mock(KubectlGet.class);
        final KubectlGet.KubectlGetSingle kubectlGetSingle = mock(KubectlGet.KubectlGetSingle.class);

        kubectlStaticMock.when(() -> Kubectl.get(eq(V1Deployment.class)))
                .thenReturn(getDeploymentMock);

        when(getDeploymentMock.namespace(any())).thenReturn(getDeploymentMock);
        when(getDeploymentMock.namespace(any()).name(any())).thenReturn(kubectlGetSingle);
        when(getDeploymentMock.namespace(any()).name(any()).execute()).thenReturn(deployment);

        final V1Deployment v1Deployment = Mockito.mock(V1Deployment.class);
        final V1DeploymentSpec v1DeploymentSpec = Mockito.mock(V1DeploymentSpec.class);
        when(v1Deployment.getSpec()).thenReturn(v1DeploymentSpec);
        when(v1Deployment.getSpec().getReplicas()).thenReturn(REPLICAS);
    }
}
