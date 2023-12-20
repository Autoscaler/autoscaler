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
package com.github.autoscaler.dockerswarm.shared.endpoint.docker;

import java.util.Collection;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * DockerSwarm filters class, groups together definitions for how and by what an object can be filtered.
 */
public class DockerSwarmFilters
{
    private final static Logger LOG = LoggerFactory.getLogger(DockerSwarmFilters.class);

    public class ServiceFilterByType
    {
        public final static String LABEL = "label";
        public final static String NAME = "name";
        public final static String ID = "id";
    }

    public class FilterLabelKeys
    {
        public final static String DOCKER_STACK = "com.docker.stack.namespace";
    }

    /**
     *
     */
    public class TaskFilters
    {
        // states are running | shutdown | accepted
        public final static String DESIRED_STATE = "desired-state";
        // <task id>
        public final static String ID = "id";
        // label=key or label="key=value"
        public final static String LABEL = "label";
        // name =<task name>
        public final static String NAME = "name";
        // node=<node id or name>
        public final static String NODE = "node";
        // service = <service name or uuid>
        public final static String SERVICE = "service";

    }

    public enum TaskState
    {
        RUNNING,
        ACCEPTED,
        SHUTDOWN;

        @Override
        public String toString()
        {
            return this.name().toLowerCase();
        }

    }

    DockerSwarmFilters()
    {
    }

    /**
     *
     * @param filterWithinProperty
     * @param filterKeyName
     * @param filterKeyValue
     * @return String representing the filter information supplied.
     */
    public static String buildServiceFilter(final String filterWithinProperty, final String filterKeyName, final String filterKeyValue)
    {
        return buildFilterInformation(new DockerSwarmFilterInformation(filterWithinProperty, filterKeyName, filterKeyValue));
    }

    /**
     * Build a filter based on a single piece of DockerSwarm filter information.
     *
     * @param filterInformation
     * @return String representing the filter information supplied.
     */
    public static String buildFilterInformation(final DockerSwarmFilterInformation filterInformation)
    {
        final String filterAsString = String.format("{%s}", filterInformation.build());
        LOG.trace("buildServiceFilter constructed filter: " + filterAsString);
        return filterAsString;
    }

    /**
     * Construct filter information based on a list of various filter information.
     * @param filterInformationList
     * @return String representing the filter information supplied.
     */
    public static String buildFilterInformation(final Collection<DockerSwarmFilterInformation> filterInformationList)
    {
        StringBuilder sb = new StringBuilder();
        for (DockerSwarmFilterInformation filterInformation : filterInformationList) {
            final String filterAsString = filterInformation.build();
            
            if ( sb.length() > 0)
            {
                // we have already got an item, as such add on a comma before next item
                sb.append(",");
            }
            sb.append(filterAsString);
        }

        final String filterAsString = String.format("{%s}", sb.toString());
        LOG.trace("buildServiceFilter constructed filter: " + filterAsString);
        return filterAsString;
    }
}
