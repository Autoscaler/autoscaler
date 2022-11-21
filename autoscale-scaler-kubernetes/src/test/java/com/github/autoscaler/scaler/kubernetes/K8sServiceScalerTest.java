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
