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
package com.github.autoscaler.source.dockerswarm;

import com.github.autoscaler.api.ScalerException;
import com.github.autoscaler.api.ScalingConfiguration;
import com.github.autoscaler.dockerswarm.shared.DockerSwarmAutoscaleConfiguration;
import com.github.autoscaler.dockerswarm.shared.endpoint.docker.DockerSwarm;
import com.github.autoscaler.dockerswarm.shared.endpoint.docker.DockerSwarmClient;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 *
 * @author Trevor Getty <trevor.getty@microfocus.com>
 */
@ExtendWith(MockitoExtension.class)
public class DeveloperEndpointIT
{
    // Change items here to match your env, until we get a real docker swarm set of integration tests
    private DockerSwarmAutoscaleConfiguration buildDockerConfiguration()
    {
        DockerSwarmAutoscaleConfiguration config = new DockerSwarmAutoscaleConfiguration();
        config.setEndpoint("http://192.168.56.10:2375");
        config.setTimeoutInSecs(Long.valueOf(10));
        config.setStackId("jobservicedemo");
        return config;
    }

    @Test
    public void TestDockerSourceServices() throws MalformedURLException, ScalerException
    {

        DockerSwarm dockerClient = buildDockerSwarmClient();
        DockerSwarmAutoscaleConfiguration config = buildDockerConfiguration();

        DockerSwarmServiceSource source = new DockerSwarmServiceSource(config, new URL(config.getEndpoint()));

        Set<ScalingConfiguration> scalingItems = source.getServices();

        // a general list of objects. 
        assertNotNull(scalingItems);
        assertTrue(scalingItems.size() > 0);

        // get ID field
        for (ScalingConfiguration scaledItem : scalingItems) {

            assertFalse(scaledItem.getId().isEmpty());
            assertEquals(30, scaledItem.getInterval());

        }
    }

    private DockerSwarm buildDockerSwarmClient()
    {
        DockerSwarmAutoscaleConfiguration config = buildDockerConfiguration();

        return DockerSwarmClient.getInstance(config);
    }

}
