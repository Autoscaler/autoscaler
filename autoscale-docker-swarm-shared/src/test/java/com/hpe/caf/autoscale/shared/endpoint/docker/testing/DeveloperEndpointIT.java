package com.hpe.caf.autoscale.shared.endpoint.docker.testing;

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
import com.jayway.jsonpath.Configuration;
import com.jayway.jsonpath.DocumentContext;
import static com.jayway.jsonpath.JsonPath.using;
import com.jayway.jsonpath.Option;
import com.jayway.jsonpath.Predicate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.runners.MockitoJUnitRunner;

/**
 *
 * @author Trevor Getty <trevor.getty@hpe.com>
 */
@RunWith(MockitoJUnitRunner.class)
public class DeveloperEndpointIT
{

    @Test
    public void TestEndpointGetServices()
    {

        DockerSwarm dockerClient = buildDockerSwarmClient();
        DocumentContext document = dockerClient.getServices();

        // a general list of objects. 
        Assert.assertNotNull(document);
        ArrayList<Object> listOfServiceObjects = document.json();

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
        //Assert.assertTrue("Object list item, contains a field called ID", (LinkedHashMap)().containsKey("ID"));    
    }

    @Test
    public void TestEndpointGetServicesByLabel()
    {
        DockerSwarm dockerClient = buildDockerSwarmClient();

        final String myServiceStackName = "jobservicedemo";

        String filterByLabel = dockerClient.buildServiceFilter("label", "com.docker.stack.namespace", myServiceStackName);

        DocumentContext document = dockerClient.getServicesFiltered(filterByLabel);

        // a general list of objects. 
        Assert.assertNotNull(document);

        ArrayList<Object> listOfServiceObjects = document.json();

        Assert.assertTrue(listOfServiceObjects.size() > 0);

        // get ID field
        Object firstItem = listOfServiceObjects.get(0);
        if (LinkedHashMap.class.isInstance(firstItem)) {
            String idOfFirstEntry = ((LinkedHashMap) firstItem).get("ID").toString();
            Assert.assertTrue(idOfFirstEntry != null && !idOfFirstEntry.isEmpty());
        }

        // Try getting all ids.
        LinkedList<String> allIds = document.read("$..ID");

        tryGetMePaths(document, "$..Spec.TaskTemplate.ContainerSpec.Labels");
        tryQuery(document, "$..Spec.TaskTemplate.ContainerSpec.Labels");
        tryQuery(document, "$..length()");  
        tryQuery(document, "$[0].ID");
        tryQuery(document, "$..ID");
        
        
        tryQuery(document, "$[?(@.ID == 'cl39i406exd1ehapv7izk7ptx')]");
        
        
        
        Assert.assertTrue(allIds.size() > 0);
        Assert.assertEquals("expect jobservicedemo to contain a filtered list of 6 elements", 6, allIds.size());
        //Assert.assertTrue("Object list item, contains a field called ID", (LinkedHashMap)().containsKey("ID"));    

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
        DockerSwarmAutoscaleConfiguration config = new DockerSwarmAutoscaleConfiguration();
        config.setEndpoint("http://192.168.56.10:2375");
        config.setProxyEndpoint("http://getty5:8888");
        config.setTimeoutInSecs(new Long(10));

        return DockerSwarmClient.getInstance(config);
    }
}
