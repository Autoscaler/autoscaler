package com.hpe.caf.autoscale.scaler.marathon;


import com.hpe.caf.api.ConfigurationException;
import com.hpe.caf.api.ConfigurationSource;
import com.hpe.caf.api.autoscale.ScalerException;
import com.hpe.caf.api.autoscale.ServiceScaler;
import com.hpe.caf.api.autoscale.ServiceScalerProvider;
import com.hpe.caf.autoscale.MarathonAutoscaleConfiguration;
import feign.Feign;
import feign.Request;
import mesosphere.marathon.client.Marathon;
import mesosphere.marathon.client.MarathonClient;

import java.net.MalformedURLException;
import java.net.URL;


public class MarathonServiceScalerProvider implements ServiceScalerProvider
{
    private static final int MARATHON_TIMEOUT = 10_000;


    @Override
    public ServiceScaler getServiceScaler(final ConfigurationSource configurationSource)
            throws ScalerException
    {
        try {
            MarathonAutoscaleConfiguration config = configurationSource.getConfiguration(MarathonAutoscaleConfiguration.class);
            URL url = new URL(config.getEndpoint());
            Feign.Builder builder = Feign.builder().options(new Request.Options(MARATHON_TIMEOUT, MARATHON_TIMEOUT));
            Marathon marathon = MarathonClient.getInstance(builder, url.toString());
            return new MarathonServiceScaler(marathon, config.getMaximumInstances(), url);
        } catch (ConfigurationException | MalformedURLException e) {
            throw new ScalerException("Failed to create service scaler", e);
        }
    }
}
