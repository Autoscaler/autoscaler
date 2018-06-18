/*
 * Copyright 2015-2018 Micro Focus or one of its affiliates.
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
package com.hpe.caf.autoscale.scaler.docker.swarm;

import com.hpe.caf.api.ConfigurationException;
import com.hpe.caf.api.ConfigurationSource;
import com.hpe.caf.api.autoscale.ScalerException;
import com.hpe.caf.api.autoscale.ServiceScaler;
import com.hpe.caf.api.autoscale.ServiceScalerProvider;
import com.hpe.caf.autoscale.DockerSwarmAutoscaleConfiguration;
import com.hpe.caf.autoscale.endpoint.docker.DockerSwarm;
import com.hpe.caf.autoscale.endpoint.docker.DockerSwarmClient;

import java.net.MalformedURLException;
import java.net.URL;

public class DockerSwarmServiceScalerProvider implements ServiceScalerProvider
{
    @Override
    public ServiceScaler getServiceScaler(final ConfigurationSource configurationSource)
        throws ScalerException
    {
        try {
            final DockerSwarmAutoscaleConfiguration config = configurationSource.getConfiguration(DockerSwarmAutoscaleConfiguration.class);
            final DockerSwarm dockerClient = DockerSwarmClient.getInstance(config);            
            final URL url = new URL(config.getEndpoint());
            
            return new DockerSwarmServiceScaler(dockerClient, config, url);
        } catch (ConfigurationException | MalformedURLException e) {
            throw new ScalerException("Failed to create service scaler", e);
        }
    }
}
