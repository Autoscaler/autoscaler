package com.hpe.caf.autoscale.core;


import com.hpe.caf.api.autoscale.ScalingConfiguration;

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
