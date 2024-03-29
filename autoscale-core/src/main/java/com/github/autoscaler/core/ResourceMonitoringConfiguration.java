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
package com.github.autoscaler.core;

import com.hpe.caf.api.Configuration;
import jakarta.validation.constraints.NotNull;

@Configuration
public final class ResourceMonitoringConfiguration
{
    /**
     * The percentage of available memory used by the messaging platform that will trigger the Autoscaler to dispatch alerts indicating
     * that the messaging platform is running out of memory. Defaults to value of stage one threshold.
     */
    @NotNull
    private int memoryUsedPercentAlertDispatchThreshold;

    /**
     * The percentage of available memory used by the messaging platform that will trigger the Autoscaler to take stage 1 action.
     * Defaults to 70.
     */
    @NotNull
    private double memoryUsedPercentLimitStageOne;

    /**
     * The percentage of available memory used by the messaging platform that will trigger the Autoscaler to take stage 2 action.
     * Defaults to 80.
     */
    @NotNull
    private double memoryUsedPercentLimitStageTwo;

    /**
     * The percentage of available memory used by the messaging platform that will trigger the Autoscaler to take stage 1 action.
     * Defaults to 90.
     */
    @NotNull
    private double memoryUsedPercentLimitStageThree;

    /**
     * The amount of disk space (MB) remaining on the messaging platform that will trigger the Autoscaler to dispatch alerts indicating
     * that the messaging platform is running out of disk space. Defaults to value of stage one threshold.
     */
    @NotNull
    private int diskFreeMbAlertDispatchThreshold;

    /**
     * The amount of disk space (MB) remaining on the messaging platform that will trigger the Autoscaler to take stage 1 action.
     * Defaults to 400.
     */
    @NotNull
    private int diskFreeMbLimitStageOne;

    /**
     * The amount of disk space (MB) remaining on the messaging platform that will trigger the Autoscaler to take stage 2 action.
     * Defaults to 200.
     */
    @NotNull
    private int diskFreeMbLimitStageTwo;

    /**
     * The amount of disk space (MB) remaining on the messaging platform that will trigger the Autoscaler to take stage 3 action.
     * Defaults to 100.
     */
    @NotNull
    private int diskFreeMbLimitStageThree;

    /**
     * Shutdown Priority threshold for resource limit one. Defaults to 1.
     */
    @NotNull
    private int resourceLimitOneShutdownThreshold;

    /**
     * Shutdown Priority threshold for resource limit two. Defaults to 3.
     */
    @NotNull
    private int resourceLimitTwoShutdownThreshold;

    /**
     * Shutdown Priority threshold for resource limit three. Defaults to 5.
     */
    @NotNull
    private int resourceLimitThreeShutdownThreshold;

    /**
     * @return the memoryUsedPercentAlertDispatchThreshold
     */
    public int getMemoryUsedPercentAlertDispatchThreshold()
    {
        return memoryUsedPercentAlertDispatchThreshold;
    }

    /**
     * @return the memoryUsedPercentLimitStageOne
     */
    public double getMemoryUsedPercentLimitStageOne()
    {
        return memoryUsedPercentLimitStageOne;
    }

    /**
     * @return the memoryUsedPercentLimitStageTwo
     */
    public double getMemoryUsedPercentLimitStageTwo()
    {
        return memoryUsedPercentLimitStageTwo;
    }

    /**
     * @return the memoryUsedPercentLimitStageThree
     */
    public double getMemoryUsedPercentLimitStageThree()
    {
        return memoryUsedPercentLimitStageThree;
    }

    /**
     * @return the diskFreeMbAlertDispatchThreshold
     */
    public int getDiskFreeMbAlertDispatchThreshold()
    {
        return diskFreeMbAlertDispatchThreshold;
    }

    /**
     * @return the diskFreeMbLimitStageOne
     */
    public int getDiskFreeMbLimitStageOne()
    {
        return diskFreeMbLimitStageOne;
    }

    /**
     * @return the diskFreeMbLimitStageTwo
     */
    public int getDiskFreeMbLimitStageTwo()
    {
        return diskFreeMbLimitStageTwo;
    }

    /**
     * @return the diskFreeMbLimitStageThree
     */
    public int getDiskFreeMbLimitStageThree()
    {
        return diskFreeMbLimitStageThree;
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

    @Override
    public String toString()
    {
        return "ResourceMonitoringConfiguration{" +
                "memoryUsedPercentAlertDispatchThreshold=" + memoryUsedPercentAlertDispatchThreshold +
                ", memoryUsedPercentLimitStageOne=" + memoryUsedPercentLimitStageOne +
                ", memoryUsedPercentLimitStageTwo=" + memoryUsedPercentLimitStageTwo +
                ", memoryUsedPercentLimitStageThree=" + memoryUsedPercentLimitStageThree +
                ", diskFreeMbAlertDispatchThreshold=" + diskFreeMbAlertDispatchThreshold +
                ", diskFreeMbLimitStageOne=" + diskFreeMbLimitStageOne +
                ", diskFreeMbLimitStageTwo=" + diskFreeMbLimitStageTwo +
                ", diskFreeMbLimitStageThree=" + diskFreeMbLimitStageThree +
                ", resourceLimitOneShutdownThreshold=" + resourceLimitOneShutdownThreshold +
                ", resourceLimitTwoShutdownThreshold=" + resourceLimitTwoShutdownThreshold +
                ", resourceLimitThreeShutdownThreshold=" + resourceLimitThreeShutdownThreshold +
                '}';
    }
}
