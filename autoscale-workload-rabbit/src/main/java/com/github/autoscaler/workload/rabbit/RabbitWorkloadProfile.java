/*
 * Copyright 2015-2023 Open Text.
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
package com.github.autoscaler.workload.rabbit;


import javax.validation.constraints.Min;


/**
 * A scaling profile for a RabbitWorkloadAnalyser.
 */
public class RabbitWorkloadProfile
{
    /**
     * The number of analysis iterations that must be performed between
     * making scaling recommendations. In practice, this means that scaling
     * may be done every scalingDelay * interval seconds.
     */
    @Min(1)
    private int scalingDelay;
    /**
     * The time (in seconds) that the backlog of messages should be finished in.
     */
    @Min(1)
    private int backlogGoal;


    public RabbitWorkloadProfile() { }


    public RabbitWorkloadProfile(final int scalingDelay, final int backlogGoal)
    {
        this.scalingDelay = scalingDelay;
        this.backlogGoal = backlogGoal;
    }


    public int getScalingDelay()
    {
        return scalingDelay;
    }


    public void setScalingDelay(final int scalingDelay)
    {
        this.scalingDelay = scalingDelay;
    }


    public int getBacklogGoal()
    {
        return backlogGoal;
    }


    public void setBacklogGoal(final int backlogGoal)
    {
        this.backlogGoal = backlogGoal;
    }

    @Override
    public String toString()
    {
        return "RabbitWorkloadProfile{" +
                "scalingDelay=" + scalingDelay +
                ", backlogGoal=" + backlogGoal +
                '}';
    }
}
