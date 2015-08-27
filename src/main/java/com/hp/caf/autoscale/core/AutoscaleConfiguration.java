package com.hp.caf.autoscale.core;


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
    @Min(2)
    @Max(20)
    private int executorThreads = 5;


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
