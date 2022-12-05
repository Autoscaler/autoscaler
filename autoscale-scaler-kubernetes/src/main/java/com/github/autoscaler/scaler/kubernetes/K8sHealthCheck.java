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
import io.kubernetes.client.extended.kubectl.exception.KubectlException;
import io.kubernetes.client.openapi.ApiException;
import io.kubernetes.client.openapi.apis.AuthorizationV1Api;
import io.kubernetes.client.openapi.models.V1ResourceAttributes;
import io.kubernetes.client.openapi.models.V1SelfSubjectAccessReviewSpec;
import io.kubernetes.client.openapi.models.V1SelfSubjectAccessReview;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

final class K8sHealthCheck
{
    private static final Logger LOG = LoggerFactory.getLogger(K8sHealthCheck.class);

    private K8sHealthCheck()
    {

    }

    public static HealthResult healthCheck(final K8sAutoscaleConfiguration config)
    {
        final HealthResult connectionHealthResult = checkConnection();
        if (connectionHealthResult == HealthResult.RESULT_HEALTHY) {
            return checkPermissions(config);
        } else {
            return connectionHealthResult;
        }
    }

    private static HealthResult checkConnection()
    {
        try {
            Kubectl.version().execute();
            return HealthResult.RESULT_HEALTHY;
        } catch (KubectlException e) {
            LOG.warn("Connection failure to kubernetes", e);
            return new HealthResult(HealthStatus.UNHEALTHY, "Cannot connect to Kubernetes");
        }
    }

    private static HealthResult checkPermissions(final K8sAutoscaleConfiguration config)
    {
        for (final String namespace: config.getNamespacesArray()) {

            final V1ResourceAttributes resourceAttributes = new V1ResourceAttributes();
            resourceAttributes.setGroup("apps");
            resourceAttributes.setResource("deployments");
            resourceAttributes.setVerb("patch");
            resourceAttributes.setNamespace(namespace);

            final V1SelfSubjectAccessReviewSpec spec = new V1SelfSubjectAccessReviewSpec();
            spec.setResourceAttributes(resourceAttributes);

            final V1SelfSubjectAccessReview body = new V1SelfSubjectAccessReview();
            body.setApiVersion("authorization.k8s.io/v1");
            body.setKind("SelfSubjectAccessReview");
            body.setSpec(spec);

            final V1SelfSubjectAccessReview review;
            try {
                review = new AuthorizationV1Api().createSelfSubjectAccessReview(body, "All", "fas", "true");
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
}
