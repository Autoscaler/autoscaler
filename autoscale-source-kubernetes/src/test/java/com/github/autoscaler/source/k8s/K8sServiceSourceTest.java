package com.github.autoscaler.source.k8s;

import com.github.autoscaler.api.ScalerException;
import com.github.autoscaler.api.ScalingConfiguration;
import com.github.autoscaler.kubernetes.shared.K8sAutoscaleConfiguration;
import com.github.autoscaler.source.kubernetes.K8sServiceSource;
import com.google.common.collect.ImmutableMap;
import io.kubernetes.client.extended.kubectl.Kubectl;
import io.kubernetes.client.extended.kubectl.KubectlGet;
import io.kubernetes.client.extended.kubectl.exception.KubectlException;
import io.kubernetes.client.openapi.models.V1Deployment;
import io.kubernetes.client.openapi.models.V1DeploymentSpec;
import io.kubernetes.client.openapi.models.V1ObjectMeta;
import org.apache.commons.compress.utils.Lists;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

import java.util.List;
import java.util.Set;
import java.util.stream.Stream;

import static java.util.stream.Collectors.toList;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
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

    @Test
    public void getServicesTest()
            throws ScalerException {

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
                3);

        final List<V1Deployment> deployments = Lists.newArrayList();
        deployments.add(classificationWorkerDeployment);

        final K8sAutoscaleConfiguration config = Mockito.mock(K8sAutoscaleConfiguration.class);
        final String namespaces = "private";
        final List<String> namespacesArray = Stream.of(namespaces.split(","))
                .map(String::trim)
                .collect(toList());
        when(config.getNamespacesArray()).thenReturn(namespacesArray);
        when(config.getMaximumInstances()).thenReturn(4);
        when(config.getGroupId()).thenReturn("managed-queue-workers");

        try (MockedStatic<Kubectl> kubectlStaticMock = Mockito.mockStatic(Kubectl.class)) {
            final KubectlGet<V1Deployment> getMock = mock(KubectlGet.class);
            kubectlStaticMock.when(() -> Kubectl.get(V1Deployment.class)).thenReturn(getMock);
            when(getMock.execute()).thenReturn(deployments);
            when(getMock.namespace(any())).thenReturn(getMock);

            final K8sServiceSource source = new K8sServiceSource(config);
            Set<ScalingConfiguration> services = source.getServices();

            assertEquals(1, services.size());

            assertEquals(NAMESPACE + ":" + WORKER_CLASSIFICATION_NAME, services.iterator().next().getId());
            assertEquals(Integer.valueOf(MAX), services.iterator().next().getMaxInstances());
            assertEquals(Integer.valueOf(MIN), services.iterator().next().getMinInstances());
            assertEquals(Integer.valueOf(INTERVAL), services.iterator().next().getInterval());
            assertEquals(Integer.valueOf(BACKOFF), services.iterator().next().getBackoffAmount());
            assertEquals(WORKER_CLASSIFICATION_TARGET, services.iterator().next().getScalingTarget());
            assertEquals(METRIC, services.iterator().next().getWorkloadMetric());
        } catch (KubectlException e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    public void multipleGroupPathTest() throws ScalerException, KubectlException{
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
                3);

        final V1Deployment entityextractWorkerDeployment = createDeploymentWithLabels(
                WORKER_ENTITYEXTRACT_NAME,
                NAMESPACE,
                MIN,
                MAX,
                WORKER_ENTITYEXTRACT_TARGET,
                GROUPID,
                INTERVAL,
                BACKOFF,
                PROFILE,
                WORKER_CLASSIFICATION_TARGET,
                METRIC,
                SHUTDOWN_PRIORITY,
                3);

        final List<V1Deployment> deployments = Lists.newArrayList();
        deployments.add(classificationWorkerDeployment);
        deployments.add(entityextractWorkerDeployment);

        final K8sAutoscaleConfiguration config = Mockito.mock(K8sAutoscaleConfiguration.class);
        final String namespaces = "private";
        final List<String> namespacesArray = Stream.of(namespaces.split(","))
                .map(String::trim)
                .collect(toList());
        when(config.getNamespacesArray()).thenReturn(namespacesArray);
        when(config.getMaximumInstances()).thenReturn(4);
        when(config.getGroupId()).thenReturn("managed-queue-workers");

        try (MockedStatic<Kubectl> kubectlStaticMock = Mockito.mockStatic(Kubectl.class)) {
            final KubectlGet<V1Deployment> getMock = mock(KubectlGet.class);
            kubectlStaticMock.when(() -> Kubectl.get(V1Deployment.class)).thenReturn(getMock);
            when(getMock.execute()).thenReturn(deployments);
            when(getMock.namespace(any())).thenReturn(getMock);

            final K8sServiceSource source = new K8sServiceSource(config);
            Set<ScalingConfiguration> services = source.getServices();

            assertEquals(2, services.size());
        }
    }

    @Test
    public void getServicesExceptionTest() throws KubectlException {

        final K8sAutoscaleConfiguration config = Mockito.mock(K8sAutoscaleConfiguration.class);
        final String namespaces = "private";
        final List<String> namespacesArray = Stream.of(namespaces.split(","))
                .map(String::trim)
                .collect(toList());
        when(config.getNamespacesArray()).thenReturn(namespacesArray);
        when(config.getMaximumInstances()).thenReturn(4);
        when(config.getGroupId()).thenReturn("managed-queue-workers");

        try (MockedStatic<Kubectl> kubectlStaticMock = Mockito.mockStatic(Kubectl.class)) {
            final KubectlGet<V1Deployment> getMock = mock(KubectlGet.class);
            kubectlStaticMock.when(() -> Kubectl.get(V1Deployment.class)).thenReturn(getMock);
            when(getMock.execute()).thenThrow(KubectlException.class);
            when(getMock.namespace(any())).thenReturn(getMock);

            final K8sServiceSource source = new K8sServiceSource(config);
            Assertions.assertThrows(ScalerException.class, source::getServices);
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
            final int replicas)
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
                .build());
        deployment.setSpec(new V1DeploymentSpec());
        deployment.getSpec().setReplicas(replicas);

        return deployment;
    }
}
