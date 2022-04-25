/*
 * Copyright 2015-2022 Micro Focus or one of its affiliates.
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
 * Information about the host for a specific service.
 */
public final class ServiceHost
{
    private final String host;
    private final Collection<Integer> ports;


    public ServiceHost(final String hostName, final Collection<Integer> ports)
    {
        this.host = Objects.requireNonNull(hostName);
        this.ports = Collections.unmodifiableCollection(Objects.requireNonNull(ports));
    }


    /**
     * @return the hostname of the system hosting the service
     */
    public String getHost()
    {
        return host;
    }


    /**
     * @return the ports used by the service on this host
     */
    public Collection<Integer> getPorts()
    {
        return ports;
    }
}
