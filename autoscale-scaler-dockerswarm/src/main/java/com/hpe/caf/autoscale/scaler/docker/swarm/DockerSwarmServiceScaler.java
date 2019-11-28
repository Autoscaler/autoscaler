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
package com.hpe.caf.autoscale.scaler.docker.swarm;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.hpe.caf.api.HealthResult;
import com.hpe.caf.api.HealthStatus;
import com.hpe.caf.api.autoscale.InstanceInfo;
import com.hpe.caf.api.autoscale.ScalerException;
import com.hpe.caf.api.autoscale.ServiceScaler;
import com.hpe.caf.autoscale.DockerSwarmAutoscaleConfiguration;
import com.hpe.caf.autoscale.endpoint.docker.DockerSwarm;
import com.hpe.caf.autoscale.endpoint.HttpClientException;
import com.hpe.caf.autoscale.endpoint.docker.DockerSwarmService;
import static com.hpe.caf.autoscale.json.JsonPathQueryAssistance.queryForValueAsInteger;
import com.jayway.jsonpath.DocumentContext;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.URL;
import java.util.LinkedHashMap;
import java.util.Objects;

/**
 * A ServiceScaler uses a Docker Java client library to make calls to a Docker Swarm server to trigger scaling of a service, and return
 * information on the number of instances of a configured task.
 */
public class DockerSwarmServiceScaler implements ServiceScaler
{
    private final DockerSwarm dockerClient;
    private final int maximumInstances;
    private final URL url;
    private final DockerSwarmAutoscaleConfiguration config;
    
    private static final Logger LOG = LoggerFactory.getLogger(ServiceScaler.class);

    public DockerSwarmServiceScaler(final DockerSwarm dockerClient, final DockerSwarmAutoscaleConfiguration config, final URL url)
    {
        this.dockerClient = Objects.requireNonNull(dockerClient);
        this.config = Objects.requireNonNull(config);
        this.maximumInstances = Math.max(1, config.getMaximumInstances());
        this.url = Objects.requireNonNull(url);
    }

    @Override
    public void scaleUp(final String serviceReference, final int amount)
        throws ScalerException
    {
        try {

            DockerSwarmService serviceItem = getServiceAsObject(serviceReference);
            InstanceInfo instanceInfo = serviceItem.getInstanceInformation();

            int current = instanceInfo.getTotalInstances();
            int target = Math.min(maximumInstances, current + amount);
            if (target > current) {
                LOG.info("Scaling service {} up by {} instances to {} total replicas", serviceReference, amount, target);
                scaleServiceInformation(serviceReference, target, serviceItem);
            }

        } catch (HttpClientException ex) {
            throw new ScalerException("Failed to scale up service " + serviceReference, ex);
        } catch (Exception ex) {
            throw new ScalerException("Failed to scale up service " + serviceReference, ex);
        }
    }

    /**
     * Change the service scaling information whether up or down, just give a requested amount of instances.
     *
     * @param serviceReference
     * @param target
     * @param serviceItem
     * @return
     * @throws JsonProcessingException
     * @throws HttpClientException
     * @throws Exception
     */
    private boolean scaleServiceInformation(final String serviceReference, int target, DockerSwarmService serviceItem)
        throws JsonProcessingException, HttpClientException, Exception
    {
        // We want to get a hold of the entire JSON spec object in order to use it to then update the service
        LinkedHashMap serviceObject = serviceItem.getServiceRepresentation();
        if (!serviceObject.containsKey("Spec")) {
            throw new Exception("Invalid service configuration desont' contain a Specification object.");
        }
        LinkedHashMap specObject = (LinkedHashMap) serviceObject.get("Spec");
        // It must contain an existing specification.
        Objects.requireNonNull(specObject);
        final Integer version = queryForValueAsInteger(serviceItem, 1, "$..Version.Index");
        Objects.requireNonNull(version, "No valid version information found for service: " + serviceReference);
        /**
         * update the replicas count to the new expected value, if any element in our json isn't there add it, as the default is
         * replicated. Except where the type is Mode.global as this isn't scaleable.
         */
        if (!specObject.containsKey("Mode")) {
            // it doesn't contain mode yet, so add it, default is replicated anyway.
            specObject.put("Mode", new LinkedHashMap<>().put("Replicated", new LinkedHashMap<>()));
        }
        LinkedHashMap modeNode = (LinkedHashMap) specObject.get("Mode");
        if (!modeNode.containsKey("Replicated")) {
            // now if it doesn't contain replicated, check quickly if this is a global item, if so it can't be scaled,
            // Check =>  should it be marked with maxInstances of 1 to prevent it entering here.
            if (modeNode.containsKey("Global")) {
                // exit now without scaling.
                LOG.warn(
                    "Service: {} is a global service and cannot be scaled beyond one singleton per swarm node, prevent warning by marking as maxInstances=1");
                return true;
            }
            modeNode.put("Replicated", new LinkedHashMap<>());
        }
        LinkedHashMap replicatedNode = (LinkedHashMap) modeNode.get("Replicated");
        replicatedNode.replace("Replicas", target);
        /**
         * turn the replicated mode node into a JSON string to be supplied to the update method *
         */
        final String updateSpecification = new ObjectMapper().writeValueAsString(specObject);
        LOG.debug("About to update sevice: {} version: {} with updated spec: {}", serviceReference, version, updateSpecification);
        dockerClient.updateService(serviceReference, version, updateSpecification);
        return false;
    }

    @Override
    public void scaleDown(final String serviceReference, final int amount)
        throws ScalerException
    {
        try {
            DockerSwarmService serviceItem = getServiceAsObject(serviceReference);
            InstanceInfo instanceInfo = serviceItem.getInstanceInformation();

            int current = instanceInfo.getTotalInstances();
            if (current == 0) {
                // already at 0 instances, just leave.
                return;
            }

            int target = Math.max(0, current - amount);
            if (target >= current) {
                return;
            }

            LOG.info("Scaling service {} down by {} instances to {} total replicas", serviceReference, amount, target);
            scaleServiceInformation(serviceReference, target, serviceItem);

        } catch (HttpClientException ex) {
            throw new ScalerException("Failed to scale down service " + serviceReference, ex);
        } catch (Exception ex) {
            throw new ScalerException("Failed to scale downservice " + serviceReference, ex);
        }
    }

    /**
     * Get information about the application specified by the service reference 'serviceId'
     *
     * @param serviceReference
     * @return
     * @throws ScalerException
     */
    @Override
    public InstanceInfo getInstanceInfo(final String serviceReference)
        throws ScalerException
    {
        try {
            InstanceInfo instanceInfo = getServiceInstanceInfo(serviceReference);
            LOG.trace("getInstanceInfo for serviceReference: {%s} returned \r\n getTotalInstances: ", instanceInfo.getTotalInstances());
            return instanceInfo;
        } catch (HttpClientException e) {
            throw new ScalerException("Failed to get number of instances of " + serviceReference, e);
        }
    }

    /**
     * Try a trivial connection to the Docker REST endpoint.
     *
     * @return whether a connection to the Docker swarm host works or not
     */
    @Override
    public HealthResult healthCheck()
    {
        // TODO -> Check HTTPS comms with this.
        try (Socket socket = new Socket()) {
            socket.connect(new InetSocketAddress(url.getHost(), url.getPort()), Integer.valueOf(config.getHealthCheckTimeoutInSecs().toString())* 1000);
            return HealthResult.RESULT_HEALTHY;
        } catch (IOException e) {
            LOG.warn("Connection failure to HTTP endpoint", e);
            return new HealthResult(HealthStatus.UNHEALTHY, "Cannot connect to REST endpoint: " + url);
        }
    }

    /**
     * Get information about the specific service.
     *
     * @param serviceReference Service reference to be inspected
     * @return InstanceInformation obtained about a given service.
     * @throws ScalerException
     */
    private InstanceInfo getServiceInstanceInfo(final String serviceReference) throws ScalerException
    {
        try {
            DockerSwarmService serviceItem = getServiceAsObject(serviceReference);
            return serviceItem.getInstanceInformation();
        } catch (Exception ex) {
            throw new ScalerException(ex.getMessage(), ex);
        }

    }

    private DockerSwarmService getServiceAsObject(final String serviceReference) throws ScalerException, HttpClientException
    {
        DocumentContext documentContext = dockerClient.getService(serviceReference);
        LinkedHashMap serviceInformation = documentContext.json();
        if (serviceInformation == null || serviceInformation.isEmpty()) {
            // we have no valid service objects returned.
            throw new ScalerException(
                String.format("Failed to get correct service information for reference: %s.", serviceReference));
        }

        DockerSwarmService requestedService = new DockerSwarmService(documentContext, serviceInformation, serviceReference);
        return requestedService;
    }
}
