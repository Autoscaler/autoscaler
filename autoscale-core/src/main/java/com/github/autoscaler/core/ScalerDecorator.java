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
package com.github.autoscaler.core;


import com.github.autoscaler.api.InstanceInfo;
import com.github.autoscaler.api.ScalerException;
import com.github.autoscaler.api.ServiceScaler;
import com.hpe.caf.api.HealthResult;

import java.util.Objects;
import java.util.concurrent.atomic.AtomicBoolean;


/**
 * A ScalerDecorator is used by the AutoscaleApplication to trivially enable
 * or disable scaling behaviour depending upon whether this instance of the
 * AutoscaleApplication is active or not. The class mimics a ServiceScaler, hence
 * a Decorator, and if this instance is active, it will simply pass through all
 * requests to the actual ServiceScaler. If it is not, the scale up or down requests
 * will be discarded. This allows the rest of the application to behave identically.
 */
public class ScalerDecorator implements ServiceScaler
{
    private final ServiceScaler realScaler;
    private final AtomicBoolean active;


    public ScalerDecorator(final ServiceScaler serviceScaler, final boolean active)
    {
        this.realScaler = Objects.requireNonNull(serviceScaler);
        this.active = new AtomicBoolean(active);
    }


    /**
     * {@inheritDoc}
     *
     * If this instance is active, perform scale up, otherwise ignore.
     */
    @Override
    public void scaleUp(final String service, final int amount)
            throws ScalerException
    {
        if ( active.get() ) {
            realScaler.scaleUp(service, amount);
        }
    }


    /**
     * {@inheritDoc}
     *
     * If this instance is active, perform scale down, otherwise ignore.
     */
    @Override
    public void scaleDown(final String service, final int amount)
            throws ScalerException
    {
        if ( active.get() ) {
            realScaler.scaleDown(service, amount);
        }
    }


    @Override
    public InstanceInfo getInstanceInfo(final String service)
            throws ScalerException
    {
        return realScaler.getInstanceInfo(service);
    }


    /**
     * Update the active status of this instance.
     * @param active the new active status
     */
    public void setActive(final boolean active)
    {
        this.active.set(active);
    }


    @Override
    public HealthResult healthCheck()
    {
        return realScaler.healthCheck();
    }
}
