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
package com.github.autoscaler.source.dockerswarm;

import com.github.autoscaler.api.ScalerException;
import com.github.autoscaler.api.ScalingConfiguration;
import com.github.autoscaler.api.ServiceSource;
import com.github.autoscaler.dockerswarm.shared.DockerSwarmAutoscaleConfiguration;
import com.github.autoscaler.dockerswarm.shared.endpoint.HttpClientException;
import com.github.autoscaler.dockerswarm.shared.endpoint.docker.DockerSwarm;
import com.github.autoscaler.dockerswarm.shared.endpoint.docker.DockerSwarmApp;
import com.github.autoscaler.dockerswarm.shared.endpoint.docker.DockerSwarmClient;
import com.github.autoscaler.dockerswarm.shared.endpoint.docker.DockerSwarmFilters;
import static com.github.autoscaler.dockerswarm.shared.endpoint.docker.DockerSwarmFilters.buildServiceFilter;
import com.hpe.caf.api.HealthResult;
import com.hpe.caf.api.HealthStatus;
import com.jayway.jsonpath.DocumentContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * A DockerSwarmServiceSource uses HTTP Rest calls to communicate with a Docker Swarm endpoint and retrieve information about running
 * tasks. It will create ScalingConfiguration objects based upon this data.
 */
public class DockerSwarmServiceSource implements ServiceSource
{
    private final DockerSwarm dockerSwarm;
    private final URL url;
    private final String stackPath;
    private final DockerSwarmAutoscaleConfiguration config;
    private static final Logger LOG = LoggerFactory.getLogger(DockerSwarmServiceSource.class);

    public DockerSwarmServiceSource(final DockerSwarmAutoscaleConfiguration config, final URL url)
    {
        this.config = Objects.requireNonNull(config);
        this.dockerSwarm = DockerSwarmClient.getInstance(config);
        Objects.requireNonNull(dockerSwarm);
        
        this.stackPath = Objects.requireNonNull(config.getStackId());
        this.url = Objects.requireNonNull(url);
    }

    /**
     * Call to Docker Swarm and find all running tasks. Create ScalingConfiguration objects based upon the data Docker Swarm returns. If
     * the service has the env variable specifying its app name, this will be set as the application owner for that service, but the
     * application owner label can override this.
     *
     * @return available ScalingConfiguration services that have a workload metric
     */
    @Override
    public Set<ScalingConfiguration> getServices()
        throws ScalerException
    {
        Set<ScalingConfiguration> ret = new HashSet<>();
        for (DockerSwarmApp app : getDockerSwarmApps()) {
            ScalingConfiguration sv = new ScalingConfiguration();
            LOG.debug("Checking Docker Swarm service: {}", app.getId());
            sv.setId(app.getId());
            Map<String, String> labels = app.getLabels();
            if (labels.containsKey(ScalingConfiguration.KEY_WORKLOAD_METRIC)) {
                handleStrings(sv, labels);
                handleIntegers(sv, labels);
                LOG.debug("Returning scaling service: {}", sv);
                ret.add(sv);
            } else {
                LOG.debug("Skipping service {}, workload metric {} not supported", app.getId(), labels.get(
                          ScalingConfiguration.KEY_WORKLOAD_METRIC));
            }
        }
        return ret;
    }

    private Collection<DockerSwarmApp> getDockerSwarmApps()
        throws ScalerException
    {
        try {
            Collection<DockerSwarmApp> apps = new ArrayList<>();
            String[] stackPaths;
            if (stackPath.contains(",")) {
                LOG.info("Multiple Docker Swarm stacks detected: {} splitting on ','.", stackPath);
                stackPaths = stackPath.split(",");
            } else {
                stackPaths = new String[]{stackPath};
            }
            for (String stackPath : stackPaths) {
                apps.addAll(getAllStackApps(stackPath));
            }
            return apps;
        } catch (HttpClientException e) {
            throw new ScalerException("Failed to get stack apps", e);
        }
    }

    /**
     * Returns all the applications under specified Docker Swarm stack
     */
    private Collection<DockerSwarmApp> getAllStackApps(final String stackId)
    {
        final Collection<DockerSwarmApp> apps = new ArrayList<>();
        addAllGroupAppsToCollection(apps, stackId);
        return apps;

    }

    /**
     * Adds the applications that are under the specified group into the specified collection.
     */
    private void addAllGroupAppsToCollection(final Collection<DockerSwarmApp> apps, final String stackId)
    {
        assert apps != null;
        assert stackId != null;

        DocumentContext applicationsResponse = dockerSwarm.getServicesFiltered(buildServiceFilter(
            DockerSwarmFilters.ServiceFilterByType.LABEL, DockerSwarmFilters.FilterLabelKeys.DOCKER_STACK, stackId));

        // query the list of all the apps, for all apps, that have a label autoscale.metric, in this way we know it is one we should
        // be interested in.        
        LinkedList<LinkedHashMap> allAutoscaleAppsInStack = applicationsResponse.read(
            "$[?(@.Spec.TaskTemplate.ContainerSpec.Labels['autoscale.metric'])]", LinkedList.class);

        if (allAutoscaleAppsInStack.isEmpty()) {
            LOG.trace("No valid services in stack which have an autoscale.metric label.: " + stackId);
            return;
        }

        for (LinkedHashMap appObjectInJson : allAutoscaleAppsInStack) {
            
            
            // Build a dockerswarmapp, by using the ID and labels fields.
            DockerSwarmApp app = new DockerSwarmApp();
            app.setId(appObjectInJson.get("ID").toString());
            
            // Get the labels object for the object with this ID, we could drill down, or query json again.
            
            LinkedHashMap specNodeMap = (LinkedHashMap) appObjectInJson.get("Spec");
            Objects.requireNonNull(specNodeMap, "Application failed to have a valid Spec object with labels.");
            LinkedHashMap templateNodeMap = (LinkedHashMap) specNodeMap.get("TaskTemplate");
            Objects.requireNonNull(templateNodeMap, "Application failed to have a valid TaskTemplate object with labels.");
            LinkedHashMap containerSpecNodeMap = (LinkedHashMap) templateNodeMap.get("ContainerSpec");
            Objects.requireNonNull(containerSpecNodeMap, "Application failed to have a valid ContainerSpec object with labels.");
            LinkedHashMap labelsNodeMap = (LinkedHashMap) containerSpecNodeMap.get("Labels");
            Objects.requireNonNull(labelsNodeMap, "Application failed to have a valid Labels object.");
            
            // LinkedList<LinkedHashMap> labels  = applicationsResponse.read("$[?(@.ID == '" + app.getId() + "')]");
            
            Map<String, String> labelsItems = ((Map<String, String>)labelsNodeMap);
            app.setLabels(labelsItems);
            
            apps.add(app);
        }

    }

    private void handleStrings(final ScalingConfiguration sv, final Map<String, String> labels)
    {
        sv.setWorkloadMetric(labels.get(ScalingConfiguration.KEY_WORKLOAD_METRIC));
        if (labels.containsKey(ScalingConfiguration.KEY_SCALING_TARGET)) {
            sv.setScalingTarget(labels.get(ScalingConfiguration.KEY_SCALING_TARGET));
        }
        if (labels.containsKey(ScalingConfiguration.KEY_SCALING_PROFILE)) {
            sv.setScalingProfile(labels.get(ScalingConfiguration.KEY_SCALING_PROFILE));
        }
    }

    private void handleIntegers(final ScalingConfiguration sv, final Map<String, String> labels)
    {
        if (labels.containsKey(ScalingConfiguration.KEY_INTERVAL)) {
            sv.setInterval(Integer.parseInt(labels.get(ScalingConfiguration.KEY_INTERVAL)));
        }
        if (labels.containsKey(ScalingConfiguration.KEY_MAX_INSTANCES)) {
            sv.setMaxInstances(Integer.parseInt(labels.get(ScalingConfiguration.KEY_MAX_INSTANCES)));
        }
        if (labels.containsKey(ScalingConfiguration.KEY_MIN_INSTANCES)) {
            sv.setMinInstances(Integer.parseInt(labels.get(ScalingConfiguration.KEY_MIN_INSTANCES)));
        }
        if (labels.containsKey(ScalingConfiguration.KEY_BACKOFF_AMOUNT)) {
            sv.setBackoffAmount(Integer.parseInt(labels.get(ScalingConfiguration.KEY_BACKOFF_AMOUNT)));
        }
    }

    /**
     * Try a trivial connection to the HTTP endpoint.
     *
     * @return whether a connection to the docker swarm works or not
     */
    @Override
    public HealthResult healthCheck()
    {
        try (Socket socket = new Socket()) {
            socket.connect(new InetSocketAddress(url.getHost(), url.getPort()), Integer.valueOf(config.getHealthCheckTimeoutInSecs().toString())*1000);
            return HealthResult.RESULT_HEALTHY;
        } catch (IOException e) {
            LOG.warn("Connection failure to HTTP endpoint", e);
            return new HealthResult(HealthStatus.UNHEALTHY, "Cannot connect to REST endpoint: " + url);
        }
    }
}
