package com.hp.caf.autoscale.workload.rabbit;


import com.hp.caf.api.ConfigurationException;
import com.hp.caf.api.ConfigurationSource;
import com.hp.caf.api.autoscale.ScalerException;
import com.hp.caf.api.autoscale.WorkloadAnalyserFactory;
import com.hp.caf.api.autoscale.WorkloadAnalyserFactoryProvider;

import java.net.MalformedURLException;


public class RabbitWorkloadAnalyserFactoryProvider implements WorkloadAnalyserFactoryProvider
{
    @Override
    public WorkloadAnalyserFactory getWorkloadAnalyserFactory(final ConfigurationSource configurationSource)
            throws ScalerException
    {
        try {
            return new RabbitWorkloadAnalyserFactory(configurationSource.getConfiguration(RabbitWorkloadAnalyserConfiguration.class));
        } catch (ConfigurationException | MalformedURLException e) {
            throw new ScalerException("Failed to create a workload analyser factory", e);
        }
    }


    @Override
    public String getWorkloadAnalyserName()
    {
        return "rabbitmq";
    }
}
