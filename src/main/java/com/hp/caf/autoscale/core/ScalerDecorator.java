package com.hp.caf.autoscale.core;


import com.hp.caf.api.HealthResult;
import com.hp.caf.api.autoscale.InstanceInfo;
import com.hp.caf.api.autoscale.ScalerException;
import com.hp.caf.api.autoscale.ServiceScaler;

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
public class ScalerDecorator extends ServiceScaler
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
