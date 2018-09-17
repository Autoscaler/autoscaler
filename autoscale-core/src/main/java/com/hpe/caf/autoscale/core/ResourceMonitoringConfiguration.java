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
package com.hpe.caf.autoscale.core;

import com.hpe.caf.api.Configuration;
import javax.validation.constraints.NotNull;

@Configuration
public final class ResourceMonitoringConfiguration
{
    
     /**
     * What percentage of resource limit can be used before an alert should be sent.
     * Defaults to value of stage one threshold.
     */
    @NotNull
    private int alertDispatchThreshold;

    /**
     * Percentage of max memory available to use allowed before triggering resource limit one behaviour.
     * Defaults to 70.
     */
    @NotNull
    private double resourceLimitOne;

    /**
     * Percentage of max memory available to use allowed before triggering resource limit two behaviour.
     * Defaults to 80.
     */
    @NotNull
    private double resourceLimitTwo;

    /**
     * Percentage of max memory available to use allowed before triggering resource limit three behaviour.
     * Defaults to 90.
     */
    @NotNull
    private double resourceLimitThree;

    /**
     * Shutdown Priority threshold for resource limit one.
     * Defaults to 1.
     */
    @NotNull
    private int resourceLimitOneShutdownThreshold;

    /**
     * Shutdown Priority threshold for resource limit two.
     * Defaults to 3.
     */
    @NotNull
    private int resourceLimitTwoShutdownThreshold;

    /**
     * Shutdown Priority threshold for resource limit three.
     * Defaults to 5.
     */
    @NotNull
    private int resourceLimitThreeShutdownThreshold;

    /**
     * @return the resourceLimitOne
     */
    public double getResourceLimitOne()
    {
        return resourceLimitOne;
    }

    /**
     * @return the resourceLimitTwo
     */
    public double getResourceLimitTwo()
    {
        return resourceLimitTwo;
    }

    /**
     * @return the resourceLimitThree
     */
    public double getResourceLimitThree()
    {
        return resourceLimitThree;
    }

    /**
     * @return the resourceLimitOneShutdownThreshold
     */
    public int getResourceLimitOneShutdownThreshold()
    {
        return resourceLimitOneShutdownThreshold;
    }

    /**
     * @return the resourceLimitTwoShutdownThreshold
     */
    public int getResourceLimitTwoShutdownThreshold()
    {
        return resourceLimitTwoShutdownThreshold;
    }

    /**
     * @return the resourceLimitThreeShutdownThreshold
     */
    public int getResourceLimitThreeShutdownThreshold()
    {
        return resourceLimitThreeShutdownThreshold;
    }

    /**
     * @return the alertDispatchThreshold
     */
    public int getAlertDispatchThreshold()
    {
        return alertDispatchThreshold;
    }
}
