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
package com.hpe.caf.autoscale;

import static java.util.stream.Collectors.toList;

import javax.validation.constraints.Min;
import javax.validation.constraints.NotEmpty;
import java.util.List;
import java.util.stream.Stream;

/**
 * Shared configuration between the K8sServiceScalar and K8sServiceSource.
 */
public class K8sAutoscaleConfiguration
{
    /**
     * The absolute maximum instances for any service in Kubernetes.
     */
    @Min(1)
    private int maximumInstances;

    @NotEmpty
    private String metric;

    @NotEmpty
    private String namespaces;
    
    @NotEmpty
    private String resourceIdSeparator;
    
    public String getMetric()
    {
        return metric;
    }

    public void setMetric(final String metric)
    {
        this.metric = metric;
    }


    public int getMaximumInstances()
    {
        return maximumInstances;
    }

    public void setMaximumInstances(final int maximumInstances)
    {
        this.maximumInstances = maximumInstances;
    }

    public void setNamespaces(String namespaces)
    {
        this.namespaces = namespaces;
    }

    public List<String> getNamespacesArray()
    {
        return Stream.of(this.namespaces.split(","))
            .map(String::trim)
            .collect(toList());
    }

    public String getResourceIdSeparator()
    {
        return resourceIdSeparator;
    }

    public void setResourceIdSeparator(String resourceIdSeparator)
    {
        this.resourceIdSeparator = resourceIdSeparator;
    }
}
