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
import com.hpe.caf.api.autoscale.ScalerException;
import com.hpe.caf.api.autoscale.ScalingConfiguration;
import static com.hpe.caf.api.autoscale.ScalingConfiguration.KEY_BACKOFF_AMOUNT;
import static com.hpe.caf.api.autoscale.ScalingConfiguration.KEY_INTERVAL;
import static com.hpe.caf.api.autoscale.ScalingConfiguration.KEY_MAX_INSTANCES;
import static com.hpe.caf.api.autoscale.ScalingConfiguration.KEY_MIN_INSTANCES;
import static com.hpe.caf.api.autoscale.ScalingConfiguration.KEY_SCALE_DOWN_BACKOFF_AMOUNT;
import static com.hpe.caf.api.autoscale.ScalingConfiguration.KEY_SCALE_UP_BACKOFF_AMOUNT;
import static com.hpe.caf.api.autoscale.ScalingConfiguration.KEY_SCALING_PROFILE;
import static com.hpe.caf.api.autoscale.ScalingConfiguration.KEY_SCALING_TARGET;
import static com.hpe.caf.api.autoscale.ScalingConfiguration.KEY_WORKLOAD_METRIC;
import com.hpe.caf.autoscale.K8sAutoscaleConfiguration;
import io.kubernetes.client.openapi.ApiException;
import io.kubernetes.client.openapi.apis.AppsV1Api;
import io.kubernetes.client.openapi.models.V1Deployment;
import io.kubernetes.client.openapi.models.V1DeploymentList;
import io.kubernetes.client.openapi.models.V1ObjectMeta;
import org.hamcrest.CoreMatchers;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ErrorCollector;
import static org.mockito.ArgumentMatchers.any;
import org.mockito.Mockito;
import static org.mockito.Mockito.when;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public class K8sServiceSourceTest
{
    private K8sServiceSource source;
    private final String METRIC = "rabbitmq";
    private final String PROFILE = "profile";
    private final String DEPLOYMENT_NAME = "myreplicaset";

    @Rule
    public ErrorCollector errorCollector = new ErrorCollector();

    @Before
    public void setup() throws ApiException
    {
        final AppsV1Api mockApi = Mockito.mock(AppsV1Api.class);
        final V1DeploymentList mockDeploymentList = Mockito.mock(V1DeploymentList.class);
        final V1Deployment mockDeploymentForRabbitMQ = Mockito.mock(V1Deployment.class);
        final V1ObjectMeta mockV1ObjectMeta = Mockito.mock(V1ObjectMeta.class);
        final Map<String, String> labelsForRabbitMQDeployment = new HashMap<>();
        labelsForRabbitMQDeployment.put(KEY_WORKLOAD_METRIC, METRIC);
        labelsForRabbitMQDeployment.put(KEY_SCALING_PROFILE, PROFILE);
        labelsForRabbitMQDeployment.put(KEY_BACKOFF_AMOUNT, "1");
        labelsForRabbitMQDeployment.put(KEY_INTERVAL, "2");
        labelsForRabbitMQDeployment.put(KEY_MAX_INSTANCES, "3");
        labelsForRabbitMQDeployment.put(KEY_MIN_INSTANCES, "4");
        labelsForRabbitMQDeployment.put(KEY_SCALE_DOWN_BACKOFF_AMOUNT, "5");
        labelsForRabbitMQDeployment.put(KEY_SCALE_UP_BACKOFF_AMOUNT, "6");
        labelsForRabbitMQDeployment.put(KEY_SCALING_TARGET, "7");
        when(mockV1ObjectMeta.getLabels()).thenReturn(labelsForRabbitMQDeployment);
        when(mockV1ObjectMeta.getName()).thenReturn(DEPLOYMENT_NAME);
        when(mockDeploymentForRabbitMQ.getMetadata()).thenReturn(mockV1ObjectMeta);

        final V1Deployment mockDeployment_NOT_FOR_RABBITMQ = Mockito.mock(V1Deployment.class);
        final V1ObjectMeta mockV1ObjectMeta_NOT_FOR_RABBITMQ = Mockito.mock(V1ObjectMeta.class);
        final Map<String, String> labelsMQDeployment_NOT_FOR_RABBITMQ = new HashMap<>();
        labelsMQDeployment_NOT_FOR_RABBITMQ.put(KEY_WORKLOAD_METRIC, "SomethingElse");
        when(mockV1ObjectMeta_NOT_FOR_RABBITMQ.getName()).thenReturn("SomethingElse");
        when(mockV1ObjectMeta_NOT_FOR_RABBITMQ.getLabels()).thenReturn(labelsMQDeployment_NOT_FOR_RABBITMQ);
        when(mockDeployment_NOT_FOR_RABBITMQ.getMetadata()).thenReturn(mockV1ObjectMeta_NOT_FOR_RABBITMQ);

        //  This will contain 2 items, only one of which is labeled for rabbit mq scaling.
        when(mockDeploymentList.getItems()).thenReturn(Arrays.asList(mockDeploymentForRabbitMQ, mockDeployment_NOT_FOR_RABBITMQ));
        when(mockApi.listNamespacedDeployment(
            any(), any(), any(), any(), any(), any(), any(), any(), any(), any())
        ).thenReturn(mockDeploymentList);
        final K8sAutoscaleConfiguration cfg = new K8sAutoscaleConfiguration();
        cfg.setMetric(METRIC);
        source = new K8sServiceSource(mockApi, cfg);
    }

    @Test
    public void checkThatLabelsAreConvertedToScalingConfigurationTest() throws ScalerException
    { 
        final Set<ScalingConfiguration> scSet = source.getServices();
        assertNotNull("K8sServiceSource should not be null", scSet);
        assertEquals("There should only be one ScalingConfiguration", 1, scSet.size());
        final ScalingConfiguration sc = scSet.iterator().next();
        errorCollector.checkThat("Workload metric should be " + METRIC, sc.getWorkloadMetric(), CoreMatchers.equalTo(METRIC));
        errorCollector.checkThat("Scaling profile should be " + PROFILE, sc.getScalingProfile(), CoreMatchers.equalTo(PROFILE));
        errorCollector.checkThat("Id should be the name of the replica set", sc.getId(), CoreMatchers.equalTo(DEPLOYMENT_NAME));
        errorCollector.checkThat("Back off amount should be 1", sc.getBackoffAmount(), CoreMatchers.equalTo(1));
        errorCollector.checkThat("Interval should be 2", sc.getInterval(), CoreMatchers.equalTo(2));
        errorCollector.checkThat("Max instances should be 3", sc.getMaxInstances(), CoreMatchers.equalTo(3));
        errorCollector.checkThat("Min instances should be 4", sc.getMinInstances(), CoreMatchers.equalTo(4));
        errorCollector.checkThat("Scale down backoff should be 5", sc.getScaleDownBackoffAmount(), CoreMatchers.equalTo(5));
        errorCollector.checkThat("Scale ip backoff should be 6", sc.getScaleUpBackoffAmount(), CoreMatchers.equalTo(6));
        errorCollector.checkThat("Scaling target should be (string)7", sc.getScalingTarget(), CoreMatchers.equalTo("7"));
    }

    @Test
    public void healthCheckTest() {
        assertEquals(HealthResult.RESULT_HEALTHY, source.healthCheck());
    }
}
