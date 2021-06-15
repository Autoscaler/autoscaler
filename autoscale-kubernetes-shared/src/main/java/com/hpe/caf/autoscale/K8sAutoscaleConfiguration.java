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

import javax.validation.constraints.Min;

/**
 * Shared configuration between the KubernetesServiceScalar and KubernetesServiceSource.
 */
public class K8sAutoscaleConfiguration
{
    /**
     * The absolute maximum instances for any service in Marathon.
     */
    @Min(1)
    private int maximumInstances;

    private String namespace;

    public int getMaximumInstances()
    {
        return maximumInstances;
    }

    public void setMaximumInstances(int maximumInstances)
    {
        this.maximumInstances = maximumInstances;
    }

    public String getNamespace()
    {
        return this.namespace;
    }
    public void setNamespace(String namespace)
    {
        this.namespace = namespace;
    }
}
