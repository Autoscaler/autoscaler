/*
 * Copyright 2015-2017 Hewlett Packard Enterprise Development LP.
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
package com.hpe.caf.autoscale.scaler.endpoint.docker;

import com.hpe.caf.autoscale.scaler.endpoint.HttpClientException;
import com.hpe.caf.autoscale.scaler.endpoint.HttpClientSupport.ObjectList;
import com.hpe.caf.autoscale.scaler.endpoint.HttpClientSupport.TypedList;
import com.hpe.caf.autoscale.scaler.endpoint.Param;
import com.jayway.jsonpath.DocumentContext;

/**
 * Interface describing the communication with the Docker Swarm REST endpoint.
 */
public interface DockerSwarm {

    /**
     * Return a list of services.
     * @return 
     */
    public DocumentContext getServices() throws HttpClientException;
    
    /**
     * Return a filtered list of services.
     * @param filters
     * @throws HttpClientException 
     */
    public void getServicesFiltered(@Param("filters") String filters) throws HttpClientException;
}
