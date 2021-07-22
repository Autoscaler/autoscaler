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
package com.github.autoscaler.source.marathon;


import com.github.autoscaler.api.ScalerException;
import com.github.autoscaler.api.ServiceSource;
import com.github.autoscaler.api.ServiceSourceProvider;
import com.github.autoscaler.marathon.shared.MarathonAutoscaleConfiguration;
import com.hpe.caf.api.ConfigurationException;
import com.hpe.caf.api.ConfigurationSource;
import com.hpe.caf.naming.ServicePath;
import mesosphere.marathon.client.Marathon;
import mesosphere.marathon.client.MarathonClient;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.Iterator;


public class MarathonServiceSourceProvider implements ServiceSourceProvider
{
    @Override
    public ServiceSource getServiceSource(final ConfigurationSource configurationSource, final ServicePath servicePath)
            throws ScalerException
    {
        try {
            final MarathonAutoscaleConfiguration config = configurationSource.getConfiguration(MarathonAutoscaleConfiguration.class);
            final String groupId = getGroupId(config, servicePath);
            Marathon marathon = MarathonClient.getInstance(config.getEndpoint());
            return new MarathonServiceSource(marathon, groupId, new URL(config.getEndpoint()));
        } catch (ConfigurationException | MalformedURLException e) {
            throw new ScalerException("Failed to create service source", e);
        }
    }

    /**
     * Gets the groupId from configuration or falls back to using the autoscaler service path
     */
    private static String getGroupId(final MarathonAutoscaleConfiguration config, final ServicePath servicePath)
    {
        final String groupId = config.getGroupId();

        return (groupId == null || groupId.length() == 0)
            ? getGroupIdFromServicePath(servicePath)
            : groupId;
    }

    /**
     * Gets the groupId from the autoscaler service path
     */
    private static String getGroupIdFromServicePath(final ServicePath servicePath)
    {
        final Iterator<String> groupIterator = servicePath.groupIterator();
        final StringBuilder groupPath = new StringBuilder();
        while (groupIterator.hasNext()) {
            groupPath.append(groupIterator.next()).append('/');
        }

        return groupPath.toString();
    }
}
