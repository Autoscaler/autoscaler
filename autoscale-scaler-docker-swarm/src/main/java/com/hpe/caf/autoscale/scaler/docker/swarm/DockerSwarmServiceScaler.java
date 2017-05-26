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

import com.hpe.caf.api.HealthResult;
import com.hpe.caf.api.HealthStatus;
import com.hpe.caf.api.autoscale.InstanceInfo;
import com.hpe.caf.api.autoscale.ScalerException;
import com.hpe.caf.api.autoscale.ServiceScaler;
import com.hpe.caf.autoscale.scaler.endpoint.docker.DockerSwarm;
import com.hpe.caf.autoscale.scaler.endpoint.HttpClientException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.URL;
import java.util.Objects;
import sun.reflect.generics.reflectiveObjects.NotImplementedException;

/**
 * A ServiceScaler uses a Docker Java client library to make calls to a Docker Swarm server to trigger scaling of a service, and return
 * information on the number of instances of a configured task.
 */
public class DockerSwarmServiceScaler implements ServiceScaler
{
    private DockerSwarm dockerClient;
    private final int maximumInstances;
    private final URL url;
    private static final Logger LOG = LoggerFactory.getLogger(ServiceScaler.class);

    public DockerSwarmServiceScaler(final DockerSwarm dockerClient, final int maxInstances, final URL url)
    {
        this.dockerClient = Objects.requireNonNull(dockerClient);
        maximumInstances = Math.max(1, maxInstances);
        this.url = Objects.requireNonNull(url);
    }

    @Override
    public void scaleUp(final String serviceReference, final int amount)
        throws ScalerException
    {
        try {
//            GetAppResponse appGet = marathon.getApp(serviceReference);
//            App app = appGet.getApp();
//            int current = app.getTasksRunning() + app.getTasksStaged();
//            int target = Math.min(maximumInstances, current + amount);
//            if ( target > current ) {
//                app.setInstances(Math.min(maximumInstances, app.getTasksRunning() + app.getTasksStaged() + amount));
//                LOG.debug("Scaling service {} up by {} instances", serviceReference, amount);
//                marathon.updateApp(serviceReference, app, true);
//            }


            throw new NotImplementedException();
        } catch (HttpClientException e) {
            throw new ScalerException("Failed to scale up service " + serviceReference, e);
        }
    }

    @Override
    public void scaleDown(final String serviceReference, final int amount)
        throws ScalerException
    {
        try {
//            GetAppResponse appGet = marathon.getApp(serviceReference);
//            App app = appGet.getApp();
//            int current = app.getTasksRunning() + app.getTasksStaged();
//            if ( current > 0 ) {
//                app.setInstances(Math.max(0, current - amount));
//                LOG.debug("Scaling service {} down by {} instances", serviceReference, amount);
//                marathon.updateApp(serviceReference, app, true);
//            }
            throw new NotImplementedException();
        } catch (HttpClientException e) {
            throw new ScalerException("Failed to scale down service " + serviceReference, e);
        }
    }

    @Override
    public InstanceInfo getInstanceInfo(final String serviceReference)
        throws ScalerException
    {
        try {
            throw new NotImplementedException();
//            GetAppResponse appGet = marathon.getApp(serviceReference);
//            Collection<ServiceHost> hosts = appGet.getApp()
//                                                  .getTasks()
//                                                  .stream()
//                                                  .map(t -> new ServiceHost(t.getHost(), t.getPorts()))
//                                                  .collect(Collectors.toCollection(LinkedList::new));
//            return new InstanceInfo(appGet.getApp().getTasksRunning(), appGet.getApp().getTasksStaged(), hosts);
        } catch (HttpClientException e) {
            throw new ScalerException("Failed to get number of instances of " + serviceReference, e);
        }
    }

    /**
     * Try a trivial connection to the Docker REST endpoint.
     *
     * @return whether a connection to the Docker swarm host works or not
     */
    @Override
    public HealthResult healthCheck()
    {
        // TREV TODO -> Check HTTPS comms with this.
        try (Socket socket = new Socket()) {
            socket.connect(new InetSocketAddress(url.getHost(), url.getPort()), 5000);
            return HealthResult.RESULT_HEALTHY;
        } catch (IOException e) {
            LOG.warn("Connection failure to HTTP endpoint", e);
            return new HealthResult(HealthStatus.UNHEALTHY, "Cannot connect to REST endpoint: " + url);
        }
    }
}
