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
package com.github.autoscaler.api;

import com.hpe.caf.api.ConfigurationSource;

public interface AlertDispatcherFactory
{
    /**
     * Instantiate a new WorkloadAnalyser for a specific target and profile.
     *
     * @param configs The alert dispatcher configurations
     * @return a new AlertDispatcher instance for the given AlertDispatcher implementation
     * @throws ScalerException if the dispatcher cannot be created
     */
    AlertDispatcher getAlertDispatcher(ConfigurationSource configs) throws ScalerException;

    /**
     * @return a unique key name for this sort of AlertDispatcher the factory produces
     */
    String getAlertDispatcherName();
}
