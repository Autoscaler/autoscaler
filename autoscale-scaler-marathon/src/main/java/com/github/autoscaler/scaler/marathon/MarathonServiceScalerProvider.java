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
package com.github.autoscaler.scaler.marathon;


import com.github.autoscaler.api.ScalerException;
import com.github.autoscaler.api.ServiceScaler;
import com.github.autoscaler.api.ServiceScalerProvider;
import com.github.autoscaler.marathon.shared.MarathonAutoscaleConfiguration;
import com.hpe.caf.api.ConfigurationException;
import com.hpe.caf.api.ConfigurationSource;
import mesosphere.marathon.client.Marathon;
import mesosphere.marathon.client.MarathonClient;

import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;


public class MarathonServiceScalerProvider implements ServiceScalerProvider
{
    @Override
    public ServiceScaler getServiceScaler(final ConfigurationSource configurationSource)
            throws ScalerException
    {
        try {
            MarathonAutoscaleConfiguration config = configurationSource.getConfiguration(MarathonAutoscaleConfiguration.class);
            URL url = new URL(config.getEndpoint());
            Marathon marathon = MarathonClient.getInstance(url.toString());
            return new MarathonServiceScaler(marathon, config.getMaximumInstances(), url, new AppInstancePatcher(url.toURI()));
        } catch (ConfigurationException | MalformedURLException | URISyntaxException e) {
            throw new ScalerException("Failed to create service scaler", e);
        }
    }
}
