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
package com.github.autoscaler.scaler.dockerswarm;

import com.github.autoscaler.api.InstanceInfo;
import com.github.autoscaler.api.ScalerException;
import com.github.autoscaler.dockerswarm.shared.DockerSwarmAutoscaleConfiguration;
import com.github.autoscaler.dockerswarm.shared.endpoint.HttpClientException;
import com.github.autoscaler.dockerswarm.shared.endpoint.docker.DockerSwarm;
import com.github.autoscaler.dockerswarm.shared.endpoint.docker.DockerSwarmClient;
import com.jayway.jsonpath.DocumentContext;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedList;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author Trevor Getty <trevor.getty@microfocus.com>
 */
@ExtendWith(MockitoExtension.class)
public class DockerSwarmServiceScalerIT
{

    // Develeper integration testing, the tests are valid, but the docker swarm configuration needs changed per machine
    // until we get setup correctly - See CAF
    private final Logger LOG = LoggerFactory.getLogger(DockerSwarmServiceScalerIT.class);

    @Test
    public void TestEndpointGetServices()
    {
        LOG.info("Starting test");

        DockerSwarm dockerClient = buildDockerSwarmClient();
        DocumentContext document = dockerClient.getServices();

        // a general list of objects. 
        assertNotNull(document);
        ArrayList<LinkedHashMap> listOfServiceObjects = document.json();

        assertTrue(listOfServiceObjects.size() > 0);

        // get ID field
        Object firstItem = listOfServiceObjects.get(0);
        if (LinkedHashMap.class.isInstance(firstItem)) {
            String idOfFirstEntry = ((LinkedHashMap) firstItem).get("ID").toString();
            assertTrue(idOfFirstEntry != null && !idOfFirstEntry.isEmpty());
        }

        // Try getting all ids.
        LinkedList<String> allIds = document.read("$..ID");
        assertTrue(allIds.size() > 0);
    }

    @Test
    public void TestAutoscalerScaler() throws MalformedURLException, ScalerException
    {

        DockerSwarm dockerClient = buildDockerSwarmClient();
        DockerSwarmAutoscaleConfiguration config = buildDockerConfiguration();

        DockerSwarmServiceScaler scaler = new DockerSwarmServiceScaler(dockerClient, config, new URL(
                                                                       config.getEndpoint()));
        assertNotNull(scaler);

        final String serviceReference = getValidServiceId(dockerClient);

        InstanceInfo serviceInformation = scaler.getInstanceInfo(serviceReference);

        // check instance info for a runnin element.
        assertTrue(serviceInformation.getTotalRunningAndStageInstances() > 0, "Total instances > 0");
        assertEquals(0, serviceInformation.getInstancesStaging(), "Staged instances == 0");
        assertEquals(serviceInformation.getTotalRunningAndStageInstances(), serviceInformation.getInstancesRunning(),
                "Instances Running == total for docker all the time.");
        assertTrue(serviceInformation.getHosts().isEmpty(), "No hosts map for docker swarm scaler");
    }

    @Test
    public void TestAutoscalerScaleUp() throws MalformedURLException, ScalerException
    {

        DockerSwarm dockerClient = buildDockerSwarmClient();
        DockerSwarmAutoscaleConfiguration config = buildDockerConfiguration();

        DockerSwarmServiceScaler scaler = new DockerSwarmServiceScaler(dockerClient, config, new URL(
                                                                       config.getEndpoint()));
        assertNotNull(scaler);

        final String serviceReference = getValidServiceId(dockerClient);

        InstanceInfo serviceInformation = scaler.getInstanceInfo(serviceReference);

        final int currentAmount = serviceInformation.getTotalRunningAndStageInstances();
        final int requestedAmount = 2;
        final int expectedAmount  = Math.max(config.getMaximumInstances(), currentAmount + requestedAmount);
        scaler.scaleUp(serviceReference, requestedAmount);
        
        InstanceInfo serviceInformationNow = scaler.getInstanceInfo(serviceReference);
        
        // check instance info for a runnin element.
        assertTrue(serviceInformationNow.getTotalRunningAndStageInstances() > 0,
                "Total instances should have at least 1 running app");
        assertEquals(expectedAmount, serviceInformationNow.getTotalRunningAndStageInstances(),
                "Total instances should now match updated value.");
    }
    
    @Test
    public void TestAutoscalerScaleDown() throws MalformedURLException, ScalerException
    {

        DockerSwarm dockerClient = buildDockerSwarmClient();
        DockerSwarmAutoscaleConfiguration config = buildDockerConfiguration();

        DockerSwarmServiceScaler scaler = new DockerSwarmServiceScaler(dockerClient, config, new URL(
                                                                       config.getEndpoint()));
        assertNotNull(scaler);

        final String serviceReference = getValidServiceId(dockerClient);

        InstanceInfo serviceInformation = scaler.getInstanceInfo(serviceReference);

        final int currentAmount = serviceInformation.getTotalRunningAndStageInstances();
        final int requestedAmount = 1;
        final int expectedAmount  = Math.max(0, currentAmount - requestedAmount);
        scaler.scaleDown(serviceReference, requestedAmount);
        
        InstanceInfo serviceInformationNow = scaler.getInstanceInfo(serviceReference);
        
        // check instance info for a runnin element.
        assertEquals(expectedAmount, serviceInformationNow.getTotalRunningAndStageInstances(),
                "Total instances should now match updated value.");
    }

    private String getValidServiceId(DockerSwarm dockerClient) throws HttpClientException
    {
        /**
         * Get me a valid ID to query a service for. *
         */
        DocumentContext document = dockerClient.getServices();
        // a general list of objects. 
        assertNotNull(document);
        // Try getting all ids.
        LinkedList<String> allIds = document.read("$..ID");
        assertFalse(allIds.isEmpty(), "Must have some valid ids");
        final String serviceReference = allIds.get(0);
        return serviceReference;
    }

    private DockerSwarm buildDockerSwarmClient()
    {
        DockerSwarmAutoscaleConfiguration config = buildDockerConfiguration();

        return DockerSwarmClient.getInstance(config);
    }

    private DockerSwarmAutoscaleConfiguration buildDockerConfiguration()
    {
        DockerSwarmAutoscaleConfiguration config = new DockerSwarmAutoscaleConfiguration();
        config.setEndpoint("http://192.168.56.10:2375");
        config.setTimeoutInSecs(Long.valueOf(10));
        config.setMaximumInstances(10);
        return config;
    }
}
