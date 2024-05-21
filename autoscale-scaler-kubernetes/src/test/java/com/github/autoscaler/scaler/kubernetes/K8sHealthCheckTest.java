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
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;

import java.util.Collections;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.lenient;

@RunWith(MockitoJUnitRunner.class)
public class K8sHealthCheckTest {

    @Test
    public void testHealthCheck_ReturnHealthy() throws Exception {
        final KubectlVersion version = Mockito.mock(KubectlVersion.class);
        final VersionInfo info = Mockito.mock(VersionInfo.class);
        final MockedStatic<Kubectl> kubectlMockedStatic = Mockito.mockStatic(Kubectl.class);
        when(Kubectl.version()).thenReturn(version);
        when(Kubectl.version().execute()).thenReturn(info);

        final V1SubjectAccessReviewStatus status = new V1SubjectAccessReviewStatus();
        status.setAllowed(true);
        final V1SelfSubjectAccessReview review = new V1SelfSubjectAccessReview();
        review.setStatus(status);

        final V1SelfSubjectAccessReview body = Mockito.mock(V1SelfSubjectAccessReview.class);
        final MockedStatic<V1SelfSubjectAccessReviewFactory> v1SelfSubjectAccessReviewFactoryMockedStatic =
                Mockito.mockStatic(V1SelfSubjectAccessReviewFactory.class);
        when(V1SelfSubjectAccessReviewFactory.createSelfSubjectAccessReview()).thenReturn(body);

        final AuthorizationV1Api authApi = Mockito.mock(AuthorizationV1Api.class);
        final MockedStatic<AuthorizationV1ApiFactory> authorizationV1ApiFactoryMockedStatic = Mockito.mockStatic(AuthorizationV1ApiFactory.class);
        when(AuthorizationV1ApiFactory.createAuthorizationV1Api()).thenReturn(authApi);
        final AuthorizationV1Api.APIcreateSelfSubjectAccessReviewRequest requestBuilder
            = Mockito.mock(AuthorizationV1Api.APIcreateSelfSubjectAccessReviewRequest.class);
        when(authApi.createSelfSubjectAccessReview(body)).thenReturn(requestBuilder);
        when(requestBuilder.dryRun("All")).thenReturn(requestBuilder);
        when(requestBuilder.fieldManager(null)).thenReturn(requestBuilder);
        when(requestBuilder.fieldValidation(null)).thenReturn(requestBuilder);
        when(requestBuilder.pretty("true")).thenReturn(requestBuilder);
        when(requestBuilder.execute()).thenReturn(review);

        final K8sAutoscaleConfiguration config = Mockito.mock(K8sAutoscaleConfiguration.class);
        final List<String> mockNamespaces = Collections.singletonList("private");
        when(config.getNamespacesArray()).thenReturn(mockNamespaces);
        final K8sServiceScaler serviceScaler = new K8sServiceScaler(config);

        assertEquals(HealthResult.RESULT_HEALTHY, serviceScaler.healthCheck());

        kubectlMockedStatic.close();
        authorizationV1ApiFactoryMockedStatic.close();
        v1SelfSubjectAccessReviewFactoryMockedStatic.close();
    }

    @Test
    public void testHealthCheck_ReturnUnhealthyConnectionError() throws Exception {
        final KubectlVersion version = Mockito.mock(KubectlVersion.class);
        final MockedStatic<Kubectl> kubectlMockedStatic = Mockito.mockStatic(Kubectl.class);
        lenient().when(Kubectl.version()).thenReturn(version);
        lenient().when(Kubectl.version().execute()).thenThrow(new KubectlException("Error connecting to Kubernetes"));

        final K8sAutoscaleConfiguration config = Mockito.mock(K8sAutoscaleConfiguration.class);
        final List<String> mockNamespaces = Collections.singletonList("private");
        lenient().when(config.getNamespacesArray()).thenReturn(mockNamespaces);
        final K8sServiceScaler serviceScaler = new K8sServiceScaler(config);

        final HealthResult expectedResult = new HealthResult(HealthStatus.UNHEALTHY,
                "Cannot connect to Kubernetes");
        final HealthResult actualResult = serviceScaler.healthCheck();

        assertEquals(expectedResult.getStatus(), actualResult.getStatus());
        assertEquals(expectedResult.getMessage(), actualResult.getMessage());
        kubectlMockedStatic.close();
    }

    @Test
    public void testHealthCheck_ReturnUnhealthyPermissionError() throws Exception {
        final KubectlVersion version = Mockito.mock(KubectlVersion.class);
        final VersionInfo info = Mockito.mock(VersionInfo.class);
        final MockedStatic<Kubectl> kubectlMockedStatic = Mockito.mockStatic(Kubectl.class);
        when(Kubectl.version()).thenReturn(version);
        when(Kubectl.version().execute()).thenReturn(info);

        final V1SubjectAccessReviewStatus status = new V1SubjectAccessReviewStatus();
        status.setAllowed(false);
        final V1SelfSubjectAccessReview review = new V1SelfSubjectAccessReview();
        review.setStatus(status);

        final V1SelfSubjectAccessReview body = Mockito.mock(V1SelfSubjectAccessReview.class);
        final MockedStatic<V1SelfSubjectAccessReviewFactory> v1SelfSubjectAccessReviewFactoryMockedStatic =
                Mockito.mockStatic(V1SelfSubjectAccessReviewFactory.class);
        when(V1SelfSubjectAccessReviewFactory.createSelfSubjectAccessReview()).thenReturn(body);

        final AuthorizationV1Api authApi = Mockito.mock(AuthorizationV1Api.class);
        final MockedStatic<AuthorizationV1ApiFactory> authorizationV1ApiFactoryMockedStatic =
                Mockito.mockStatic(AuthorizationV1ApiFactory.class);
        when(AuthorizationV1ApiFactory.createAuthorizationV1Api()).thenReturn(authApi);
        final AuthorizationV1Api.APIcreateSelfSubjectAccessReviewRequest requestBuilder
            = Mockito.mock(AuthorizationV1Api.APIcreateSelfSubjectAccessReviewRequest.class);
        when(authApi.createSelfSubjectAccessReview(body)).thenReturn(requestBuilder);
        when(requestBuilder.dryRun("All")).thenReturn(requestBuilder);
        when(requestBuilder.fieldManager(null)).thenReturn(requestBuilder);
        when(requestBuilder.fieldValidation(null)).thenReturn(requestBuilder);
        when(requestBuilder.pretty("true")).thenReturn(requestBuilder);
        when(requestBuilder.execute()).thenReturn(review);

        final K8sAutoscaleConfiguration config = Mockito.mock(K8sAutoscaleConfiguration.class);
        final List<String> mockNamespaces = Collections.singletonList("private");
        when(config.getNamespacesArray()).thenReturn(mockNamespaces);
        final K8sServiceScaler serviceScaler = new K8sServiceScaler(config);

        final String expectedMessage = String.format("Error: Kubernetes Service Account does not have correct permissions: %s", StringUtils.normalizeSpace(review.toString()));

        final HealthResult expectedResult = new HealthResult(HealthStatus.UNHEALTHY, expectedMessage);
        final HealthResult actualResult = serviceScaler.healthCheck();

        assertEquals(expectedResult.getStatus(), actualResult.getStatus());
        assertEquals(expectedResult.getMessage(), actualResult.getMessage());

        kubectlMockedStatic.close();
        v1SelfSubjectAccessReviewFactoryMockedStatic.close();
        authorizationV1ApiFactoryMockedStatic.close();
    }

    @Test
    public void testHealthCheck_ReturnNoNamespacesError() throws Exception
    {
        final KubectlVersion version = Mockito.mock(KubectlVersion.class);
        final VersionInfo info = Mockito.mock(VersionInfo.class);
        final MockedStatic<Kubectl> kubectlMockedStatic = Mockito.mockStatic(Kubectl.class);
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

        kubectlMockedStatic.close();
    }
}
