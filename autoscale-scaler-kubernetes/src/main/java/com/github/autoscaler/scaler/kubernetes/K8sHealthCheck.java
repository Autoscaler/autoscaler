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
import com.github.cafapi.kubernetes.client.model.IoK8sApiAuthorizationV1ResourceAttributes;
import com.github.cafapi.kubernetes.client.model.IoK8sApiAuthorizationV1SelfSubjectAccessReview;
import com.github.cafapi.kubernetes.client.model.IoK8sApiAuthorizationV1SelfSubjectAccessReviewSpec;
import com.hpe.caf.api.HealthResult;
import com.hpe.caf.api.HealthStatus;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

final class K8sHealthCheck
{
    private static final Logger LOG = LoggerFactory.getLogger(K8sHealthCheck.class);

    private K8sHealthCheck()
    {

    }

    public static HealthResult healthCheck(final K8sAutoscaleConfiguration config, final ApiClient apiClient)
    {
        final HealthResult connectionHealthResult = checkConnection(apiClient);
        if (connectionHealthResult == HealthResult.RESULT_HEALTHY) {
            return checkPermissions(config, apiClient);
        } else {
            return connectionHealthResult;
        }
    }

    private static HealthResult checkConnection(final ApiClient apiClient)
    {
        try {
            new VersionApi(apiClient).getCodeVersion().execute();
            return HealthResult.RESULT_HEALTHY;
        } catch (final ApiException e) {
            LOG.warn("Connection failure to kubernetes", e);
            return new HealthResult(HealthStatus.UNHEALTHY, "Cannot connect to Kubernetes");
        }
    }

    private static HealthResult checkPermissions(final K8sAutoscaleConfiguration config, final ApiClient apiClient)
    {
        final List<String> namespaces = config.getNamespacesArray();

        if(!namespaces.isEmpty()) {
            for (final String namespace: namespaces) {

                final IoK8sApiAuthorizationV1ResourceAttributes resourceAttributes = new IoK8sApiAuthorizationV1ResourceAttributes();
                resourceAttributes.setGroup("apps");
                resourceAttributes.setResource("deployments");
                resourceAttributes.setVerb("patch");
                resourceAttributes.setNamespace(namespace);

                final IoK8sApiAuthorizationV1SelfSubjectAccessReviewSpec spec = new IoK8sApiAuthorizationV1SelfSubjectAccessReviewSpec();
                spec.setResourceAttributes(resourceAttributes);

                final IoK8sApiAuthorizationV1SelfSubjectAccessReview body = new IoK8sApiAuthorizationV1SelfSubjectAccessReview();
                body.setApiVersion("authorization.k8s.io/v1");
                body.setKind("SelfSubjectAccessReview");
                body.setSpec(spec);

                final IoK8sApiAuthorizationV1SelfSubjectAccessReview review;
                try {
                    final AuthorizationV1Api.APIcreateAuthorizationV1SelfSubjectAccessReviewRequest request =
                            new AuthorizationV1Api(apiClient).createAuthorizationV1SelfSubjectAccessReview();

                    request.dryRun("All");
                    request.fieldManager(null);
                    request.fieldValidation(null);
                    request.pretty("true");
                    request.body(body);

                    review = request.execute();
                } catch (final ApiException e) {
                    throw new RuntimeException(e);
                }

                if (review.getStatus() == null || !review.getStatus().getAllowed()) {
                    final String errorMessage = String.format(
                            "Error: Kubernetes Service Account does not have correct permissions: %s",
                            StringUtils.normalizeSpace(review.toString()));
                    LOG.warn(errorMessage);
                    return new HealthResult(HealthStatus.UNHEALTHY, errorMessage);
                }
            }
            return HealthResult.RESULT_HEALTHY;
        }
        return new HealthResult(HealthStatus.UNHEALTHY, "Error: No namespaces were found");
    }
}
