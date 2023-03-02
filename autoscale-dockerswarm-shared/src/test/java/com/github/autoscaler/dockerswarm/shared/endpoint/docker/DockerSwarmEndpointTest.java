/*
 * Copyright 2015-2023 Open Text.
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
package com.github.autoscaler.dockerswarm.shared.endpoint.docker;

import com.jayway.jsonpath.DocumentContext;
import com.jayway.jsonpath.JsonPath;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.apache.commons.io.IOUtils;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.runners.MockitoJUnitRunner;

/**
 *
 * @author Trevor Getty <trevor.getty@microfocus.com>
 */
@RunWith(MockitoJUnitRunner.class)
public class DockerSwarmEndpointTest
{
    @Mock
    DockerSwarm dockerClient;
    
    @Test
    public void TestEndpointGetServices()
    {
        // DockerSwarm dockerClient = buildDockerSwarmClient();
        
        try (InputStream is = Thread.currentThread().getContextClassLoader().getResourceAsStream("jsonServicesResponse.json");) {
            String jsonContent = IOUtils.toString(is);
            DocumentContext documentContext = JsonPath.parse(jsonContent);
            Mockito.when(dockerClient.getServices()).thenReturn(documentContext);
            
        } catch (IOException ex) {
            Logger.getLogger(DockerSwarmEndpointTest.class.getName()).log(Level.SEVERE, null, ex);
            throw new RuntimeException(ex);
        }
        
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
        LinkedList<String> allIds = document.read("$..ID", LinkedList.class);
        Assert.assertTrue(allIds.size() > 0);
        Assert.assertEquals("Service IDs match", "1go9020n17eyhufay1nbponlu", allIds.stream().findFirst().get());    
    }
}
