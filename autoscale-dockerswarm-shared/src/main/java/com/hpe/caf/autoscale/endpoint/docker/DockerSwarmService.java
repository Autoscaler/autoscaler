/*
 * Copyright 2015-2018 Micro Focus or one of its affiliates.
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

import com.hpe.caf.api.autoscale.InstanceInfo;
import com.hpe.caf.api.autoscale.ServiceHost;
import com.jayway.jsonpath.DocumentContext;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.Objects;

/**
 * A simple object to hold service information, which makes it easier to hold onto the JSON document context, while we query it, or make
 * changes to it to update the service. Note this isn't meant to be a fixed representation, only a location to hold onto the generic JSON
 * document, and supporting methods to make querying it easier.
 *
 */
public class DockerSwarmService
{
    private DocumentContext documentContext;
    private LinkedHashMap serviceRepresentation;
    private String serviceReference;

    /**
     * Constructor to cache off information someone already hold.
     *
     * @param documentContext Response context with JSON information for a given service.
     * @param serviceItem Response context in a serialized JSON generic object format.
     * @param serviceReference Unique service reference for this service instance.
     */
    public DockerSwarmService(final DocumentContext documentContext, final LinkedHashMap serviceItem,
                              final String serviceReference)
    {
        Objects.requireNonNull(documentContext);
        Objects.requireNonNull(serviceItem);
        Objects.requireNonNull(serviceReference);

        this.documentContext = documentContext;
        this.serviceRepresentation = serviceItem;
        this.serviceReference = serviceReference;
    }

    /**
     * Construct and return the instance information based on the documentContext we hold about a service item.
     *
     * @return Instance information about this service
     * @throws java.lang.Exception
     */
    public InstanceInfo getInstanceInformation() throws Exception
    {
        // make sure we already have the responseDocument information cached and build Instance information from this.
        Objects.requireNonNull(documentContext);

        // Find out how many instances are currently meant to be running.  We have taken a decision to leave
        // the orchestrator in charge of the number of running instances i.e. we aren't going to check the acutal number of running 
        // tasks.
        LinkedList<Integer> replicasList = documentContext.read("$..Spec.Mode.Replicated.Replicas");

        // service hosts aren't being by API any longer, so returning blank list until its deprecated. 
        Collection<ServiceHost> blankServiceHosts = new ArrayList<>();

        if (replicasList.isEmpty()) {
            /**
             * The default mode for scaling in docker swarm = replicated. Although if we have an no replicas element, we need to assume a
             * single instance for the given service. Either because it hasn't been scaled above 1 element yet, or it is in swarm deploy
             * mode = global
             */
            return new InstanceInfo(1, 0, blankServiceHosts, -1);
        }

        if (replicasList.size() != 1) {
            throw new Exception(
                String.format("Failed to get correct service information for reference: %s - found %d replica elements.",
                              serviceReference, replicasList.size()));
        }

        return new InstanceInfo(replicasList.get(0), 0, blankServiceHosts, -1);
    }

    /**
     * Accessors
     */
    /**
     *
     * @return
     */
    public String getServiceReference()
    {
        return serviceReference;
    }

    public DocumentContext getDocumentContext()
    {
        return documentContext;
    }

    public LinkedHashMap getServiceRepresentation()
    {
        return serviceRepresentation;
    }

}
