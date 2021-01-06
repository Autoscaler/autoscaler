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
package com.hpe.caf.autoscale.core;


import com.hpe.caf.api.HealthResult;
import com.hpe.caf.api.autoscale.InstanceInfo;
import com.hpe.caf.api.autoscale.ScalerException;
import com.hpe.caf.api.autoscale.ServiceScaler;

import java.util.Objects;
import java.util.concurrent.atomic.AtomicBoolean;


/**
 * A ScalerDecorator is used by the AutoscaleApplication to trivially enable
 * or disable scaling behaviour depending upon whether this instance of the
 * AutoscaleApplication is master or not. The class mimicks a ServiceScaler, hence
 * a Decorator, and if this instance if the master, will simply pass through all
 * requests to the actual ServiceScaler. If it is not, the scale up or down requests
 * will be discarded. This allows the rest of the application to behave identically.
 */
public class ScalerDecorator implements ServiceScaler
{
    private final ServiceScaler realScaler;
    private final AtomicBoolean master;


    public ScalerDecorator(final ServiceScaler serviceScaler, final boolean master)
    {
        this.realScaler = Objects.requireNonNull(serviceScaler);
        this.master = new AtomicBoolean(master);
    }


    /**
     * {@inheritDoc}
     *
     * If this instance is the master, perform scale up, otherwise ignore.
     */
    @Override
    public void scaleUp(final String service, final int amount)
            throws ScalerException
    {
        if ( master.get() ) {
            realScaler.scaleUp(service, amount);
        }
    }


    /**
     * {@inheritDoc}
     *
     * If this instance is the master, perform scale down, otherwise ignore.
     */
    @Override
    public void scaleDown(final String service, final int amount)
            throws ScalerException
    {
        if ( master.get() ) {
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
     * Update the master status of this instance.
     * @param master the new master status
     */
    public void setMaster(final boolean master)
    {
        this.master.set(master);
    }


    @Override
    public HealthResult healthCheck()
    {
        return realScaler.healthCheck();
    }
}
