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
package com.hpe.caf.autoscale.scaler.docker.swarm;

import com.hpe.caf.api.autoscale.InstanceInfo;
import com.hpe.caf.api.autoscale.ScalerException;
import com.hpe.caf.autoscale.DockerSwarmAutoscaleConfiguration;
import com.hpe.caf.autoscale.endpoint.HttpClientException;
import com.hpe.caf.autoscale.endpoint.docker.DockerSwarm;
import com.hpe.caf.autoscale.endpoint.docker.DockerSwarmClient;
import com.jayway.jsonpath.DocumentContext;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.runners.MockitoJUnitRunner;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author Trevor Getty <trevor.getty@hpe.com>
 */
@RunWith(MockitoJUnitRunner.class)
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
        Assert.assertNotNull(document);
        ArrayList<LinkedHashMap> listOfServiceObjects = document.json();

        Assert.assertTrue(listOfServiceObjects.size() > 0);

        // get ID field
        Object firstItem = listOfServiceObjects.get(0);
        if (LinkedHashMap.class.isInstance(firstItem)) {
            String idOfFirstEntry = ((LinkedHashMap) firstItem).get("ID").toString();
            Assert.assertTrue(idOfFirstEntry != null && !idOfFirstEntry.isEmpty());
        }

        // Try getting all ids.
        LinkedList<String> allIds = document.read("$..ID");
        Assert.assertTrue(allIds.size() > 0);
    }

    @Test
    public void TestAutoscalerScaler() throws MalformedURLException, ScalerException
    {

        DockerSwarm dockerClient = buildDockerSwarmClient();
        DockerSwarmAutoscaleConfiguration config = buildDockerConfiguration();

        DockerSwarmServiceScaler scaler = new DockerSwarmServiceScaler(dockerClient, config.getMaximumInstances(), new URL(
                                                                       config.getEndpoint()));
        Assert.assertNotNull(scaler);

        final String serviceReference = getValidServiceId(dockerClient);

        InstanceInfo serviceInformation = scaler.getInstanceInfo(serviceReference);

        // check instance info for a runnin element.
        Assert.assertTrue("Total instances > 0", serviceInformation.getTotalInstances() > 0);
        Assert.assertEquals("Staged instances == 0", 0, serviceInformation.getInstancesStaging());
        Assert.assertEquals("Instances Running == total for docker all the time.", serviceInformation.getTotalInstances(),
                            serviceInformation.getInstancesRunning());
        Assert.assertTrue("No hosts map for docker swarm scaler", serviceInformation.getHosts().isEmpty());
    }

    @Test
    public void TestAutoscalerScaleUp() throws MalformedURLException, ScalerException
    {

        DockerSwarm dockerClient = buildDockerSwarmClient();
        DockerSwarmAutoscaleConfiguration config = buildDockerConfiguration();

        DockerSwarmServiceScaler scaler = new DockerSwarmServiceScaler(dockerClient, config.getMaximumInstances(), new URL(
                                                                       config.getEndpoint()));
        Assert.assertNotNull(scaler);

        final String serviceReference = getValidServiceId(dockerClient);

        InstanceInfo serviceInformation = scaler.getInstanceInfo(serviceReference);

        final int currentAmount = serviceInformation.getTotalInstances();
        final int requestedAmount = 2;
        final int expectedAmount  = Math.max(config.getMaximumInstances(), currentAmount + requestedAmount);
        scaler.scaleUp(serviceReference, requestedAmount);
        
        InstanceInfo serviceInformationNow = scaler.getInstanceInfo(serviceReference);
        
        // check instance info for a runnin element.
        Assert.assertTrue("Total instances should have at least 1 running app", serviceInformationNow.getTotalInstances() > 0);
        Assert.assertEquals("Total instances should now match updated value.", expectedAmount, serviceInformationNow.getTotalInstances());
    }
    
    @Test
    public void TestAutoscalerScaleDown() throws MalformedURLException, ScalerException
    {

        DockerSwarm dockerClient = buildDockerSwarmClient();
        DockerSwarmAutoscaleConfiguration config = buildDockerConfiguration();

        DockerSwarmServiceScaler scaler = new DockerSwarmServiceScaler(dockerClient, config.getMaximumInstances(), new URL(
                                                                       config.getEndpoint()));
        Assert.assertNotNull(scaler);

        final String serviceReference = getValidServiceId(dockerClient);

        InstanceInfo serviceInformation = scaler.getInstanceInfo(serviceReference);

        final int currentAmount = serviceInformation.getTotalInstances();
        final int requestedAmount = 1;
        final int expectedAmount  = Math.max(0, currentAmount - requestedAmount);
        scaler.scaleDown(serviceReference, requestedAmount);
        
        InstanceInfo serviceInformationNow = scaler.getInstanceInfo(serviceReference);
        
        // check instance info for a runnin element.
        Assert.assertEquals("Total instances should now match updated value.", expectedAmount, serviceInformationNow.getTotalInstances());
    }

    private String getValidServiceId(DockerSwarm dockerClient) throws HttpClientException
    {
        /**
         * Get me a valid ID to query a service for. *
         */
        DocumentContext document = dockerClient.getServices();
        // a general list of objects. 
        Assert.assertNotNull(document);
        // Try getting all ids.
        LinkedList<String> allIds = document.read("$..ID");
        Assert.assertTrue("Must have some valid ids", !allIds.isEmpty());
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
        config.setProxyEndpoint("http://getty5:8888");
        config.setTimeoutInSecs(new Long(10));
        config.setMaximumInstances(10);
        return config;
    }
}
