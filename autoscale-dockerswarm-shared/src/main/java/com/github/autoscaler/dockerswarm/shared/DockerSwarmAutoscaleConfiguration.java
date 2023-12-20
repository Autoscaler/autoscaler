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
package com.github.autoscaler.dockerswarm.shared;

import javax.validation.constraints.Min;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;

/**
 * Shared configuration between the docker swarm ServiceScaler and ServiceSource.
 */
public class DockerSwarmAutoscaleConfiguration
{
    /**
     * The URL to the Docker Swarm REST Endpoint.
     */
    @NotNull
    @Size(min = 1)
    private String endpoint;

    /**
     * The timeout to be used when communicating with the docker swarm endpoint.
     */
    @NotNull
    @Min(1)
    private Long timeoutInSecs;

    /**
     * The timeout to be used by the healthcheck when communicating with the docker swarm endpoint
     */
    @NotNull
    @Min(1)
    private Long healthCheckTimeoutInSecs;

    /**
     * Whether to use HTTPS certificate for endpoint communication.
     */
    @NotNull
    private Boolean tlsVerify;

    /**
     * The certificate to be used when communicating with the docker swarm endpoint when using https.
     */
    private String certificatePath;

    /**
     * The absolute maximum instances for any service in docker swarm.
     */
    @Min(1)
    private int maximumInstances;

    /**
     * The docker swarm stack to be auto-scaled
     */
    private String stackId;

    public DockerSwarmAutoscaleConfiguration()
    {
    }

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

    public String getStackId()
    {
        return stackId;
    }

    public void setStackId(String stackId)
    {
        this.stackId = stackId;
    }

    public Long getTimeoutInSecs()
    {
        return timeoutInSecs;
    }

    public void setTimeoutInSecs(Long timeoutInSecs)
    {
        this.timeoutInSecs = timeoutInSecs;
    }

    public Long getHealthCheckTimeoutInSecs()
    {
        return healthCheckTimeoutInSecs;
    }

    public void setHealthCheckTimeoutInSecs(Long healthCheckTimeoutInSecs)
    {
        this.healthCheckTimeoutInSecs = healthCheckTimeoutInSecs;
    }

    public Boolean getTlsVerify()
    {
        return tlsVerify;
    }

    public void setTlsVerify(Boolean tlsVerify)
    {
        this.tlsVerify = tlsVerify;
    }

    public String getCertificatePath()
    {
        return certificatePath;
    }

    public void setCertificatePath(String certificatePath)
    {
        this.certificatePath = certificatePath;
    }

}
