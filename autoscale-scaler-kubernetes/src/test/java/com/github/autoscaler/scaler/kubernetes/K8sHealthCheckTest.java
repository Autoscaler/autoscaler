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

import com.github.autoscaler.kubernetes.shared.K8sAutoscaleConfiguration;
import com.hpe.caf.api.HealthResult;
import com.hpe.caf.api.HealthStatus;
import io.kubernetes.client.extended.kubectl.Kubectl;
import io.kubernetes.client.extended.kubectl.KubectlVersion;
import io.kubernetes.client.extended.kubectl.exception.KubectlException;
import io.kubernetes.client.openapi.apis.AuthorizationV1Api;
import io.kubernetes.client.openapi.models.*;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.when;

@RunWith(PowerMockRunner.class)
@PrepareForTest({ Kubectl.class, K8sHealthCheck.class })
@PowerMockIgnore({"jdk.internal.reflect.*", "javax.net.ssl.*"})
public class K8sHealthCheckTest {

    @Test
    public void testHealthCheck_ReturnHealthy() throws Exception {
        KubectlVersion version = Mockito.mock(KubectlVersion.class);
        VersionInfo info = Mockito.mock(VersionInfo.class);
        PowerMockito.mockStatic(Kubectl.class);
        when(Kubectl.version()).thenReturn(version);
        when(Kubectl.version().execute()).thenReturn(info);

        V1SubjectAccessReviewStatus status = new V1SubjectAccessReviewStatus();
        status.setAllowed(true);
        V1SelfSubjectAccessReview review = new V1SelfSubjectAccessReview();
        review.setStatus(status);

        V1SelfSubjectAccessReview body = PowerMockito.mock(V1SelfSubjectAccessReview.class);
        PowerMockito.whenNew(V1SelfSubjectAccessReview.class)
                .withNoArguments().thenReturn(body);

        AuthorizationV1Api authApi = PowerMockito.mock(AuthorizationV1Api.class);
        PowerMockito.whenNew(AuthorizationV1Api.class)
                .withNoArguments().thenReturn(authApi);
        when(authApi.createSelfSubjectAccessReview(body, "All", "fas", "true")).thenReturn(review);

        K8sAutoscaleConfiguration config = Mockito.mock(K8sAutoscaleConfiguration.class);
        K8sServiceScaler serviceScaler = new K8sServiceScaler(config);

        assertEquals(HealthResult.RESULT_HEALTHY, serviceScaler.healthCheck());
    }

    @Test
    public void testHealthCheck_ReturnUnhealthyConnectionError() throws Exception {
        KubectlVersion version = Mockito.mock(KubectlVersion.class);
        PowerMockito.mockStatic(Kubectl.class);
        when(Kubectl.version()).thenReturn(version);
        when(Kubectl.version().execute()).thenThrow(new KubectlException("Error connecting to Kubernetes"));

        V1SubjectAccessReviewStatus status = new V1SubjectAccessReviewStatus();
        status.setAllowed(true);
        V1SelfSubjectAccessReview review = new V1SelfSubjectAccessReview();
        review.setStatus(status);

        V1SelfSubjectAccessReview body = PowerMockito.mock(V1SelfSubjectAccessReview.class);
        PowerMockito.whenNew(V1SelfSubjectAccessReview.class)
                .withNoArguments().thenReturn(body);

        AuthorizationV1Api authApi = PowerMockito.mock(AuthorizationV1Api.class);
        PowerMockito.whenNew(AuthorizationV1Api.class)
                .withNoArguments().thenReturn(authApi);
        when(authApi.createSelfSubjectAccessReview(body, "All", "fas", "true")).thenReturn(review);

        K8sAutoscaleConfiguration config = Mockito.mock(K8sAutoscaleConfiguration.class);
        K8sServiceScaler serviceScaler = new K8sServiceScaler(config);

        HealthResult expectedResult = new HealthResult(HealthStatus.UNHEALTHY,
                "Cannot connect to Kubernetes");
        HealthResult actualResult = serviceScaler.healthCheck();

        assertEquals(expectedResult.getStatus(), actualResult.getStatus());
        assertEquals(expectedResult.getMessage(), actualResult.getMessage());
    }

    @Test
    public void testHealthCheck_ReturnUnhealthyPermissionError() throws Exception {
        KubectlVersion version = Mockito.mock(KubectlVersion.class);
        VersionInfo info = Mockito.mock(VersionInfo.class);
        PowerMockito.mockStatic(Kubectl.class);
        when(Kubectl.version()).thenReturn(version);
        when(Kubectl.version().execute()).thenReturn(info);

        V1SubjectAccessReviewStatus status = new V1SubjectAccessReviewStatus();
        status.setAllowed(false);
        V1SelfSubjectAccessReview review = new V1SelfSubjectAccessReview();
        review.setStatus(status);

        V1SelfSubjectAccessReview body = PowerMockito.mock(V1SelfSubjectAccessReview.class);
        PowerMockito.whenNew(V1SelfSubjectAccessReview.class)
                .withNoArguments().thenReturn(body);

        AuthorizationV1Api authApi = PowerMockito.mock(AuthorizationV1Api.class);
        PowerMockito.whenNew(AuthorizationV1Api.class)
                .withNoArguments().thenReturn(authApi);
        when(authApi.createSelfSubjectAccessReview(body, "All", "fas", "true")).thenReturn(review);

        K8sAutoscaleConfiguration config = Mockito.mock(K8sAutoscaleConfiguration.class);
        K8sServiceScaler serviceScaler = new K8sServiceScaler(config);

        String expectedMessage = String.format("Error: Kubernetes Service Account does not have correct permissions: %s", review);

        HealthResult expectedResult = new HealthResult(HealthStatus.UNHEALTHY, expectedMessage);
        HealthResult actualResult = serviceScaler.healthCheck();

        assertEquals(expectedResult.getStatus(), actualResult.getStatus());
        assertEquals(expectedResult.getMessage(), actualResult.getMessage());
    }
}
