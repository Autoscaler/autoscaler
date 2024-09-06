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
import com.github.cafapi.kubernetes.client.api.AuthorizationV1Api;
import com.github.cafapi.kubernetes.client.api.VersionApi;
import com.github.cafapi.kubernetes.client.client.ApiClient;
import com.github.cafapi.kubernetes.client.client.ApiException;
import com.github.cafapi.kubernetes.client.model.IoK8sApiAuthorizationV1SelfSubjectAccessReview;
import com.github.cafapi.kubernetes.client.model.IoK8sApiAuthorizationV1SubjectAccessReviewStatus;
import com.hpe.caf.api.HealthResult;
import com.hpe.caf.api.HealthStatus;
import org.apache.commons.lang3.StringUtils;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.MockedConstruction;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class K8sHealthCheckTest {

    @Test
    public void testHealthCheck_ReturnHealthy() {
        final IoK8sApiAuthorizationV1SubjectAccessReviewStatus status = new IoK8sApiAuthorizationV1SubjectAccessReviewStatus();
        status.setAllowed(true);
        final IoK8sApiAuthorizationV1SelfSubjectAccessReview review = new IoK8sApiAuthorizationV1SelfSubjectAccessReview();
        review.setStatus(status);

        final AuthorizationV1Api.APIcreateAuthorizationV1SelfSubjectAccessReviewRequest requestMock =
                mock(AuthorizationV1Api.APIcreateAuthorizationV1SelfSubjectAccessReviewRequest.class);

        try (MockedConstruction<VersionApi> versionApiMock = Mockito.mockConstruction(VersionApi.class,
                (mock, context) -> when(mock.getCodeVersion()).thenReturn(mock(VersionApi.APIgetCodeVersionRequest.class)));
             MockedConstruction<AuthorizationV1Api> authorizationV1ApiMock = Mockito.mockConstruction(AuthorizationV1Api.class,
                     (mock, context) -> {
                         when(mock.createAuthorizationV1SelfSubjectAccessReview()).thenReturn(requestMock);
                         when(requestMock.dryRun(anyString())).thenReturn(requestMock);
                         when(requestMock.fieldManager(any())).thenReturn(requestMock);
                         when(requestMock.fieldValidation(any())).thenReturn(requestMock);
                         when(requestMock.pretty(any())).thenReturn(requestMock);
                         when(requestMock.execute()).thenReturn(review);
                     })
        ) {
            final K8sAutoscaleConfiguration config = mock(K8sAutoscaleConfiguration.class);
            final List<String> mockNamespaces = Collections.singletonList("private");
            when(config.getNamespacesArray()).thenReturn(mockNamespaces);

            final ApiClient apiClientMock = mock(ApiClient.class);

            assertEquals(HealthResult.RESULT_HEALTHY, K8sHealthCheck.healthCheck(config, apiClientMock));
        }
    }

    @Test
    public void testHealthCheck_ReturnUnhealthyConnectionError() {
        try (MockedConstruction<VersionApi> versionApiMock = Mockito.mockConstruction(VersionApi.class,
                (mock, context) -> {
                    VersionApi.APIgetCodeVersionRequest requestMock = mock(VersionApi.APIgetCodeVersionRequest.class);
                    when(mock.getCodeVersion()).thenReturn(requestMock);
                    when(requestMock.execute()).thenThrow(new ApiException("Error connecting to Kubernetes"));
                })
        ) {
            final K8sAutoscaleConfiguration config = mock(K8sAutoscaleConfiguration.class);
            final ApiClient apiClientMock = mock(ApiClient.class);

            final HealthResult expectedResult = new HealthResult(HealthStatus.UNHEALTHY,
                    "Cannot connect to Kubernetes");
            final HealthResult actualResult = K8sHealthCheck.healthCheck(config, apiClientMock);

            assertEquals(expectedResult.getStatus(), actualResult.getStatus());
            assertEquals(expectedResult.getMessage(), actualResult.getMessage());
        }
    }

    @Test
    public void testHealthCheck_ReturnUnhealthyPermissionError() {
        final IoK8sApiAuthorizationV1SubjectAccessReviewStatus status = new IoK8sApiAuthorizationV1SubjectAccessReviewStatus();
        status.setAllowed(false);
        final IoK8sApiAuthorizationV1SelfSubjectAccessReview review = new IoK8sApiAuthorizationV1SelfSubjectAccessReview();
        review.setStatus(status);

        final AuthorizationV1Api.APIcreateAuthorizationV1SelfSubjectAccessReviewRequest requestMock =
                mock(AuthorizationV1Api.APIcreateAuthorizationV1SelfSubjectAccessReviewRequest.class);

        try (MockedConstruction<VersionApi> versionApiMock = Mockito.mockConstruction(VersionApi.class,
                (mock, context) -> when(mock.getCodeVersion()).thenReturn(mock(VersionApi.APIgetCodeVersionRequest.class)));
             MockedConstruction<AuthorizationV1Api> authorizationV1ApiMock = Mockito.mockConstruction(AuthorizationV1Api.class,
                     (mock, context) -> {
                         when(mock.createAuthorizationV1SelfSubjectAccessReview()).thenReturn(requestMock);
                         when(requestMock.dryRun(anyString())).thenReturn(requestMock);
                         when(requestMock.fieldManager(any())).thenReturn(requestMock);
                         when(requestMock.fieldValidation(any())).thenReturn(requestMock);
                         when(requestMock.pretty(any())).thenReturn(requestMock);
                         when(requestMock.execute()).thenReturn(review);
                     })
        ) {
            final K8sAutoscaleConfiguration config = mock(K8sAutoscaleConfiguration.class);
            final List<String> mockNamespaces = Collections.singletonList("private");
            when(config.getNamespacesArray()).thenReturn(mockNamespaces);

            final ApiClient apiClientMock = mock(ApiClient.class);

            final String expectedMessage = String.format("Error: Kubernetes Service Account does not have correct permissions: %s",
                    StringUtils.normalizeSpace(review.toString()));

            final HealthResult expectedResult = new HealthResult(HealthStatus.UNHEALTHY, expectedMessage);
            final HealthResult actualResult = K8sHealthCheck.healthCheck(config, apiClientMock);

            assertEquals(expectedResult.getStatus(), actualResult.getStatus());
            assertEquals(expectedResult.getMessage(), actualResult.getMessage());
        }
    }

    @Test
    public void testHealthCheck_ReturnNoNamespacesError() {
        try (MockedConstruction<VersionApi> versionApiMock = Mockito.mockConstruction(VersionApi.class,
                (mock, context) -> when(mock.getCodeVersion()).thenReturn(mock(VersionApi.APIgetCodeVersionRequest.class)))
        ) {
            final K8sAutoscaleConfiguration config = mock(K8sAutoscaleConfiguration.class);
            final List<String> mockNamespaces = Collections.emptyList();
            when(config.getNamespacesArray()).thenReturn(mockNamespaces);

            final ApiClient apiClientMock = mock(ApiClient.class);

            final HealthResult expectedResult = new HealthResult(HealthStatus.UNHEALTHY, "Error: No namespaces were found");
            final HealthResult actualResult = K8sHealthCheck.healthCheck(config, apiClientMock);

            assertEquals(expectedResult.getStatus(), actualResult.getStatus());
            assertEquals(expectedResult.getMessage(), actualResult.getMessage());
        }
    }
}
