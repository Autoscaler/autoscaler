/*
 * Copyright 2015-2017 EntIT Software LLC, a Micro Focus company.
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
package com.hpe.caf.autoscale.scaler.marathon;

import com.hpe.caf.api.HealthResult;
import com.hpe.caf.api.HealthStatus;
import com.hpe.caf.api.autoscale.InstanceInfo;
import com.hpe.caf.api.autoscale.ScalerException;
import com.hpe.caf.api.autoscale.ServiceHost;
import com.hpe.caf.api.autoscale.ServiceScaler;

import mesosphere.marathon.client.Marathon;
import mesosphere.marathon.client.MarathonException;
import mesosphere.marathon.client.model.v2.App;
import mesosphere.marathon.client.model.v2.GetAppResponse;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.URL;
import java.util.Collection;
import java.util.LinkedList;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * A MarathonServiceScaler uses a Marathon Java client library to make calls to a
 * Marathon server to trigger scaling of a service, and return information on the
 * number of instances of a configured task.
 */
public class MarathonServiceScaler implements ServiceScaler
{
    private final Marathon marathon;
    private final int maximumInstances;
    private final URL url;
    private final AppInstancePatcher appInstancePatcher;

    private static final Logger LOG = LoggerFactory.getLogger(MarathonServiceScaler.class);


    public MarathonServiceScaler(final Marathon marathon, final int maxInstances, final URL url,
                                 final AppInstancePatcher appInstancePatcher)
    {
        this.marathon = Objects.requireNonNull(marathon);
        maximumInstances = Math.max(1, maxInstances);
        this.url = Objects.requireNonNull(url);
        this.appInstancePatcher = appInstancePatcher;
    }

    @Override
    public void scaleUp(final String serviceReference, final int amount)
            throws ScalerException
    {
        try {
            GetAppResponse appGet = marathon.getApp(serviceReference);
            App app = appGet.getApp();
            int current = app.getTasksRunning() + app.getTasksStaged();
            int target = Math.min(maximumInstances, current + amount);
            if ( target > current ) {
                LOG.debug("Scaling service {} up by {} instances", serviceReference, amount);
                appInstancePatcher.patchInstances(app.getId(), target);
            }
        } catch (MarathonException  e) {
            throw new ScalerException("Failed to scale up service " + serviceReference, e);
        }
    }


    @Override
    public void scaleDown(final String serviceReference, final int amount)
            throws ScalerException
    {
        try {
            GetAppResponse appGet = marathon.getApp(serviceReference);
            App app = appGet.getApp();
            int current = app.getTasksRunning() + app.getTasksStaged();
            if ( current > 0 ) {
                LOG.debug("Scaling service {} down by {} instances", serviceReference, amount);
                appInstancePatcher.patchInstances(app.getId(), Math.max(0, current - amount));
            }
        } catch (MarathonException e) {
            throw new ScalerException("Failed to scale down service " + serviceReference, e);
        }
    }


    @Override
    public InstanceInfo getInstanceInfo(final String serviceReference)
            throws ScalerException
    {
        try {
            GetAppResponse appGet = marathon.getApp(serviceReference);
            Collection<ServiceHost> hosts = appGet.getApp()
                                                  .getTasks()
                                                  .stream()
                                                  .map(t -> new ServiceHost(t.getHost(), t.getPorts()))
                                                  .collect(Collectors.toCollection(LinkedList::new));
            return new InstanceInfo(appGet.getApp().getTasksRunning(), appGet.getApp().getTasksStaged(), hosts);
        } catch (MarathonException e) {
            throw new ScalerException("Failed to get number of instances of " + serviceReference, e);
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
