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
package com.github.autoscaler.workload.rabbit;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.Optional;

import org.junit.Assert;
import org.junit.Test;
import org.mockito.Mockito;

import com.github.autoscaler.api.ResourceUtilisation;

import javax.ws.rs.core.Response;

public class RabbitSystemResourceMonitorTest {

    @Test
    public void testReturnsHighestMemLoadAndLowestDiskFreeInCluster() throws Exception {
        // Arrange
        final String responseBody = "[" +
                "{\"mem_limit\": 100000000, \"mem_used\": 50000000, \"disk_free\": 107374182400}," +  // Highest memory used in cluster
                "{\"mem_limit\": 200000000, \"mem_used\": 0, \"disk_free\": 104857600}" +             // Lowest disk space free in cluster
                "]";
        final InputStream responseStream = new ByteArrayInputStream(responseBody.getBytes());

        final Response mockResponse = Mockito.mock(Response.class);
        Mockito.when(mockResponse.readEntity(InputStream.class)).thenReturn(responseStream);

        RabbitManagementApi mockRabbitManagementApi =
                Mockito.mock(RabbitManagementApi.class);
        Mockito.when(mockRabbitManagementApi.getNodeStatus()).thenReturn(mockResponse);

        // Act
        final RabbitSystemResourceMonitor rabbitSystemResourceMonitor = new RabbitSystemResourceMonitor(mockRabbitManagementApi, 60);
        final ResourceUtilisation resourceUtilisation = rabbitSystemResourceMonitor.getCurrentResourceUtilisation();

        // Assert
        Assert.assertNotNull(resourceUtilisation);
        Assert.assertEquals("Expected 50% memory used (highest in cluster)",
                50.0, resourceUtilisation.getMemoryUsedPercent(), 0.01);
        Assert.assertEquals("Expected 100MB of disk space free (lowest in cluster)",
                Optional.of(100), resourceUtilisation.getDiskFreeMbOpt());
    }

    @Test
    public void testHandlesMissingPropertiesInRabbitMqResponse() throws Exception {
        // Arrange
        final String responseBody = "[{}]"; // No properties in response, this can happen if the node is down
        final InputStream responseStream = new ByteArrayInputStream(responseBody.getBytes());

        final Response mockResponse = Mockito.mock(Response.class);
        Mockito.when(mockResponse.readEntity(InputStream.class)).thenReturn(responseStream);

        RabbitManagementApi mockRabbitManagementApi =
                Mockito.mock(RabbitManagementApi.class);
        Mockito.when(mockRabbitManagementApi.getNodeStatus()).thenReturn(mockResponse);

        // Act
        final RabbitSystemResourceMonitor rabbitSystemResourceMonitor = new RabbitSystemResourceMonitor(mockRabbitManagementApi, 60);
        final ResourceUtilisation resourceUtilisation = rabbitSystemResourceMonitor.getCurrentResourceUtilisation();

        // Assert
        Assert.assertNotNull(resourceUtilisation);
        Assert.assertEquals("Expected 0% memory used as the RabbitMQ response did not contain the mem_limit and mem_used properties",
                0.0, resourceUtilisation.getMemoryUsedPercent(), 0.01);
        Assert.assertEquals("Expected unknown disk space free as the RabbitMQ response did not contain a disk_free property",
                Optional.empty(), resourceUtilisation.getDiskFreeMbOpt());
    }
}
