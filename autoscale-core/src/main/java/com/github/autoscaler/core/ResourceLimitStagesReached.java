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

public final class ResourceLimitStagesReached
{
    private final ResourceLimitStage memoryLimitStageReached;

    private final ResourceLimitStage diskLimitStageReached;

    public ResourceLimitStagesReached(final ResourceLimitStage memoryLimitStageReached, final ResourceLimitStage diskLimitStageReached)
    {
        this.memoryLimitStageReached = memoryLimitStageReached;
        this.diskLimitStageReached = diskLimitStageReached;
    }

    public ResourceLimitStage getMemoryLimitStageReached()
    {
        return memoryLimitStageReached;
    }

    public ResourceLimitStage getDiskLimitStageReached()
    {
        return diskLimitStageReached;
    }

    @Override
    public String toString()
    {
        return "ResourceLimitStagesReached{" +
                "memoryLimitStageReached=" + memoryLimitStageReached +
                ", diskLimitStageReached=" + diskLimitStageReached +
                '}';
    }
}
