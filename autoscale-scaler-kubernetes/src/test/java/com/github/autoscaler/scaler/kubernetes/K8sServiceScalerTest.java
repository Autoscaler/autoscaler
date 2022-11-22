/*
 * Copyright 2015-2022 Micro Focus or one of its affiliates.
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

import com.hpe.caf.api.HealthResult;
import com.hpe.caf.api.HealthStatus;
import org.junit.Test;
import org.mockito.Mockito;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.when;

public class K8sServiceScalerTest {

    @Test
    public void testHealthCheck_ReturnHealthy()
    {
        K8sServiceScaler k8sServiceScaler = Mockito.mock(K8sServiceScaler.class);
        when(k8sServiceScaler.connectionHealthCheck()).thenReturn(HealthResult.RESULT_HEALTHY);
        when(k8sServiceScaler.permissionsHealthCheck()).thenReturn(HealthResult.RESULT_HEALTHY);
        when(k8sServiceScaler.healthCheck()).thenCallRealMethod();

        assertEquals(HealthResult.RESULT_HEALTHY, k8sServiceScaler.healthCheck());
    }

    @Test
    public void testHealthCheck_ReturnUnhealthyConnectionError()
    {
        K8sServiceScaler k8sServiceScaler = Mockito.mock(K8sServiceScaler.class);
        when(k8sServiceScaler.connectionHealthCheck()).thenReturn(new HealthResult(HealthStatus.UNHEALTHY, "Cannot connect to Kubernetes"));
        when(k8sServiceScaler.permissionsHealthCheck()).thenReturn(HealthResult.RESULT_HEALTHY);
        when(k8sServiceScaler.healthCheck()).thenCallRealMethod();

        HealthResult expectedResult = new HealthResult(HealthStatus.UNHEALTHY, "Cannot connect to Kubernetes");
        HealthResult actualResult = k8sServiceScaler.healthCheck();

        assertEquals(expectedResult.getStatus(), actualResult.getStatus());
        assertEquals(expectedResult.getMessage(), actualResult.getMessage());
    }

    @Test
    public void testHealthCheck_ReturnUnhealthyPermissionError()
    {
        K8sServiceScaler k8sServiceScaler = Mockito.mock(K8sServiceScaler.class);
        when(k8sServiceScaler.connectionHealthCheck()).thenReturn(HealthResult.RESULT_HEALTHY);
        when(k8sServiceScaler.permissionsHealthCheck())
                .thenReturn(new HealthResult(HealthStatus.UNHEALTHY, "Error: Kubernetes Service Account does not have correct permissions"));
        when(k8sServiceScaler.healthCheck()).thenCallRealMethod();

        HealthResult expectedResult = new HealthResult(HealthStatus.UNHEALTHY,
                "Error: Kubernetes Service Account does not have correct permissions");
        HealthResult actualResult = k8sServiceScaler.healthCheck();

        assertEquals(expectedResult.getStatus(), actualResult.getStatus());
        assertEquals(expectedResult.getMessage(), actualResult.getMessage());
    }
}
