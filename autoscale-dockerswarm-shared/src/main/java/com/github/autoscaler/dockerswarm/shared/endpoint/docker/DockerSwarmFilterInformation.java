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
package com.github.autoscaler.dockerswarm.shared.endpoint.docker;

import static com.github.autoscaler.dockerswarm.shared.endpoint.docker.DockerSwarmFilterInformation.FilterType.*;
import java.security.InvalidParameterException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Filter information definitions, to assist in building docker swarm rest filter requests.
 */
public class DockerSwarmFilterInformation extends DockerSwarmFilters
{
    private final static Logger LOG = LoggerFactory.getLogger(DockerSwarmFilterInformation.class);

    private static final String FILTER_PROPERTY_BY_KEY_AND_VALUE = "\"%s\":{\"%s=%s\":true}";
    private static final String FILTER_PROPERTY_BY_KEY_ONLY = "\"%s\":{\"%s\":true}";

    public enum FilterType
    {
        FilterTypeUndefined,
        FilterTypeKeyAndValue,
        FilterTypeKeyOnly;
    }

    final String filterWithinProperty;
    final String filterKeyName;
    final String filterKeyValue;

    final FilterType filterType;

    public DockerSwarmFilterInformation(final String filterWithinProperty, final String filterKeyName)
    {
        this.filterWithinProperty = filterWithinProperty;
        this.filterKeyName = filterKeyName;
        this.filterKeyValue = null;
        this.filterType = FilterTypeKeyOnly;
    }

    public DockerSwarmFilterInformation(final String filterWithinProperty, final String filterKeyName, final String filterKeyValue)
    {
        this.filterWithinProperty = filterWithinProperty;
        this.filterKeyName = filterKeyName;
        this.filterKeyValue = filterKeyValue;
        this.filterType = FilterTypeUndefined;
    }

    public String getFilterFormat()
    {
        switch (filterType) {

            case FilterTypeUndefined:
                // if someone has set the type, use that definition, otherwise work it out by the information we hold
                if (filterKeyValue == null || filterKeyValue.isEmpty()) {
                    return FILTER_PROPERTY_BY_KEY_ONLY;
                }

                return FILTER_PROPERTY_BY_KEY_AND_VALUE;
            case FilterTypeKeyAndValue:
                return FILTER_PROPERTY_BY_KEY_AND_VALUE;
            case FilterTypeKeyOnly:
                return FILTER_PROPERTY_BY_KEY_ONLY;
            default:
                throw new InvalidParameterException("Invalid filter type being used: " + filterType);
        }

    }

    /**
     * Build the specified DockerSwarmFilterInformation object into a string representation to be used in the Filter type calls.
     *
     * @return
     */
    protected String build()
    {
        final String filterAsString = String.format(getFilterFormat(),
                                                    filterWithinProperty,
                                                    filterKeyName,
                                                    filterKeyValue);

        LOG.trace("build constructed filter: " + filterAsString);
        return filterAsString;
    }
}
