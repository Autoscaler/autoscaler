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
package com.github.autoscaler.api;

public final class ResourceUtilisation
{
    private final double memoryUsedPercent;

    private final int diskFreeMb;

    public ResourceUtilisation(final double memoryUsedPercent, final int diskFreeMb)
    {
        this.memoryUsedPercent = memoryUsedPercent;
        this.diskFreeMb = diskFreeMb;
    }

    public double getMemoryUsedPercent()
    {
        return memoryUsedPercent;
    }

    public int getDiskFreeMb()
    {
        return diskFreeMb;
    }

    @Override
    public String toString()
    {
        return "ResourceUtilisation{" +
                "memoryUsedPercent=" + memoryUsedPercent +
                "diskFreeMb=" + diskFreeMb +
                '}';
    }
}
