package com.hpe.caf.autoscale;


import javax.validation.constraints.Min;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;


/**
 * Shared configuration between the MarathonServiceScaler and MarathonServiceSource.
 */
public class MarathonAutoscaleConfiguration
{
    /**
     * The URL to the Marathon HTTP server.
     */
    @NotNull
    @Size(min = 1)
    private String endpoint;

    /**
     * The absolute maximum instances for any service in Marathon.
     */
    @Min(1)
    private int maximumInstances;

    /**
     * The Marathon group to be auto-scaled
     */
    private String groupId;


    public MarathonAutoscaleConfiguration() { }


    public String getEndpoint()
    {
        return endpoint;
    }


    public void setEndpoint(final String endpoint)
    {
        this.endpoint = endpoint;
    }


    public int getMaximumInstances()
    {
        return maximumInstances;
    }


    public void setMaximumInstances(final int maximumInstances)
    {
        this.maximumInstances = maximumInstances;
    }


    public String getGroupId()
    {
        return groupId;
    }


    public void setGroupId(final String groupId)
    {
        this.groupId = groupId;
    }
}
