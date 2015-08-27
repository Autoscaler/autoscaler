package com.hp.caf.autoscale.scaler.marathon;


import com.hp.caf.api.ConfigurationException;
import com.hp.caf.api.ConfigurationSource;
import com.hp.caf.api.autoscale.ScalerException;
import com.hp.caf.api.autoscale.ServiceScaler;
import com.hp.caf.api.autoscale.ServiceScalerProvider;
import com.hp.caf.autoscale.MarathonAutoscaleConfiguration;
import mesosphere.marathon.client.MarathonClient;

import java.net.MalformedURLException;
import java.net.URL;


public class MarathonServiceScalerProvider implements ServiceScalerProvider
{
    @Override
    public ServiceScaler getServiceScaler(final ConfigurationSource configurationSource)
            throws ScalerException
    {
        try {
            MarathonAutoscaleConfiguration config = configurationSource.getConfiguration(MarathonAutoscaleConfiguration.class);
            URL url = new URL(config.getEndpoint());
            return new MarathonServiceScaler(MarathonClient.getInstance(url.toString()), config.getMaximumInstances(), url);
        } catch (ConfigurationException | MalformedURLException e) {
            throw new ScalerException("Failed to create service scaler", e);
        }
    }
}
