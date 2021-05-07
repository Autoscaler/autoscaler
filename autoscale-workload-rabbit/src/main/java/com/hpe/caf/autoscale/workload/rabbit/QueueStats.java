/*
 * Copyright 2015-2021 Micro Focus or one of its affiliates.
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
package com.hpe.caf.autoscale.workload.rabbit;


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
