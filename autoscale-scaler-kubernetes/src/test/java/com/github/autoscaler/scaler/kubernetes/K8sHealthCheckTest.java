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

import com.github.autoscaler.kubernetes.shared.K8sAutoscaleConfiguration;
import com.hpe.caf.api.HealthResult;
import com.hpe.caf.api.HealthStatus;
import io.kubernetes.client.extended.kubectl.Kubectl;
import io.kubernetes.client.extended.kubectl.KubectlVersion;
import io.kubernetes.client.extended.kubectl.exception.KubectlException;
import io.kubernetes.client.openapi.apis.AuthorizationV1Api;
import io.kubernetes.client.openapi.models.*;
import org.apache.commons.lang3.StringUtils;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.MockedConstruction;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;

import java.util.Collections;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class K8sHealthCheckTest {

    @Test
    public void testHealthCheck_ReturnHealthy() throws Exception {

        final V1SubjectAccessReviewStatus status = new V1SubjectAccessReviewStatus();
        status.setAllowed(true);
        final V1SelfSubjectAccessReview review = new V1SelfSubjectAccessReview();
        review.setStatus(status);

        final AuthorizationV1Api.APIcreateSelfSubjectAccessReviewRequest requestBuilder
                = Mockito.mock(AuthorizationV1Api.APIcreateSelfSubjectAccessReviewRequest.class);

        try (MockedConstruction<V1SelfSubjectAccessReview> mockV1SelfSubjectAccessReview =
                     Mockito.mockConstruction(V1SelfSubjectAccessReview.class);
             MockedConstruction<AuthorizationV1Api> mockAuthorizationV1Api =
                     Mockito.mockConstruction(AuthorizationV1Api.class, (mock, context) ->{
                         when(mock.createSelfSubjectAccessReview(any())).thenReturn(requestBuilder);
                         when(mock.createSelfSubjectAccessReview(any()).dryRun(anyString())).thenReturn(requestBuilder);
                         when(mock.createSelfSubjectAccessReview(any()).fieldManager(any())).thenReturn(requestBuilder);
                         when(mock.createSelfSubjectAccessReview(any()).fieldValidation(any())).thenReturn(requestBuilder);
                         when(mock.createSelfSubjectAccessReview(any()).pretty(any())).thenReturn(requestBuilder);
                         when(mock.createSelfSubjectAccessReview(any()).execute()).thenReturn(review);
                     });
             MockedStatic<Kubectl> mockKubectl = Mockito.mockStatic(Kubectl.class)
             ) {
            final KubectlVersion version = Mockito.mock(KubectlVersion.class);
            final VersionInfo info = Mockito.mock(VersionInfo.class);
            when(Kubectl.version()).thenReturn(version);
            when(Kubectl.version().execute()).thenReturn(info);

            final K8sAutoscaleConfiguration config = Mockito.mock(K8sAutoscaleConfiguration.class);
            final List<String> mockNamespaces = Collections.singletonList("private");
            when(config.getNamespacesArray()).thenReturn(mockNamespaces);
            final K8sServiceScaler serviceScaler = new K8sServiceScaler(config);

            assertEquals(HealthResult.RESULT_HEALTHY, serviceScaler.healthCheck());
        }
    }

    @Test
    public void testHealthCheck_ReturnUnhealthyConnectionError() throws Exception {

        try(MockedStatic<Kubectl> mockKubectl = Mockito.mockStatic(Kubectl.class)){
            final KubectlVersion version = Mockito.mock(KubectlVersion.class);
            when(Kubectl.version()).thenReturn(version);
            when(Kubectl.version().execute()).thenThrow(new KubectlException("Error connecting to Kubernetes"));

            final K8sAutoscaleConfiguration config = Mockito.mock(K8sAutoscaleConfiguration.class);
            final List<String> mockNamespaces = Collections.singletonList("private");
            final K8sServiceScaler serviceScaler = new K8sServiceScaler(config);

            final HealthResult expectedResult = new HealthResult(HealthStatus.UNHEALTHY,
                    "Cannot connect to Kubernetes");
            final HealthResult actualResult = serviceScaler.healthCheck();

            assertEquals(expectedResult.getStatus(), actualResult.getStatus());
            assertEquals(expectedResult.getMessage(), actualResult.getMessage());
        }
    }

    @Test
    public void testHealthCheck_ReturnUnhealthyPermissionError() throws Exception {

        final V1SubjectAccessReviewStatus status = new V1SubjectAccessReviewStatus();
        status.setAllowed(false);
        final V1SelfSubjectAccessReview review = new V1SelfSubjectAccessReview();
        review.setStatus(status);

        final AuthorizationV1Api.APIcreateSelfSubjectAccessReviewRequest requestBuilder
                = Mockito.mock(AuthorizationV1Api.APIcreateSelfSubjectAccessReviewRequest.class);

        try (MockedConstruction<V1SelfSubjectAccessReview> mockV1SelfSubjectAccessReview =
                     Mockito.mockConstruction(V1SelfSubjectAccessReview.class);
             MockedConstruction<AuthorizationV1Api> mockAuthorizationV1Api =
                     Mockito.mockConstruction(AuthorizationV1Api.class, (mock, context) ->{
                         when(mock.createSelfSubjectAccessReview(any())).thenReturn(requestBuilder);
                         when(mock.createSelfSubjectAccessReview(any()).dryRun(anyString())).thenReturn(requestBuilder);
                         when(mock.createSelfSubjectAccessReview(any()).fieldManager(any())).thenReturn(requestBuilder);
                         when(mock.createSelfSubjectAccessReview(any()).fieldValidation(any())).thenReturn(requestBuilder);
                         when(mock.createSelfSubjectAccessReview(any()).pretty(any())).thenReturn(requestBuilder);
                         when(mock.createSelfSubjectAccessReview(any()).execute()).thenReturn(review);
                     });
             MockedStatic<Kubectl> mockKubectl = Mockito.mockStatic(Kubectl.class)
        ) {
            final KubectlVersion version = Mockito.mock(KubectlVersion.class);
            final VersionInfo info = Mockito.mock(VersionInfo.class);
            when(Kubectl.version()).thenReturn(version);
            when(Kubectl.version().execute()).thenReturn(info);

            final K8sAutoscaleConfiguration config = Mockito.mock(K8sAutoscaleConfiguration.class);
            final List<String> mockNamespaces = Collections.singletonList("private");
            when(config.getNamespacesArray()).thenReturn(mockNamespaces);
            final K8sServiceScaler serviceScaler = new K8sServiceScaler(config);

            final String expectedMessage = String.format("Error: Kubernetes Service Account does not have correct permissions: %s", StringUtils.normalizeSpace(review.toString()));

            final HealthResult expectedResult = new HealthResult(HealthStatus.UNHEALTHY, expectedMessage);
            final HealthResult actualResult = serviceScaler.healthCheck();

            assertEquals(expectedResult.getStatus(), actualResult.getStatus());
            assertEquals(expectedResult.getMessage(), actualResult.getMessage());
        }
    }

    @Test
    public void testHealthCheck_ReturnNoNamespacesError() throws Exception
    {
        try(MockedStatic<Kubectl> mockKubectl = Mockito.mockStatic(Kubectl.class)) {
            final KubectlVersion version = Mockito.mock(KubectlVersion.class);
            final VersionInfo info = Mockito.mock(VersionInfo.class);
            when(Kubectl.version()).thenReturn(version);
            when(Kubectl.version().execute()).thenReturn(info);

            final K8sAutoscaleConfiguration config = Mockito.mock(K8sAutoscaleConfiguration.class);
            final List<String> mockNamespaces = Collections.emptyList();
            when(config.getNamespacesArray()).thenReturn(mockNamespaces);
            final K8sServiceScaler serviceScaler = new K8sServiceScaler(config);

            final HealthResult expectedResult = new HealthResult(HealthStatus.UNHEALTHY, "Error: No namespaces were found");
            final HealthResult actualResult = serviceScaler.healthCheck();

            assertEquals(expectedResult.getStatus(), actualResult.getStatus());
            assertEquals(expectedResult.getMessage(), actualResult.getMessage());
        }
    }
}
