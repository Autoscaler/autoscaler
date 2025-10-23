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
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

import jakarta.ws.rs.core.Response;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class RabbitSystemResourceMonitor
{
    private volatile double memoryAllocated;
    private volatile Optional<Integer> diskFreeMbOpt = Optional.empty();

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
                        final int diskFreeMb = (int)(diskFreeBytes / 1024 / 1024);
                        if (lowestDiskFreeMbInClusterOpt.isPresent()) {
                            lowestDiskFreeMbInClusterOpt = Optional.of(
                                    diskFreeMb < lowestDiskFreeMbInClusterOpt.get() ? diskFreeMb : lowestDiskFreeMbInClusterOpt.get());
                        } else {
                            lowestDiskFreeMbInClusterOpt = Optional.of(diskFreeMb);
                        }
                    }
                }
                ResourceUtilisation utilisation = new ResourceUtilisation(highestMemUsedInCluster, lowestDiskFreeMbInClusterOpt);
                LOG.info("RABBIT UTIL: {}", utilisation);
                if (config.getIsPayloadOffloadingEnabled()) {
                    final var offloadingUtilisation = getOffloadingUtilisation();
                    LOG.info("OFFLOADING UTIL: {}", offloadingUtilisation);
                    final var highestMemoryUsed = Math.max(offloadingUtilisation.getMemoryUsedPercent(), highestMemUsedInCluster);
                    final var lowestDiskFree = List.of(offloadingUtilisation.getDiskFreeMbOpt(), lowestDiskFreeMbInClusterOpt)
                            .stream()
                            .filter(Optional::isPresent)
                            .map(Optional::get)
                            .min(Double::compare);
                    LOG.info("OVERALL UTIL: {}", new ResourceUtilisation(highestMemoryUsed, lowestDiskFree));
                    //utilisation = new ResourceUtilisation(highestMemoryUsed, lowestDiskFree);
                }
                memoryAllocated =  utilisation.getMemoryUsedPercent();
                diskFreeMbOpt = utilisation.getDiskFreeMbOpt();
                lastTime = System.currentTimeMillis();
                return utilisation;
            } catch (final IOException ex) {
                throw new ScalerException("Unable to map response to status request.", ex);
            }
        }
        return new ResourceUtilisation(memoryAllocated, diskFreeMbOpt);
    }

    private ResourceUtilisation getOffloadingUtilisation() throws ScalerException
    {
        try {
            final String datastoreDirectory = config.getDataStoreDirectory();
            final String offloadingDirectory = config.getPayloadOffloadingDirectory();
            final Integer payloadOffloadingMemoryLimitPercent = config.getPayloadOffloadingMemoryLimitPercent();
            final FileStore datastore = Files.getFileStore(Paths.get(datastoreDirectory));

            // the size, in bytes, of the file store
            // DDD effectively mem_limit?, this would be represent N% of /etc/store available memory
            //  here we would need to apply a new cfg'd env var.
            final double offloadingMemoryLimitPercent = payloadOffloadingMemoryLimitPercent / 100d;
            final double memoryLimit = (datastore.getTotalSpace() * offloadingMemoryLimitPercent);

            // the number of unallocated bytes in the file store
            // DDD effectively disk_free?
            final var diskFreeMb = Math.toIntExact((datastore.getUnallocatedSpace() / MB_IN_BYTES));

            // the total size of offloaded files in the offloading directory
            // DDD effectively mem_used?
            final double memoryUsed = (offloadedSize(Paths.get(datastoreDirectory, offloadingDirectory)) / memoryLimit) * 100;

            return new ResourceUtilisation(memoryUsed, Optional.of(diskFreeMb));
        } catch (final Exception ex) {
            throw new ScalerException("Unable to load datastore resouce utilization.", ex);
        }
    }

    private static long offloadedSize(final Path path)
    {
        try {
            if (Files.isRegularFile(path)) {
                return Files.size(path);
            }
            try (final Stream<Path> pathStream = Files.list(path)) {
                return pathStream.mapToLong(RabbitSystemResourceMonitor::offloadedSize).sum();
            }
        } catch (final IOException | SecurityException e) {
            return 0L;
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
