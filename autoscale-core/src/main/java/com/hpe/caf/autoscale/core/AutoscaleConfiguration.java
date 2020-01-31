/*
 * Copyright 2015-2020 Micro Focus or one of its affiliates.
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
package com.hpe.caf.autoscale.core;


import io.dropwizard.Configuration;

import javax.validation.constraints.Max;
import javax.validation.constraints.Min;


public class AutoscaleConfiguration extends Configuration
{
    /**
     * The time in seconds in between searching for new services (or changes to services)
     * that this instance of the autoscaler will handle.
     */
    @Min(5)
    private int sourceRefreshPeriod = 900;
    /**
     * The number of threads to use for the scheduled executor service.
     */
    @Min(1)
    @Max(20)
    private int executorThreads = 1;


    public int getSourceRefreshPeriod()
    {
        return sourceRefreshPeriod;
    }


    public void setSourceRefreshPeriod(final int sourceRefreshPeriod)
    {
        this.sourceRefreshPeriod = sourceRefreshPeriod;
    }


    public int getExecutorThreads()
    {
        return executorThreads;
    }


    public void setExecutorThreads(final int executorThreads)
    {
        this.executorThreads = executorThreads;
    }
}
