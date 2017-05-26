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

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.Utf8;
import com.hpe.caf.autoscale.DockerSwarmAutoscaleConfiguration;
import com.hpe.caf.codec.JsonCodec;
import com.hpe.caf.codec.ObjectMapperFactory;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.runners.MockitoJUnitRunner;

/**
 * DockerSwarm configuration class - unit testing only.
 */
@RunWith(MockitoJUnitRunner.class)
public class DockerSwarmAutoscaleConfigurationTest
{
    /**
     * Test the deserialization
     */
    @Test
    public void deserializeTest() throws Exception
    {
        DockerSwarmAutoscaleConfiguration autoscaleConfiguration = new DockerSwarmAutoscaleConfiguration();

        autoscaleConfiguration.setEndpoint("http://TestEndpoint:8081");
        autoscaleConfiguration.setTimeoutInSecs(new Long(10));
        autoscaleConfiguration.setStackId("AnyOldStack");
        autoscaleConfiguration.setMaximumInstances(24);
        autoscaleConfiguration.setTlsVerify(true);
        autoscaleConfiguration.setCertificatePath("/usr/thispath/mycert.crt");

        JsonCodec jsonCodec = new JsonCodec();

        byte[] serialized = jsonCodec.serialise(autoscaleConfiguration);

        DockerSwarmAutoscaleConfiguration deserialized = jsonCodec.deserialise(serialized, DockerSwarmAutoscaleConfiguration.class);

        Assert.assertEquals(autoscaleConfiguration.getEndpoint(), deserialized.getEndpoint());
        Assert.assertEquals(autoscaleConfiguration.getTimeoutInSecs(), deserialized.getTimeoutInSecs());
        Assert.assertEquals(autoscaleConfiguration.getMaximumInstances(), deserialized.getMaximumInstances());
        Assert.assertEquals(autoscaleConfiguration.getStackId(), deserialized.getStackId());
        Assert.assertEquals(autoscaleConfiguration.getTlsVerify(), deserialized.getTlsVerify());
        Assert.assertEquals(autoscaleConfiguration.getCertificatePath(), deserialized.getCertificatePath());
    }
}
