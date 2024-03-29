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


import com.hpe.caf.api.HealthReporter;

import java.util.Set;


/**
 * A ServiceSource is responsible for finding and returning services
 * that an autoscaler can handle and scale.
 */
public interface ServiceSource extends HealthReporter
{
    /**
     * @return services that the autoscaler should validate and scale
     * @throws ScalerException if the services cannot be acquired
     */
    Set<ScalingConfiguration> getServices()
        throws ScalerException;
}
