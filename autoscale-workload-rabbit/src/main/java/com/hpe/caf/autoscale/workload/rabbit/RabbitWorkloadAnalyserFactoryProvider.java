/*
 * Copyright 2015-2021 Micro Focus or one of its affiliates.
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
package com.hpe.caf.autoscale.workload.rabbit;


import com.hpe.caf.api.ConfigurationException;
import com.hpe.caf.api.ConfigurationSource;
import com.hpe.caf.api.autoscale.ScalerException;
import com.hpe.caf.api.autoscale.WorkloadAnalyserFactory;
import com.hpe.caf.api.autoscale.WorkloadAnalyserFactoryProvider;

public class RabbitWorkloadAnalyserFactoryProvider implements WorkloadAnalyserFactoryProvider
{
    @Override
    public WorkloadAnalyserFactory getWorkloadAnalyserFactory(final ConfigurationSource configurationSource)
            throws ScalerException
    {
        try {
            return new RabbitWorkloadAnalyserFactory(configurationSource.getConfiguration(RabbitWorkloadAnalyserConfiguration.class));
        } catch (ConfigurationException e) {
            throw new ScalerException("Failed to create a workload analyser factory", e);
        }
    }

    @Override
    public String getWorkloadAnalyserName()
    {
        return "rabbitmq";
    }
}
