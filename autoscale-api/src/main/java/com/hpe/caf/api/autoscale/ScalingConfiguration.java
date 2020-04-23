/*
 * Copyright 2015-2020 Micro Focus or one of its affiliates.
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
package com.hpe.caf.api.autoscale;


import javax.validation.constraints.Min;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;
import java.util.Objects;


/**
 * This object is returned by a ServiceSource and used by the autoscale application
 * in determining how to scale a service.
 */
public final class ScalingConfiguration
{
    public static final String KEY_WORKLOAD_METRIC = "autoscale.metric";
    public static final String KEY_INTERVAL = "autoscale.interval";
    public static final String KEY_SCALING_TARGET = "autoscale.scalingtarget";
    public static final String KEY_MIN_INSTANCES = "autoscale.mininstances";
    public static final String KEY_MAX_INSTANCES = "autoscale.maxinstances";
    public static final String KEY_SCALING_PROFILE = "autoscale.profile";
    public static final String KEY_BACKOFF_AMOUNT = "autoscale.backoff";
    public static final String KEY_SCALE_DOWN_BACKOFF_AMOUNT = "autoscale.scaledownbackoff";
    public static final String KEY_SCALE_UP_BACKOFF_AMOUNT = "autoscale.scaleupbackoff";
    /**
     * The unique id of the service
     */
    @NotNull
    @Size(min = 1)
    private String id;
    /**
     * The interval period (in seconds) between measuring the workload of the service
     */
    @Min(1)
    private int interval = 10;
    /**
     * The minimum number of instances of this service that are allowed
     */
    @Min(0)
    private int minInstances = 0;
    /**
     * The maximum number of instances of this service that are allowed
     */
    @Min(1)
    private int maxInstances = 5;
    /**
     * The number of interval periods to avoid scaling computations after a scale up/down has been issued
     */
    @Min(0)
    private int backoffAmount = 0;
    /**
     * The number of interval periods to avoid scaling computations after a scale down has been issued
     */
    @Min(-1)
    private int scaleDownBackoffAmount = -1;
    /**
     * The number of interval periods to avoid scaling computations after a scale up has been issued
     */
    @Min(-1)
    private int scaleUpBackoffAmount = -1;
    /**
     * The key/name of the WorkloadAnalyser to use for scaling this service
     */
    @NotNull
    @Size(min = 1)
    private String workloadMetric;
    /**
     * A reference to the target used for computing scaling (WorkloadAnalyser implementation specific)
     */
    private String scalingTarget;
    /**
     * The name of the profile to use for scaling the service (WorkloadAnalyser implementation specific)
     */
    private String scalingProfile;


    public ScalingConfiguration() { }


    public String getId()
    {
        return id;
    }


    public void setId(final String id)
    {
        this.id = id;
    }


    public int getInterval()
    {
        return interval;
    }


    public void setInterval(final int interval)
    {
        this.interval = interval;
    }


    public String getWorkloadMetric()
    {
        return workloadMetric;
    }


    public void setWorkloadMetric(final String workloadMetric)
    {
        this.workloadMetric = workloadMetric;
    }


    public String getScalingTarget()
    {
        return scalingTarget;
    }


    public void setScalingTarget(final String scalingTarget)
    {
        this.scalingTarget = scalingTarget;
    }


    public int getMinInstances()
    {
        return minInstances;
    }


    public void setMinInstances(final int minInstances)
    {
        this.minInstances = minInstances;
    }


    public int getMaxInstances()
    {
        return maxInstances;
    }


    public void setMaxInstances(final int maxInstances)
    {
        this.maxInstances = maxInstances;
    }


    public String getScalingProfile()
    {
        return scalingProfile;
    }


    public void setScalingProfile(final String scalingProfile)
    {
        this.scalingProfile = scalingProfile;
    }


    /**
     * @return the number of intervals to skip scaling computations following a scaling request.
     */
    public int getBackoffAmount()
    {
        return backoffAmount;
    }


    /**
     * @param backoffAmount the number of intervals to skip scaling computations following a scaling request.
     */
    public void setBackoffAmount(final int backoffAmount)
    {
        this.backoffAmount = backoffAmount;
    }

    /**
     * @return the number of intervals to skip scaling computations following a scaling request.
     */
    public int getScaleDownBackoffAmount()
    {
        return scaleDownBackoffAmount;
    }


    /**
     * @param scaleDownBackoffAmount the number of intervals to skip scaling computations following a scaling request.
     */
    public void setScaleDownBackoffAmount(final int scaleDownBackoffAmount)
    {
        this.scaleDownBackoffAmount = scaleDownBackoffAmount;
    }

    /**
     * @return the number of intervals to skip scaling computations following a scaling request.
     */
    public int getScaleUpBackoffAmount()
    {
        return scaleUpBackoffAmount;
    }


    /**
     * @param scaleUpBackoffAmount the number of intervals to skip scaling computations following a scaling request.
     */
    public void setScaleUpBackoffAmount(final int scaleUpBackoffAmount)
    {
        this.scaleUpBackoffAmount = scaleUpBackoffAmount;
    }


    @Override
    public String toString()
    {
        return "ScalingService{" +
               "id='" + id + '\'' +
               ", interval=" + interval +
               ", workloadMetric=" + workloadMetric +
               '}';
    }


    @Override
    public boolean equals(Object o)
    {
        if ( this == o ) {
            return true;
        }
        if ( o == null || getClass() != o.getClass() ) {
            return false;
        }
        ScalingConfiguration that = (ScalingConfiguration) o;
        return interval == that.interval &&
               minInstances == that.minInstances &&
               maxInstances == that.maxInstances &&
               backoffAmount == that.backoffAmount &&
               Objects.equals(id, that.id) &&
               Objects.equals(scalingProfile, that.scalingProfile) &&
               Objects.equals(workloadMetric, that.workloadMetric) &&
               Objects.equals(scalingTarget, that.scalingTarget);
    }


    @Override
    public int hashCode()
    {
        return Objects.hash(id, interval, minInstances, maxInstances, workloadMetric, scalingTarget, scalingProfile, backoffAmount);
    }
}
