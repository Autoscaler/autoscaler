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
package com.github.autoscaler.source.marathon;


import com.github.autoscaler.api.ScalerException;
import com.github.autoscaler.api.ScalingConfiguration;
import com.github.autoscaler.api.ServiceSource;
import com.hpe.caf.api.HealthResult;
import com.hpe.caf.api.HealthStatus;
import mesosphere.marathon.client.Marathon;
import mesosphere.marathon.client.MarathonException;
import mesosphere.marathon.client.model.v2.App;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import mesosphere.marathon.client.model.v2.Group;


/**
 * A MarathonServiceSource uses a Java client library to make calls to a Marathon host
 * and retrieve information about running tasks. It will create ScalingConfiguration
 * objects based upon this data.
 */
public class MarathonServiceSource implements ServiceSource
{
    private final Marathon marathon;
    private final URL url;
    private final String groupPath;
    private static final Logger LOG = LoggerFactory.getLogger(MarathonServiceSource.class);


    public MarathonServiceSource(final Marathon marathon, final String groupPath, final URL url)
    {
        this.groupPath = Objects.requireNonNull(groupPath);
        this.url = Objects.requireNonNull(url);
        this.marathon = Objects.requireNonNull(marathon);
    }


    /**
     * Call to Marathon and find all running tasks. Create ScalingConfiguration objects based upon
     * the data Marathon returns. If the service has the env variable specifying its app name, this
     * will be set as the application owner for that service, but the application owner label can
     * override this.
     * @return available ScalingConfiguration services that have a workload metric
     */
    @Override
    public Set<ScalingConfiguration> getServices()
            throws ScalerException
    {
        Set<ScalingConfiguration> ret = new HashSet<>();
        for ( App app : getGroupApps() ) {
            ScalingConfiguration sv = new ScalingConfiguration();
            LOG.debug("Checking marathon service: {}", app.getId());
            sv.setId(app.getId());
            Map<String, String> labels = app.getLabels();
            if ( labels.containsKey(ScalingConfiguration.KEY_WORKLOAD_METRIC) ) {
                handleStrings(sv, labels);
                handleIntegers(sv, labels);
                LOG.debug("Returning scaling service: {}", sv);
                ret.add(sv);
            } else {
                LOG.debug("Skipping service {}, workload metric {} not supported", app.getId(), labels.get(ScalingConfiguration.KEY_WORKLOAD_METRIC));
            }
        }
        return ret;
    }


    private Collection<App> getGroupApps()
            throws ScalerException
    {
        try {
            Collection<App> apps = new ArrayList<>();
            String[] groupPaths;
            if(groupPath.contains(",")){
                LOG.info("Multiple Marathon groups detected: {} splitting on ','.", groupPath);
                groupPaths = groupPath.split(",");
            }
            else{
                groupPaths = new String[]{groupPath};
            }
            for(String groupPath:groupPaths){
                apps.addAll(getAllGroupApps(marathon.getGroup(groupPath)));
            }
            return apps;
        } catch (MarathonException e) {
            throw new ScalerException("Failed to get group apps", e);
        }
    }


    /**
     * Returns all the applications under specified Marathon group,
     * even if they are in sub-groups.
     */
    private static Collection<App> getAllGroupApps(final Group group)
    {
        if (group.getGroups().isEmpty()) {
            return group.getApps();
        }
        else {
            final Collection<App> apps = new ArrayList<>();
            addAllGroupAppsToCollection(apps, group);
            return apps;
        }
    }


    /**
     * Adds the applications that are under the specified group into the specified collection.
     */
    private static void addAllGroupAppsToCollection(final Collection<App> apps, final Group group)
    {
        assert apps != null;
        assert group != null;

        apps.addAll(group.getApps());

        for (final Group subGroup : group.getGroups()) {
            addAllGroupAppsToCollection(apps, subGroup);
        }
    }


    private void handleStrings(final ScalingConfiguration sv, final Map<String, String> labels)
    {
        sv.setWorkloadMetric(labels.get(ScalingConfiguration.KEY_WORKLOAD_METRIC));
        if ( labels.containsKey(ScalingConfiguration.KEY_SCALING_TARGET) ) {
            sv.setScalingTarget(labels.get(ScalingConfiguration.KEY_SCALING_TARGET));
        }
        if ( labels.containsKey(ScalingConfiguration.KEY_SCALING_PROFILE) ) {
            sv.setScalingProfile(labels.get(ScalingConfiguration.KEY_SCALING_PROFILE));
        }
    }


    private void handleIntegers(final ScalingConfiguration sv, final Map<String, String> labels)
    {
        if ( labels.containsKey(ScalingConfiguration.KEY_INTERVAL) ) {
            sv.setInterval(Integer.parseInt(labels.get(ScalingConfiguration.KEY_INTERVAL)));
        }
        if ( labels.containsKey(ScalingConfiguration.KEY_MAX_INSTANCES) ) {
            sv.setMaxInstances(Integer.parseInt(labels.get(ScalingConfiguration.KEY_MAX_INSTANCES)));
        }
        if ( labels.containsKey(ScalingConfiguration.KEY_MIN_INSTANCES) ) {
            sv.setMinInstances(Integer.parseInt(labels.get(ScalingConfiguration.KEY_MIN_INSTANCES)));
        }
        if ( labels.containsKey(ScalingConfiguration.KEY_BACKOFF_AMOUNT) ) {
            sv.setBackoffAmount(Integer.parseInt(labels.get(ScalingConfiguration.KEY_BACKOFF_AMOUNT)));
        }
        if ( labels.containsKey(ScalingConfiguration.KEY_SCALE_DOWN_BACKOFF_AMOUNT) ) {
            sv.setScaleDownBackoffAmount(Integer.parseInt(labels.get(ScalingConfiguration.KEY_SCALE_DOWN_BACKOFF_AMOUNT)));
        }
        if ( labels.containsKey(ScalingConfiguration.KEY_SCALE_UP_BACKOFF_AMOUNT) ) {
            sv.setScaleUpBackoffAmount(Integer.parseInt(labels.get(ScalingConfiguration.KEY_SCALE_UP_BACKOFF_AMOUNT)));
        }
    }


    /**
     * Try a trivial connection to the HTTP endpoint.
     * @return whether a connection to the Marathon host works or not
     */
    @Override
    public HealthResult healthCheck()
    {
        try ( Socket socket = new Socket()) {
            socket.connect(new InetSocketAddress(url.getHost(), url.getPort()), 5000);
            return HealthResult.RESULT_HEALTHY;
        } catch (IOException e) {
            LOG.warn("Connection failure to HTTP endpoint", e);
            return new HealthResult(HealthStatus.UNHEALTHY, "Cannot connect to REST endpoint: " + url);
        }
    }
}
