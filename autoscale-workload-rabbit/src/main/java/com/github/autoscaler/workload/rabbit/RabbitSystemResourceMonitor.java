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

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.FileStore;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;

import jakarta.ws.rs.core.Response;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static com.github.autoscaler.api.ResourceUtilisationSource.OFFLOADING;
import static com.github.autoscaler.api.ResourceUtilisationSource.RABBIT_MQ;

public final class RabbitSystemResourceMonitor
{
    private volatile double memoryAllocated;
    private volatile Optional<Integer> diskFreeMbOpt = Optional.empty();
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

    public List<ResourceUtilisation> getCurrentResourceUtilisation() throws ScalerException
    {
        final List<ResourceUtilisation> resourceUtilisations = new ArrayList<>();
        if (shouldIssueRequest()) {
            final ResourceUtilisation rabbitUtil = getRabbitCurrentResourceUtilisation();
            LOG.info("{}", rabbitUtil);
            resourceUtilisations.add(rabbitUtil);
            if (config.getIsPayloadOffloadingEnabled()) {
                final ResourceUtilisation offloadingUtil = getOffloadingCurrentResourceUtilisation();
                LOG.info("{}", offloadingUtil);
                resourceUtilisations.add(offloadingUtil);

                getEtcStore();
            }
            lastTime = System.currentTimeMillis();
        } else {
            resourceUtilisations.add(new ResourceUtilisation(RABBIT_MQ, memoryAllocated, diskFreeMbOpt));
            if (config.getIsPayloadOffloadingEnabled()) {
                resourceUtilisations.add(new ResourceUtilisation(OFFLOADING, 0, offloadingDiskFreeMbOpt));
            }
        }
        return resourceUtilisations;
    }

    private ResourceUtilisation getRabbitCurrentResourceUtilisation() throws ScalerException
    {
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
            diskFreeMbOpt = lowestDiskFreeMbInClusterOpt;
            return new ResourceUtilisation(RABBIT_MQ, highestMemUsedInCluster, lowestDiskFreeMbInClusterOpt);
        } catch (final IOException ex) {
            throw new ScalerException("Unable to map response to status request.", ex);
        }
    }

    private ResourceUtilisation getOffloadingCurrentResourceUtilisation() throws ScalerException
    {
        try {
            final FileStore filestore = Files.getFileStore(Paths.get(config.getPayloadOffloadingDirectory()));

            final var unallocatedSpaceBytes = filestore.getUsableSpace();

            final var diskFreeMbOpt = Optional.of(Math.toIntExact(unallocatedSpaceBytes / MB_IN_BYTES));

            offloadingDiskFreeMbOpt = diskFreeMbOpt;
            return new ResourceUtilisation(OFFLOADING, 0, diskFreeMbOpt);
        } catch (final Exception ex) {
            throw new ScalerException("Unable to load datastore resource utilization.", ex);
        }
    }

    private void getEtcStore() throws ScalerException
    {
        try {
            File etcStore = new File("/etc/store");

            double freeSpace = etcStore.getFreeSpace();
            double usableSpace = etcStore.getUsableSpace();
            double totalSpace = etcStore.getTotalSpace();
            double oneGB = 1024 * 1024 * 1024;

            NumberFormat numberFormat = NumberFormat.getInstance();
            numberFormat.setMaximumFractionDigits(2);
            LOG.info("Free Space: " +
                    numberFormat.format(freeSpace/oneGB) + " GB");
            LOG.info("Usable Space: " +
                    numberFormat.format(usableSpace/oneGB) + " GB");
            LOG.info("Total Space: " +
                    numberFormat.format(totalSpace/oneGB) + " GB");
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
