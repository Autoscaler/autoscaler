package com.hpe.caf.autoscale.source.marathon;


import com.hpe.caf.api.HealthResult;
import com.hpe.caf.api.HealthStatus;
import com.hpe.caf.api.autoscale.ScalerException;
import com.hpe.caf.api.autoscale.ScalingConfiguration;
import com.hpe.caf.api.autoscale.ServiceSource;
import mesosphere.marathon.client.Marathon;
import mesosphere.marathon.client.model.v2.App;
import mesosphere.marathon.client.utils.MarathonException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.URL;
import java.util.Collection;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;


/**
 * A MarathonServiceSource uses a Java client library to make calls to a Marathon host
 * and retrieve information about running tasks. It will create ScalingConfiguration
 * objects based upon this data.
 */
public class MarathonServiceSource extends ServiceSource
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
            return marathon.getGroup(groupPath).getApps();
        } catch (MarathonException e) {
            throw new ScalerException("Failed to get group apps", e);
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
