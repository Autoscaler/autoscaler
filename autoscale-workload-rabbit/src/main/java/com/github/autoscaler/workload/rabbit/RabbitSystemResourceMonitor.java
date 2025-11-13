/*
 * Copyright 2015-2025 Open Text.
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
import java.nio.file.FileStore;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Iterator;
import java.util.Optional;

import jakarta.ws.rs.core.Response;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class RabbitSystemResourceMonitor
{
    private volatile double memoryAllocated;
    private volatile Optional<Integer> rabbitDiskFreeMbOpt = Optional.empty();
    private volatile Optional<Integer> offloadingDiskFreeMbOpt = Optional.empty();

    private final RabbitManagementApi rabbitManagementApi;
    private final RabbitWorkloadAnalyserConfiguration config;
    private final ObjectMapper mapper = new ObjectMapper();
    private volatile long lastTime;
    private final int resourceQueryRequestFrequency;

    private static final Logger LOG = LoggerFactory.getLogger(RabbitSystemResourceMonitor.class);

    private static final int MB_IN_BYTES  = 1_048_576;

    public RabbitSystemResourceMonitor(final RabbitManagementApi rabbitManagementApi,
                                       final RabbitWorkloadAnalyserConfiguration config)
    {
        this.rabbitManagementApi = rabbitManagementApi;
        this.lastTime = 0;
        this.config = config;
        this.resourceQueryRequestFrequency = config.getResourceQueryRequestFrequency();
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
                        final int diskFreeMb = (int) (diskFreeBytes / 1024 / 1024);
                        if (lowestDiskFreeMbInClusterOpt.isPresent()) {
                            lowestDiskFreeMbInClusterOpt = Optional.of(
                                    diskFreeMb < lowestDiskFreeMbInClusterOpt.get() ? diskFreeMb : lowestDiskFreeMbInClusterOpt.get());
                        } else {
                            lowestDiskFreeMbInClusterOpt = Optional.of(diskFreeMb);
                        }
                    }
                }
                memoryAllocated = highestMemUsedInCluster;
                rabbitDiskFreeMbOpt = lowestDiskFreeMbInClusterOpt;
                if (config.getIsPayloadOffloadingEnabled()) {
                    offloadingDiskFreeMbOpt = getOffloadingDiskFreeMb();
                }
                lastTime = System.currentTimeMillis();
            } catch (final IOException ex) {
                throw new ScalerException("Unable to map response to status request.", ex);
            }
        }
        return new ResourceUtilisation(memoryAllocated, rabbitDiskFreeMbOpt, offloadingDiskFreeMbOpt);
    }

    private Optional<Integer> getOffloadingDiskFreeMb() throws ScalerException
    {
        final String offloadingDir = config.getPayloadOffloadingDirectory();
        if (offloadingDir == null || offloadingDir.isEmpty()) {
            throw new ScalerException("Payload offloading directory is not configured.");
        }

        if (!Files.exists(Paths.get(offloadingDir))) {
            LOG.debug("Payload offloading directory {} does not exist.",  offloadingDir);
            return Optional.empty();
        }
        
        try {
            LOG.debug("Checking offloading of disk free mb: {}", offloadingDir);
            final FileStore filestore = Files.getFileStore(Paths.get(config.getPayloadOffloadingDirectory()));

            final var unallocatedSpaceBytes = filestore.getUsableSpace();

            return Optional.of(Math.toIntExact(unallocatedSpaceBytes / MB_IN_BYTES));
        } catch (final Exception ex) {
            throw new ScalerException("Unable to load datastore resource utilization.", ex);
        }
    }

    private boolean shouldIssueRequest()
    {
        if (lastTime == 0) {
            return true;
        }
        return (System.currentTimeMillis() - lastTime) >= (resourceQueryRequestFrequency * 1000);
    }
}
