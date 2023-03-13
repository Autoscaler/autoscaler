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


import java.util.Objects;


/**
 * A representation of a scaling request to be handled by the autoscaler.
 */
public final class ScalingAction
{
    private final ScalingOperation operation;
    private final int amount;
    /**
     * Perform no scaling.
     */
    public static final ScalingAction NO_ACTION = new ScalingAction(ScalingOperation.NONE, 0);
    /**
     * Scale up by one instance.
     */
    public static final ScalingAction SCALE_UP = new ScalingAction(ScalingOperation.SCALE_UP, 1);
    /**
     * Scale down by one instance.
     */
    public static final ScalingAction SCALE_DOWN = new ScalingAction(ScalingOperation.SCALE_DOWN, 1);


    public ScalingAction(final ScalingOperation scalingOperation, final int amount)
    {
        this.operation = Objects.requireNonNull(scalingOperation);
        this.amount = amount;
    }


    /**
     * @return the operation requested
     */
    public ScalingOperation getOperation()
    {
        return operation;
    }


    /**
     * @return the amount of instances requested to scale up or down by
     */
    public int getAmount()
    {
        return amount;
    }
}
