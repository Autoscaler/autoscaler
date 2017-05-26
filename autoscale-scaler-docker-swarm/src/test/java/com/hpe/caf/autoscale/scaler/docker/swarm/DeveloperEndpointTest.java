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

import com.hpe.caf.autoscale.DockerSwarmAutoscaleConfiguration;
import com.hpe.caf.autoscale.scaler.endpoint.docker.DockerSwarm;
import com.hpe.caf.autoscale.scaler.endpoint.docker.DockerSwarmClient;
import com.hpe.caf.autoscale.scaler.endpoint.HttpClientSupport.ObjectList;
import com.jayway.jsonpath.DocumentContext;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.runners.MockitoJUnitRunner;

/**
 *
 * @author Trevor Getty <trevor.getty@hpe.com>
 */
@RunWith(MockitoJUnitRunner.class)
public class DeveloperEndpointTest
{

    @Test
    public void TestEndpointGetServices()
    {

        DockerSwarmAutoscaleConfiguration config = new DockerSwarmAutoscaleConfiguration();

        config.setEndpoint("http://192.168.56.10:2375");
        config.setProxyEndpoint("http://getty5:8888");
        config.setTimeoutInSecs(new Long(10));

        DockerSwarm dockerClient = DockerSwarmClient.getInstance(config);
        DocumentContext document = dockerClient.getServices();
        
        // a general list of objects. 
        Assert.assertNotNull(document);
        ArrayList<Object> listOfServiceObjects = document.json();
        
        Assert.assertTrue(listOfServiceObjects.size()>0);
        
        // get ID field
        Object firstItem = listOfServiceObjects.get(0);
        if ( LinkedHashMap.class.isInstance(firstItem))
        {
            String idOfFirstEntry = ((LinkedHashMap)firstItem).get("ID").toString();
            Assert.assertTrue(idOfFirstEntry != null && !idOfFirstEntry.isEmpty());
        }
        
        // Try getting all ids.
        LinkedList<String> allIds = document.read("$..ID");
        Assert.assertTrue(allIds.size() > 0);
            //Assert.assertTrue("Object list item, contains a field called ID", (LinkedHashMap)().containsKey("ID"));    
        
                
    }
}
