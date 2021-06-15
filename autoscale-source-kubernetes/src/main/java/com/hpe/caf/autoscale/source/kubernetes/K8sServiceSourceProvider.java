/*
 * Copyright 2015-2021 Micro Focus or one of its affiliates.
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
package com.hpe.caf.autoscale.source.kubernetes;

import com.hpe.caf.api.ConfigurationException;
import com.hpe.caf.api.ConfigurationSource;
import com.hpe.caf.api.autoscale.ScalerException;
import com.hpe.caf.api.autoscale.ServiceSource;
import com.hpe.caf.api.autoscale.ServiceSourceProvider;
import com.hpe.caf.autoscale.K8sAutoscaleConfiguration;
import com.hpe.caf.naming.ServicePath;
import io.kubernetes.client.openapi.Configuration;
import io.kubernetes.client.openapi.apis.AppsV1Api;

public class K8sServiceSourceProvider implements ServiceSourceProvider
{

    @Override
    public ServiceSource getServiceSource(final ConfigurationSource configurationSource, final ServicePath servicePath)
        throws ScalerException
    {
        try {
            final K8sAutoscaleConfiguration config = configurationSource.getConfiguration(K8sAutoscaleConfiguration.class);
            return new K8sServiceSource(new AppsV1Api(Configuration.getDefaultApiClient()), config);
        } catch (ConfigurationException e) {
            throw new ScalerException("Failed to create service source", e);
        }
    }
}
