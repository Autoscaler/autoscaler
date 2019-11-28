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
package com.hpe.caf.autoscale.endpoint.docker;

import com.hpe.caf.autoscale.endpoint.HttpClientException;
import com.hpe.caf.autoscale.endpoint.Param;
import com.jayway.jsonpath.DocumentContext;

/**
 * Interface describing the communication with the Docker Swarm REST endpoint.
 */
public interface DockerSwarm
{

    /**
     * Return a list of services.
     *
     * @return
     */
    public DocumentContext getServices() throws HttpClientException;

    /**
     * Return a filtered list of services.
     *
     * @param filters
     * @return
     * @throws HttpClientException
     */
    public DocumentContext getServicesFiltered(@Param("filters") final String filters) throws HttpClientException;

    /**
     * Return details of the specific service specified by the serviceId.
     *
     * @param serviceId
     * @return
     * @throws HttpClientException
     */
    public DocumentContext getService(@Param("serviceId") final String serviceId) throws HttpClientException;

    /**
     * Update / scale the specific service using the new specification given
     *
     * @param serviceId The service to update
     * @param versionId The previous specification version that we are updating to prevent race conditions.
     * @param serviceSpecification The new service specification
     * @throws HttpClientException
     */
    public void updateService(@Param("serviceId") final String serviceId, @Param("versionId") final int versionId,
                              @Param("specification") final String serviceSpecification) throws HttpClientException;

    
    /**
     * Return a list of tasks.
     *
     * @return
     */
    public DocumentContext getTasks() throws HttpClientException;

    /**
     * Return a filtered list of tasks by filters like service name, label e.g. stack name etc.
     *
     * @param filters
     * @return
     * @throws HttpClientException
     */
    public DocumentContext getTasksFiltered(@Param("filters") final String filters) throws HttpClientException;

    /**
     * Return details of the specific service specified by the serviceId.
     *
     * @param taskId
     * @return
     * @throws HttpClientException
     */
    public DocumentContext getTask(@Param("taskId") final String taskId) throws HttpClientException;
    
}
