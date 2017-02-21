package com.hpe.caf.autoscale.scaler.marathon;


import com.hpe.caf.api.HealthResult;
import com.hpe.caf.api.HealthStatus;
import com.hpe.caf.api.autoscale.InstanceInfo;
import com.hpe.caf.api.autoscale.ScalerException;
import com.hpe.caf.api.autoscale.ServiceHost;
import com.hpe.caf.api.autoscale.ServiceScaler;
import mesosphere.marathon.client.Marathon;
import mesosphere.marathon.client.model.v2.App;
import mesosphere.marathon.client.model.v2.GetAppResponse;
import mesosphere.marathon.client.utils.MarathonException;
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
    private Marathon marathon;
    private final int maximumInstances;
    private final URL url;
    private static final Logger LOG = LoggerFactory.getLogger(MarathonServiceScaler.class);


    public MarathonServiceScaler(final Marathon marathon, final int maxInstances, final URL url)
    {
        this.marathon = Objects.requireNonNull(marathon);
        maximumInstances = Math.max(1, maxInstances);
        this.url = Objects.requireNonNull(url);
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
                app.setInstances(Math.min(maximumInstances, app.getTasksRunning() + app.getTasksStaged() + amount));
                LOG.debug("Scaling service {} up by {} instances", serviceReference, amount);
                marathon.updateApp(serviceReference, app, true);
            }
        } catch (MarathonException e) {
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
                app.setInstances(Math.max(0, current - amount));
                LOG.debug("Scaling service {} down by {} instances", serviceReference, amount);
                marathon.updateApp(serviceReference, app, true);
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
