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
package com.hpe.caf.api.autoscale;


import com.hpe.caf.api.ConfigurationSource;


/**
 * Provides a method for acquiring a ServiceScaler. Implementations must have a no-arg constructor.
 */
public interface ServiceScalerProvider
{
    /**
     * Get a ServiceScaler implementation.
     * @param configurationSource used for configuring a ServiceScaler
     * @return a ServiceScaler implementation
     * @throws ScalerException if the ServiceScaler could not be created
     */
    ServiceScaler getServiceScaler(final ConfigurationSource configurationSource)
        throws ScalerException;
}
