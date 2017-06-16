/*
 * Copyright 2015-2017 Hewlett Packard Enterprise Development LP.
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
package com.hpe.caf.autoscale.source.docker.swarm;

import com.hpe.caf.api.ConfigurationException;
import com.hpe.caf.api.ConfigurationSource;
import com.hpe.caf.api.autoscale.ScalerException;
import com.hpe.caf.api.autoscale.ServiceSource;
import com.hpe.caf.api.autoscale.ServiceSourceProvider;
import com.hpe.caf.autoscale.DockerSwarmAutoscaleConfiguration;
import com.hpe.caf.naming.ServicePath;

import java.net.MalformedURLException;
import java.net.URL;

public class DockerSwarmServiceSourceProvider implements ServiceSourceProvider
{

    @Override
    public ServiceSource getServiceSource(final ConfigurationSource configurationSource, final ServicePath servicePath)
        throws ScalerException
    {
        try {
            final DockerSwarmAutoscaleConfiguration config = configurationSource.getConfiguration(DockerSwarmAutoscaleConfiguration.class);
            final URL url = new URL(config.getEndpoint());
            final String stackId = getStackId(config, servicePath);

            return new DockerSwarmServiceSource(config,  url);
        } catch (ConfigurationException | MalformedURLException e) {
            throw new ScalerException("Failed to create service source", e);
        }
    }

    /**
     * Gets the stackId from configuration, if no valid configuration given, decision was taken to error out and not to look through
     * all stacks deployed on the swarm.
     */
    private static String getStackId(final DockerSwarmAutoscaleConfiguration config, final ServicePath servicePath) throws ConfigurationException
    {
        final String stackPath = config.getStackId();

        if (stackPath == null || stackPath.isEmpty() ) {
            // Check should we fallback to service path - CAF-3093
            throw new ConfigurationException("Invalid configuration, no valid autoscaler stack identifier has been specified for scaling.");
        }
        return stackPath;
    }
}
