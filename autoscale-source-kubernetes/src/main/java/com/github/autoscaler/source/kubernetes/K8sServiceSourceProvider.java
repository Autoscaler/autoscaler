/*
 * Copyright 2015-2023 Open Text.
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
import com.hpe.caf.api.ConfigurationException;
import com.hpe.caf.api.ConfigurationSource;
import com.hpe.caf.naming.ServicePath;
import io.kubernetes.client.openapi.Configuration;
import io.kubernetes.client.util.ClientBuilder;

import java.io.IOException;

public class K8sServiceSourceProvider implements ServiceSourceProvider
{

    @Override
    public ServiceSource getServiceSource(final ConfigurationSource configurationSource, final ServicePath servicePath)
        throws ScalerException
    {
        try {
            final K8sAutoscaleConfiguration config = configurationSource.getConfiguration(K8sAutoscaleConfiguration.class);
            Configuration.setDefaultApiClient(ClientBuilder.standard().build());
            return new K8sServiceSource(config);
        } catch (ConfigurationException | IOException e) {
            throw new ScalerException("Failed to create service source", e);
        }
    }
}
