/*
 * Copyright 2015-2017 EntIT Software LLC, a Micro Focus company.
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
package com.hpe.caf.autoscale.source.marathon;


import com.hpe.caf.api.ConfigurationException;
import com.hpe.caf.api.ConfigurationSource;
import com.hpe.caf.api.autoscale.ScalerException;
import com.hpe.caf.api.autoscale.ServiceSource;
import com.hpe.caf.api.autoscale.ServiceSourceProvider;
import com.hpe.caf.autoscale.MarathonAutoscaleConfiguration;
import com.hpe.caf.naming.ServicePath;
import feign.Feign;
import feign.Request;
import mesosphere.marathon.client.Marathon;
import mesosphere.marathon.client.MarathonClient;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.Iterator;


public class MarathonServiceSourceProvider implements ServiceSourceProvider
{
    private static final int MARATHON_TIMEOUT = 10_000;


    @Override
    public ServiceSource getServiceSource(final ConfigurationSource configurationSource, final ServicePath servicePath)
            throws ScalerException
    {
        try {
            final MarathonAutoscaleConfiguration config = configurationSource.getConfiguration(MarathonAutoscaleConfiguration.class);
            final String groupId = getGroupId(config, servicePath);
            Feign.Builder builder = Feign.builder().options(new Request.Options(MARATHON_TIMEOUT, MARATHON_TIMEOUT));
            Marathon marathon = MarathonClient.getInstance(builder, config.getEndpoint());
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
