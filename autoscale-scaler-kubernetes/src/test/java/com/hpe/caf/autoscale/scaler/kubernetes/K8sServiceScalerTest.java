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
import com.hpe.caf.autoscale.K8sAutoscaleConfiguration;
import io.kubernetes.client.openapi.ApiException;
import io.kubernetes.client.openapi.apis.AppsV1Api;
import io.kubernetes.client.openapi.models.V1ObjectMeta;
import io.kubernetes.client.openapi.models.V1Scale;
import io.kubernetes.client.openapi.models.V1ScaleSpec;
import static junit.framework.TestCase.assertNotNull;
import static junit.framework.TestCase.fail;
import org.hamcrest.CoreMatchers;
import static org.junit.Assert.assertEquals;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ErrorCollector;
import static org.mockito.ArgumentMatchers.any;
import org.mockito.Mockito;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.HashMap;

public class K8sServiceScalerTest
{   
    private K8sServiceScaler serviceScaler;
    private AppsV1Api mockApi;
    private V1ScaleSpec mockScaleSpec;

    @Rule
    public ErrorCollector errorCollector = new ErrorCollector();
    
    @Before
    public void setup() throws ApiException
    {
        final K8sAutoscaleConfiguration k8sAutoscaleConfiguration = new K8sAutoscaleConfiguration();
        k8sAutoscaleConfiguration.setNamespace("namespace");
        k8sAutoscaleConfiguration.setMaximumInstances(10);
        mockApi = Mockito.mock(AppsV1Api.class);
        final V1Scale mockScale = Mockito.mock(V1Scale.class);
        final V1ObjectMeta mockMetadata = Mockito.mock(V1ObjectMeta.class);
        mockScaleSpec = Mockito.mock(V1ScaleSpec.class);
        when(mockScale.getSpec()).thenReturn(mockScaleSpec);
        when(mockMetadata.getLabels()).thenReturn(new HashMap<>());
        when(mockScale.getMetadata()).thenReturn(mockMetadata);
        when(mockScaleSpec.getReplicas()).thenReturn(1);
        when(mockApi.readNamespacedDeploymentScale(any(), any(), any())).thenReturn(mockScale);
        when(mockApi.replaceNamespacedDeploymentScale(any(), any(), any(), any(), any(), any())).thenReturn(null);
        serviceScaler = new K8sServiceScaler(mockApi, k8sAutoscaleConfiguration);
    }
    
    @Test
    public void scalarCtorTest() {
        assertNotNull(serviceScaler);
    }
    
    @Test
    public void scaleUpTest() {
        try {
            serviceScaler.scaleUp("placeHolder", 1);
            verify(mockApi, times(1)).replaceNamespacedDeploymentScale(any(), any(), any(), any(), any(), any());
        } catch (ScalerException | ApiException e) {
            fail("Should not have thrown exception scaling up");
        }
    }

    @Test
    public void scaleUpBeyondMaxInstancesShouldNotHappenTest() {
        try {
            when(mockScaleSpec.getReplicas()).thenReturn(10);
            serviceScaler.scaleUp("placeHolder", 1);
            verify(mockApi, times(0)).replaceNamespacedDeploymentScale(any(), any(), any(), any(), any(), any());
        } catch (ScalerException | ApiException e) {
            fail("Should not have thrown exception scaling up");
        }
    }

    @Test
    public void scaleDownTest() {
        try {
            serviceScaler.scaleDown("placeHolder", 1);
            verify(mockApi, times(1)).replaceNamespacedDeploymentScale(any(), any(), any(), any(), any(), any());
        } catch (ScalerException | ApiException e) {
            fail("Should not have thrown exception scaling down");
        }
    }

    @Test
    public void scaleDownWhenNoReplicasExistShouldNotHappenTest() {
        try {
            when(mockScaleSpec.getReplicas()).thenReturn(0);
            serviceScaler.scaleDown("placeHolder", 1);
            verify(mockApi, times(0)).replaceNamespacedDeploymentScale(any(), any(), any(), any(), any(), any());
        } catch (ScalerException | ApiException e) {
            fail("Should not have thrown exception scaling down");
        }
    }
    
    @Test
    public void instanceInfoTest() throws ScalerException
    {
        final InstanceInfo instanceInfo = serviceScaler.getInstanceInfo("placeholder");
        assertNotNull(instanceInfo);
        errorCollector.checkThat("Replica count should be 1", instanceInfo.getInstances(), CoreMatchers.equalTo(1));
        errorCollector.checkThat("Running instances should be 1", instanceInfo.getInstancesRunning(), CoreMatchers.equalTo(1));
        errorCollector.checkThat("Staging instances should be 0", instanceInfo.getInstancesStaging(), CoreMatchers.equalTo(0));
        errorCollector.checkThat("Shutdown priority should be -1", instanceInfo.getShutdownPriority(), CoreMatchers.equalTo(-1));
        errorCollector.checkThat("Running and staging should be 1", instanceInfo.getTotalRunningAndStageInstances(), CoreMatchers.equalTo(1));
        errorCollector.checkThat("Hosts should be empty", instanceInfo.getHosts().size(), CoreMatchers.equalTo(0));
        
    }

    @Test
    public void healthCheckTest() {
        assertEquals(HealthResult.RESULT_HEALTHY, serviceScaler.healthCheck());
    }
}
