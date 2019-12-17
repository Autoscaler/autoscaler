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
package com.hpe.caf.autoscale.workload.rabbit;


import com.hpe.caf.api.Configuration;
import com.hpe.caf.api.ContainsStringKeys;
import com.hpe.caf.api.Encrypted;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;
import java.util.Collections;
import java.util.Map;


/**
 * Configuration for a RabbitWorkloadAnalyser.
 */
@Configuration
public class RabbitWorkloadAnalyserConfiguration
{
    public static final String DEFAULT_PROFILE_NAME = "default";

    /**
     * The HTTP endpoint/URL of a RabbitMQ management host.
     */
    @NotNull
    @Size(min = 1)
    private String rabbitManagementEndpoint;
    /**
     * The user name for the RabbitMQ management host.
     */
    @NotNull
    @Size(min = 1)
    private String rabbitManagementUser;
    /**
     * The password for the RabbitMQ management host.
     */
    @NotNull
    @Size(min = 1)
    @Encrypted
    private String rabbitManagementPassword;
    /**
     * The vhost that contains the queues. Defaults to / on RabbitMQ.
     */
    @NotNull
    @Size(min = 1)
    private String vhost = "/";
    /**
     * The profiles for the analyser. There must be at least one, and
     * there must be a default.
     */
    @NotNull
    @Size(min = 1)
    @ContainsStringKeys(keys = {DEFAULT_PROFILE_NAME})
    @Valid
    private Map<String, RabbitWorkloadProfile> profiles;

    @NotNull
    private int memoryQueryRequestFrequency;


    public RabbitWorkloadAnalyserConfiguration() { }


    public String getRabbitManagementEndpoint()
    {
        return rabbitManagementEndpoint;
    }


    public void setRabbitManagementEndpoint(final String rabbitManagementEndpoint)
    {
        this.rabbitManagementEndpoint = rabbitManagementEndpoint;
    }


    public String getRabbitManagementUser()
    {
        return rabbitManagementUser;
    }


    public void setRabbitManagementUser(final String rabbitManagementUser)
    {
        this.rabbitManagementUser = rabbitManagementUser;
    }


    public String getRabbitManagementPassword()
    {
        return rabbitManagementPassword;
    }


    public void setRabbitManagementPassword(final String rabbitManagementPassword)
    {
        this.rabbitManagementPassword = rabbitManagementPassword;
    }


    public String getVhost()
    {
        return vhost;
    }


    public void setVhost(final String vhost)
    {
        this.vhost = vhost;
    }


    public Map<String, RabbitWorkloadProfile> getProfiles()
    {
        return Collections.unmodifiableMap(profiles);
    }
    

    public void setProfiles(final Map<String, RabbitWorkloadProfile> profiles)
    {
        this.profiles = profiles;
    }

    public void setMemoryQueryRequestFrequency(final int memoryQueryRequestFrequency)
    {
        this.memoryQueryRequestFrequency = memoryQueryRequestFrequency;
    }

    public int getMemoryQueryRequestFrequency()
    {
        return memoryQueryRequestFrequency;
    }
}
