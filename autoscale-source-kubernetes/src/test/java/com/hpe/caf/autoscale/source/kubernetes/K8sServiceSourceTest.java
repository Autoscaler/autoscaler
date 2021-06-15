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
import io.kubernetes.client.openapi.models.V1ObjectMeta;
import io.kubernetes.client.openapi.models.V1ReplicaSet;
import io.kubernetes.client.openapi.models.V1ReplicaSetList;
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
    private final String REPLICA_SET_NAME = "myreplicaset";
    
    @Rule
    public ErrorCollector errorCollector = new ErrorCollector();
    
    @Before
    public void setup() throws ApiException
    {
        final AppsV1Api mockApi = Mockito.mock(AppsV1Api.class);
        final V1ReplicaSetList mockReplicaSetList = Mockito.mock(V1ReplicaSetList.class);
        final V1ReplicaSet mockReplicaSetForRabbitMQ = Mockito.mock(V1ReplicaSet.class);
        
        final V1ObjectMeta mockV1ObjectMeta = Mockito.mock(V1ObjectMeta.class);
        final Map<String, String> labelsForRabbitMQReplicaSet = new HashMap<>();
        labelsForRabbitMQReplicaSet.put(KEY_WORKLOAD_METRIC, METRIC);
        labelsForRabbitMQReplicaSet.put(KEY_SCALING_PROFILE, PROFILE);
        labelsForRabbitMQReplicaSet.put(KEY_BACKOFF_AMOUNT, "1");
        labelsForRabbitMQReplicaSet.put(KEY_INTERVAL, "2");
        labelsForRabbitMQReplicaSet.put(KEY_MAX_INSTANCES, "3");
        labelsForRabbitMQReplicaSet.put(KEY_MIN_INSTANCES, "4");
        labelsForRabbitMQReplicaSet.put(KEY_SCALE_DOWN_BACKOFF_AMOUNT, "5");
        labelsForRabbitMQReplicaSet.put(KEY_SCALE_UP_BACKOFF_AMOUNT, "6");
        labelsForRabbitMQReplicaSet.put(KEY_SCALING_TARGET, "7");
        when(mockV1ObjectMeta.getLabels()).thenReturn(labelsForRabbitMQReplicaSet);
        when(mockV1ObjectMeta.getName()).thenReturn(REPLICA_SET_NAME);
        when(mockReplicaSetForRabbitMQ.getMetadata()).thenReturn(mockV1ObjectMeta);


        final V1ReplicaSet mockReplicaSet_NOT_FOR_RABBITMQ = Mockito.mock(V1ReplicaSet.class);
        final V1ObjectMeta mockV1ObjectMeta_NOT_FOR_RABBITMQ = Mockito.mock(V1ObjectMeta.class);
        final Map<String, String> labelsMQReplicaSet_NOT_FOR_RABBITMQ = new HashMap<>();
        labelsMQReplicaSet_NOT_FOR_RABBITMQ.put(KEY_WORKLOAD_METRIC, "SomethingElse");
        when(mockV1ObjectMeta_NOT_FOR_RABBITMQ.getName()).thenReturn("SomethingElse");
        when(mockV1ObjectMeta_NOT_FOR_RABBITMQ.getLabels()).thenReturn(labelsMQReplicaSet_NOT_FOR_RABBITMQ);
        when(mockReplicaSet_NOT_FOR_RABBITMQ.getMetadata()).thenReturn(mockV1ObjectMeta_NOT_FOR_RABBITMQ);     

        //  This will contain 2 items, only one of which is labeled for rabbit mq scaling.
        when(mockReplicaSetList.getItems()).thenReturn(Arrays.asList(mockReplicaSetForRabbitMQ, mockReplicaSet_NOT_FOR_RABBITMQ));
        when(mockApi.listNamespacedReplicaSet(
            any(), any(), any(), any(), any(), any(), any(), any(), any(), any())
        ).thenReturn(mockReplicaSetList);
        source = new K8sServiceSource(mockApi, new K8sAutoscaleConfiguration());
    }
    
    @Test
    public void ctorTest() throws ScalerException
    { 
        final Set<ScalingConfiguration> scSet = source.getServices(); 
        assertNotNull("K8sServiceSource should not be null", scSet);
        assertEquals("There should only be one ScalingConfiguration", 1, scSet.size());
        final ScalingConfiguration sc = scSet.iterator().next();
        errorCollector.checkThat("Workload metric should be " + METRIC, sc.getWorkloadMetric(), CoreMatchers.equalTo(METRIC));
        errorCollector.checkThat("Scaling profile should be " + PROFILE, sc.getScalingProfile(), CoreMatchers.equalTo(PROFILE));
        errorCollector.checkThat("Id should be the name of the replica set", sc.getId(), CoreMatchers.equalTo(REPLICA_SET_NAME));
        errorCollector.checkThat("Backoff amount should be 1", sc.getBackoffAmount(), CoreMatchers.equalTo(1));
        errorCollector.checkThat("Interval should be 2", sc.getInterval(), CoreMatchers.equalTo(2));
        errorCollector.checkThat("Max Instances should be 3", sc.getMaxInstances(), CoreMatchers.equalTo(3));
        errorCollector.checkThat("Min Instances should be 4", sc.getMinInstances(), CoreMatchers.equalTo(4));
        errorCollector.checkThat("Scale down backoff should be 5", sc.getScaleDownBackoffAmount(), CoreMatchers.equalTo(5));
        errorCollector.checkThat("Scale ip backoff should be 6", sc.getScaleUpBackoffAmount(), CoreMatchers.equalTo(6));
        errorCollector.checkThat("Scaling target should be 7", sc.getScalingTarget(), CoreMatchers.equalTo("7"));
    }
    
    @Test
    public void healthCheckTest() {
        assertEquals(HealthResult.RESULT_HEALTHY, source.healthCheck());
    }
}
