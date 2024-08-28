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
package com.github.autoscaler.source.kubernetes;

import com.github.autoscaler.api.ScalerException;
import com.github.autoscaler.api.ServiceSource;
import com.github.autoscaler.api.ServiceSourceProvider;
import com.github.autoscaler.kubernetes.shared.K8sAutoscaleConfiguration;
import com.github.cafapi.kubernetes.client.KubernetesClientFactory;
import com.github.cafapi.kubernetes.client.client.ApiClient;
import com.hpe.caf.api.ConfigurationSource;
import com.hpe.caf.naming.ServicePath;

public class K8sServiceSourceProvider implements ServiceSourceProvider
{

    @Override
    public ServiceSource getServiceSource(final ConfigurationSource configurationSource, final ServicePath servicePath)
        throws ScalerException
    {
        try {
            final K8sAutoscaleConfiguration config = configurationSource.getConfiguration(K8sAutoscaleConfiguration.class);
            final ApiClient apiClient = KubernetesClientFactory.createClientWithCertAndToken();
            return new K8sServiceSource(config, apiClient);
        } catch (Exception e) {
            throw new ScalerException("Failed to create service source", e);
        }
    }
}
