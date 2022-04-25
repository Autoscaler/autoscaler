/*
 * Copyright 2015-2022 Micro Focus or one of its affiliates.
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
package com.github.autoscaler.api;


import com.hpe.caf.api.ConfigurationSource;


/**
 * Boilerplate for acquiring a WorkloadAnalyserFactory. It is holds the unique name for the WorkloadAnalyser.
 */
public interface WorkloadAnalyserFactoryProvider
{
    /**
     * Return a WorkloadAnalyserFactory.
     * @param configurationSource the configuration, used to setup a WorkloadAnalyserFactory
     * @return an implementation of a WorkloadAnalyserFactory
     * @throws ScalerException if the factory cannot be created
     */
    WorkloadAnalyserFactory getWorkloadAnalyserFactory(final ConfigurationSource configurationSource)
            throws ScalerException;


    /**
     * @return a unique key name for this sort of WorkloadAnalyser the factory produces
     */
    String getWorkloadAnalyserName();
}
