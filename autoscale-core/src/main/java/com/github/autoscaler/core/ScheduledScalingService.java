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
package com.github.autoscaler.core;


import com.github.autoscaler.api.ScalingConfiguration;
import java.util.Objects;
import java.util.concurrent.ScheduledFuture;


/**
 * Simple structure for associating a ScalingConfiguration with its scheduled scaling task.
 */
public class ScheduledScalingService
{
    private final ScalingConfiguration config;
    private final ScheduledFuture<?> schedule;


    public ScheduledScalingService(final ScalingConfiguration scalingConfiguration, final ScheduledFuture<?> scheduledFuture)
    {
        this.config = Objects.requireNonNull(scalingConfiguration);
        this.schedule = Objects.requireNonNull(scheduledFuture);
    }


    public ScalingConfiguration getConfig()
    {
        return config;
    }


    public ScheduledFuture<?> getSchedule()
    {
        return schedule;
    }
}
