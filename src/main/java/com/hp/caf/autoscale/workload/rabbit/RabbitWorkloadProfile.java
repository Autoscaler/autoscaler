package com.hp.caf.autoscale.workload.rabbit;


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
}
