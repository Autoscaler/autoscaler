package com.hp.caf.autoscale.workload.rabbit;


/**
 * Class that holds information on the statistics of a queue from rabbit.
 */
public class QueueStats
{
    private final int messages;
    private final double publishRate;
    private final double consumeRate;


    public QueueStats(final int messages, final double publishRate, final double consumeRate)
    {
        this.messages = messages;
        this.publishRate = publishRate;
        this.consumeRate = consumeRate;
    }


    public int getMessages()
    {
        return messages;
    }


    public double getPublishRate()
    {
        return publishRate;
    }


    public double getConsumeRate()
    {
        return consumeRate;
    }


    @Override
    public String toString()
    {
        return "QueueStats{" +
               "messages=" + messages +
               ", publishRate=" + publishRate +
               ", consumeRate=" + consumeRate +
               '}';
    }
}
