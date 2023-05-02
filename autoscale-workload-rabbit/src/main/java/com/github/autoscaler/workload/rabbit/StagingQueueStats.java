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

import com.google.common.base.MoreObjects;

/**
 * Class that holds information on the statistics of a staging queue from rabbit.
 */
public class StagingQueueStats
{
    private String name;
    private final int messages;

    public StagingQueueStats(final String name, final int messages)
    {
        this.name = name;
        this.messages = messages;
    }

    public String getName()
    {
        return name;
    }

    public int getMessages()
    {
        return messages;
    }

    @Override
    public String toString()
    {
        return MoreObjects.toStringHelper(this)
                .add("name", name)
                .add("messages", messages)
                .toString();
    }
}
