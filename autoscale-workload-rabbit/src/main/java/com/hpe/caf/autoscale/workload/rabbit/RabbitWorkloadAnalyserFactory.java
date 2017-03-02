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
package com.hpe.caf.autoscale.workload.rabbit;


import com.hpe.caf.api.HealthResult;
import com.hpe.caf.api.HealthStatus;
import com.hpe.caf.api.autoscale.WorkloadAnalyser;
import com.hpe.caf.api.autoscale.WorkloadAnalyserFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.MalformedURLException;
import java.net.Socket;
import java.net.URL;
import java.util.Objects;


public class RabbitWorkloadAnalyserFactory implements WorkloadAnalyserFactory
{
    private final RabbitWorkloadAnalyserConfiguration config;
    private final RabbitStatsReporter provider;
    private final URL url;
    private final RabbitWorkloadProfile defaultProfile;
    private static final Logger LOG = LoggerFactory.getLogger(RabbitWorkloadAnalyserFactory.class);


    public RabbitWorkloadAnalyserFactory(final RabbitWorkloadAnalyserConfiguration config)
            throws MalformedURLException
    {
        this.config = Objects.requireNonNull(config);
        this.url = new URL(config.getRabbitManagementEndpoint());
        this.provider = new RabbitStatsReporter(config.getRabbitManagementEndpoint(), config.getRabbitManagementUser(),
                                                        config.getRabbitManagementPassword(), config.getVhost());
        this.defaultProfile = config.getProfiles().get(RabbitWorkloadAnalyserConfiguration.DEFAULT_PROFILE_NAME);
    }


    @Override
    public WorkloadAnalyser getAnalyser(final String scalingTarget, final String scalingProfile)
    {
        RabbitWorkloadProfile profile;
        if ( scalingProfile == null || !config.getProfiles().containsKey(scalingProfile) ) {
            profile = defaultProfile;
        } else {
            profile = config.getProfiles().get(scalingProfile);
        }
        return new RabbitWorkloadAnalyser(scalingTarget, provider, profile);
    }


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
