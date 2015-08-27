package com.hp.caf.autoscale.core;


import com.codahale.metrics.health.HealthCheck;
import com.hp.caf.api.HealthReporter;
import com.hp.caf.api.HealthResult;
import com.hp.caf.api.HealthStatus;

import java.util.Objects;


/**
 * Wrapper for Dropwizard metrics health checks.
 */
public class ScalerHealthCheck extends HealthCheck
{
    private final HealthReporter reporter;


    public ScalerHealthCheck(final HealthReporter healthReporter)
    {
        this.reporter = Objects.requireNonNull(healthReporter);
    }


    @Override
    protected HealthCheck.Result check()
            throws Exception
    {
        HealthResult result = reporter.healthCheck();
        String message = result.getMessage();

        if ( result.getStatus() == HealthStatus.HEALTHY ) {
            return message == null ? HealthCheck.Result.healthy() : HealthCheck.Result.healthy(message);
        } else {
            return HealthCheck.Result.unhealthy(message);
        }
    }
}
