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
package com.github.autoscaler.api;

import java.util.Optional;

public final class ResourceUtilisation
{
    private final double memoryUsedPercent;

    private final Optional<Integer> diskFreeMbOpt;

    public ResourceUtilisation(final double memoryUsedPercent, final Optional<Integer> diskFreeMbOpt)
    {
        this.memoryUsedPercent = memoryUsedPercent;
        this.diskFreeMbOpt = diskFreeMbOpt;
    }

    public double getMemoryUsedPercent()
    {
        return memoryUsedPercent;
    }

    public Optional<Integer> getDiskFreeMbOpt()
    {
        return diskFreeMbOpt;
    }

    @Override
    public String toString()
    {
        final String diskFreeMbString = diskFreeMbOpt.isPresent() ? diskFreeMbOpt.get().toString() : "UNKNOWN";

        return "ResourceUtilisation{" +
                "memoryUsedPercent=" + memoryUsedPercent +
                ", diskFreeMb=" + diskFreeMbString +
                '}';
    }
}
