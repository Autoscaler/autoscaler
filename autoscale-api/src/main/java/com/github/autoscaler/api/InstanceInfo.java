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
package com.github.autoscaler.api;


import java.util.Collection;
import java.util.Collections;
import java.util.Objects;


/**
 * Contains information about the instances of a currently running service.
 */
public class InstanceInfo
{
    private final int instancesRunning;
    private final int instancesStaging;
    private final Collection<ServiceHost> hosts;
    private final int shutdownPriority;
    private final int instances;


    /**
     * Creates a new InstanceInfo with a service shutdown priority of -1.
     * 
     * @param running number of instances running
     * @param staging number of instances in staging
     * @param hosts hosts running or staging an instance of this application
     */
    public InstanceInfo(final int running, final int staging, final Collection<ServiceHost> hosts)
    {
        this(running, staging, hosts, -1, running + staging);
    }

    /**
     * Creates a new InstanceInfo.
     *
     * @param running number of instances running
     * @param staging number of instances in staging
     * @param hosts hosts running or staging an instance of this application
     * @param shutdownPriority the priority of the service, used when making scaling decisions during resource shortages
     * @param instances The total number of instances registered against the app including all running, staging and waiting
     */
    public InstanceInfo(final int running, final int staging, final Collection<ServiceHost> hosts, final int shutdownPriority,
                        final int instances)
    {
        this.instancesRunning = running;
        this.instancesStaging = staging;
        this.hosts = Collections.unmodifiableCollection(Objects.requireNonNull(hosts));
        this.shutdownPriority = shutdownPriority;
        this.instances = instances;
    }


    /**
     * @return the number of instances of a service that are running and active
     */
    public int getInstancesRunning()
    {
        return instancesRunning;
    }


    /**
     * @return the number of instances of a service that are being prepared to run
     */
    public int getInstancesStaging()
    {
        return instancesStaging;
    }


    /**
     * @return the combination of the number of running instances and those being prepared
     */
    public int getTotalRunningAndStageInstances()
    {
        return getInstancesRunning() + getInstancesStaging();
    }


    /**
     * @return the hosts that these instances are running upon
     */
    public Collection<ServiceHost> getHosts()
    {
        return hosts;
    }


    /**
     * @return the priority that these instances have, value may be null if no priority was set
     */
    public int getShutdownPriority()
    {
        return shutdownPriority;
    }
    
    /**
     * @return the number of instances of this application that are registered against this app in marathon, this will include all 
     * instances running, staging and waiting.
     */
    public int getInstances()
    {
        return this.instances;
    }  
}
