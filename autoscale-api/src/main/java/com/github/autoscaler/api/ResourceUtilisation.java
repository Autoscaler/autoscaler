/*
 * Copyright 2015-2025 Open Text.
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
    private final double rabbitMqMemoryUsedPercent;

    private final Optional<Integer> rabbitMqDiskFreeMbOpt;

    private final Optional<Integer> offloadingDiskFreeMbOpt;

    public ResourceUtilisation(final double rabbitMqMemoryUsedPercent,
                               final Optional<Integer> rabbitMqDiskFreeMbOpt,
                               final Optional<Integer> offloadingDiskFreeMbOpt)
    {
        this.rabbitMqMemoryUsedPercent = rabbitMqMemoryUsedPercent;
        this.rabbitMqDiskFreeMbOpt = rabbitMqDiskFreeMbOpt;
        this.offloadingDiskFreeMbOpt = offloadingDiskFreeMbOpt;
    }

    public double getRabbitMqMemoryUsedPercent()
    {
        return rabbitMqMemoryUsedPercent;
    }

    public Optional<Integer> getRabbitMqDiskFreeMbOpt()
    {
        return rabbitMqDiskFreeMbOpt;
    }

    public Optional<Integer> getOffloadingDiskFreeMbOpt() {
        return offloadingDiskFreeMbOpt;
    }

    @Override
    public String toString()
    {
        final String rabbitMqDiskFreeMbString = rabbitMqDiskFreeMbOpt.isPresent() ? rabbitMqDiskFreeMbOpt.get().toString() : "UNKNOWN";
        final String offloadingDiskFreeMbString = offloadingDiskFreeMbOpt.isPresent() ? offloadingDiskFreeMbOpt.get().toString() : "UNKNOWN";

        return "ResourceUtilisation{" +
                ", rabbitMqMemoryUsedPercent=" + rabbitMqMemoryUsedPercent +
                ", rabbitMqDiskFreeMbOpt=" + rabbitMqDiskFreeMbString  +
                ", offloadingDiskFreeMbOpt=" + offloadingDiskFreeMbString  +
                '}';
    }
}
