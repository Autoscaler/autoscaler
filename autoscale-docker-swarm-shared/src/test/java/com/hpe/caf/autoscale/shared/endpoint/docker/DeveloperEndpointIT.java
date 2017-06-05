package com.hpe.caf.autoscale.shared.endpoint.docker;

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
import com.hpe.caf.autoscale.DockerSwarmAutoscaleConfiguration;
import com.hpe.caf.autoscale.endpoint.docker.DockerSwarm;
import com.hpe.caf.autoscale.endpoint.docker.DockerSwarmClient;
import com.hpe.caf.autoscale.endpoint.docker.DockerSwarmFilters;
import static com.hpe.caf.autoscale.endpoint.docker.DockerSwarmFilters.buildServiceFilter;
import com.jayway.jsonpath.Configuration;
import com.jayway.jsonpath.DocumentContext;
import static com.jayway.jsonpath.JsonPath.using;
import com.jayway.jsonpath.Option;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.Before;
import org.mockito.runners.MockitoJUnitRunner;

/**
 *
 * @author Trevor Getty <trevor.getty@hpe.com>
 */
@RunWith(MockitoJUnitRunner.class)
public class DeveloperEndpointIT
{
    private DockerSwarmAutoscaleConfiguration config;

    @Before
    public void setUp()
    {
        this.config = buildConfiguration();
    }

    @Test
    public void TestEndpointGetServices()
    {

        DockerSwarm dockerClient = buildDockerSwarmClient();
        DocumentContext document = dockerClient.getServices();

        // a general list of objects. 
        Assert.assertNotNull(document);        
        ArrayList<LinkedHashMap> listOfServiceObjects = document.json();

        Assert.assertTrue(listOfServiceObjects.size() > 0);

        // get ID field, using direct properties, and then a JSONPath query, for illustration
        Object firstItem = listOfServiceObjects.get(0);
        if (LinkedHashMap.class.isInstance(firstItem)) {
            String idOfFirstEntry = ((LinkedHashMap) firstItem).get("ID").toString();
            Assert.assertTrue(idOfFirstEntry != null && !idOfFirstEntry.isEmpty());
        }

        // Try getting all ids.
        LinkedList<String> allIds = document.read("$..ID");

        Assert.assertTrue(allIds.size() > 0);
        //Assert.assertTrue("Object list item, contains a field called ID", (LinkedHashMap)().containsKey("ID"));    
    }

    @Test
    public void TestEndpointGetServicesByLabel()
    {
        DockerSwarm dockerClient = buildDockerSwarmClient();

        final String myServiceStackName = "jobservicedemo";

        final String filterByLabel = buildServiceFilter(DockerSwarmFilters.ServiceFilterByType.LABEL,
                                                        DockerSwarmFilters.FilterLabelKeys.DOCKER_STACK, myServiceStackName);

        DocumentContext document = dockerClient.getServicesFiltered(filterByLabel);

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

//        tryGetMePaths(document, "$..Spec.TaskTemplate.ContainerSpec.Labels");
//        tryQuery(document, "$..Spec.TaskTemplate.ContainerSpec.Labels");
        Assert.assertTrue(allIds.size() > 0);
        Assert.assertEquals("expect jobservicedemo to contain a filtered list of 6 elements", 6, allIds.size());
        //Assert.assertTrue("Object list item, contains a field called ID", (LinkedHashMap)().containsKey("ID"));    

    }

    @Test
    public void TestEndpointInspectService()
    {
        DockerSwarm dockerClient = buildDockerSwarmClient();

        final String myServiceStackName = "jobservicedemo";
        final String filterByLabel = buildServiceFilter(DockerSwarmFilters.ServiceFilterByType.LABEL,
                                                        DockerSwarmFilters.FilterLabelKeys.DOCKER_STACK, myServiceStackName);
        final DocumentContext document = dockerClient.getServicesFiltered(filterByLabel);

        // a general list of objects. 
        Assert.assertNotNull(document);

        // Try getting all ids.
        final LinkedList<String> allIds = document.read("$..ID");

        final String findThisId = allIds.get(0);
        System.out.println("About to find service by ID: " + findThisId);

        final String findThisDocumentInfoQuery = String.format("$[?(@.ID == '%s')]", findThisId);
        tryQuery(document, findThisDocumentInfoQuery);

        final DocumentContext singleServiceDocument = dockerClient.getService(findThisId);

        // a general list of objects. 
        Assert.assertNotNull(singleServiceDocument);

        // Try getting all ids.
        final LinkedList<String> queryIds = singleServiceDocument.read("$..ID");

        Assert.assertEquals("Should have only 1 service returned by this id", 1, queryIds.size());
        Assert.assertEquals("Service ids must match.", findThisId, queryIds.get(0));
    }

    private void tryGetMePaths(DocumentContext document, final String queryString)
    {
        try {
            Configuration conf = Configuration.builder().options(Option.AS_PATH_LIST).build();

            final String jsonString = document.jsonString();
            List<String> pathList = using(conf).parse(jsonString).read(queryString);

            Object spec = document.read(queryString);

            System.out.println("Query: " + queryString);
        } catch (Exception ex) {
            ex.printStackTrace();
        }

    }

    private void tryQuery(DocumentContext document, final String queryString)
    {
        try {
            Object spec = document.read(queryString);

            System.out.println("Query: " + queryString);
        } catch (Exception ex) {
            ex.printStackTrace();
        }

    }

    private DockerSwarm buildDockerSwarmClient()
    {
        return DockerSwarmClient.getInstance(config);
    }

    private static DockerSwarmAutoscaleConfiguration buildConfiguration()
    {
        DockerSwarmAutoscaleConfiguration config = new DockerSwarmAutoscaleConfiguration();
        config.setEndpoint("http://192.168.56.10:2375");
        config.setProxyEndpoint("http://getty5:8888");
        config.setTimeoutInSecs(new Long(10));
        return config;
    }
}
