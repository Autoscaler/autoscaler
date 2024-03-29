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

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.autoscaler.api.ResourceUtilisation;
import com.github.autoscaler.api.ScalerException;

import java.io.IOException;
import java.io.InputStream;
import java.util.Iterator;
import java.util.Optional;

import jakarta.ws.rs.core.Response;

public final class RabbitSystemResourceMonitor
{
    private volatile double memoryAllocated;
    private volatile Optional<Integer> diskFreeMbOpt = Optional.empty();

    private final RabbitManagementApi rabbitManagementApi;
    private final ObjectMapper mapper = new ObjectMapper();
    private volatile long lastTime;
    private final int resourceQueryRequestFrequency;

    public RabbitSystemResourceMonitor(final RabbitManagementApi rabbitManagementApi,
                                       final int resourceQueryRequestFrequency)
    {
        this.rabbitManagementApi = rabbitManagementApi;
        this.lastTime = 0;
        this.resourceQueryRequestFrequency = resourceQueryRequestFrequency;
    }

    public ResourceUtilisation getCurrentResourceUtilisation() throws ScalerException
    {
        if (shouldIssueRequest()) {
            try {
                final Response response = rabbitManagementApi.getNodeStatus();
                final JsonNode nodeArray = mapper.readTree(response.readEntity(InputStream.class));
                final Iterator<JsonNode> iterator = nodeArray.elements();
                double highestMemUsedInCluster = 0;
                Optional<Integer> lowestDiskFreeMbInClusterOpt = Optional.empty();
                while (iterator.hasNext()) {
                    final JsonNode node = iterator.next();

                    final JsonNode memLimitNode = node.get("mem_limit");
                    final JsonNode memUsedNode = node.get("mem_used");
                    if (memLimitNode != null && memUsedNode != null) { // These will be null if this node is down
                        final long memory_limit = memLimitNode.asLong();
                        final long memory_used = memUsedNode.asLong();
                        final double memPercentage = ((double) memory_used / memory_limit) * 100;
                        highestMemUsedInCluster = memPercentage > highestMemUsedInCluster ? memPercentage : highestMemUsedInCluster;
                    }

                    final JsonNode diskFreeNode = node.get("disk_free");
                    if (diskFreeNode != null) {
                        final long diskFreeBytes = diskFreeNode.asLong();
                        final int diskFreeMb = (int)(diskFreeBytes / 1024 / 1024);
                        if (lowestDiskFreeMbInClusterOpt.isPresent()) {
                            lowestDiskFreeMbInClusterOpt = Optional.of(
                                    diskFreeMb < lowestDiskFreeMbInClusterOpt.get() ? diskFreeMb : lowestDiskFreeMbInClusterOpt.get());
                        } else {
                            lowestDiskFreeMbInClusterOpt = Optional.of(diskFreeMb);
                        }
                    }
                }
                memoryAllocated =  highestMemUsedInCluster;
                diskFreeMbOpt = lowestDiskFreeMbInClusterOpt;
                lastTime = System.currentTimeMillis();
            } catch (final IOException ex) {
                throw new ScalerException("Unable to map response to status request.", ex);
            }
        }
        return new ResourceUtilisation(memoryAllocated, diskFreeMbOpt);
    }

    private boolean shouldIssueRequest()
    {
        if (lastTime == 0) {
            return true;
        }
        return (System.currentTimeMillis() - lastTime) >= (resourceQueryRequestFrequency * 1000);
    }
}
