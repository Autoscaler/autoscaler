/*
 * Copyright 2015-2024 Open Text.
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
package com.github.autoscaler.dockerswarm.shared;

import com.hpe.caf.codec.JsonCodec;
import static org.junit.jupiter.api.Assertions.assertEquals;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * DockerSwarm configuration class - unit testing only.
 */
@ExtendWith(MockitoExtension.class)
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
        autoscaleConfiguration.setTimeoutInSecs(Long.valueOf(10));
        autoscaleConfiguration.setStackId("AnyOldStack");
        autoscaleConfiguration.setMaximumInstances(24);
        autoscaleConfiguration.setTlsVerify(true);
        autoscaleConfiguration.setCertificatePath("/usr/thispath/mycert.crt");

        JsonCodec jsonCodec = new JsonCodec();

        byte[] serialized = jsonCodec.serialise(autoscaleConfiguration);

        DockerSwarmAutoscaleConfiguration deserialized = jsonCodec.deserialise(serialized, DockerSwarmAutoscaleConfiguration.class);

        assertEquals(autoscaleConfiguration.getEndpoint(), deserialized.getEndpoint());
        assertEquals(autoscaleConfiguration.getTimeoutInSecs(), deserialized.getTimeoutInSecs());
        assertEquals(autoscaleConfiguration.getMaximumInstances(), deserialized.getMaximumInstances());
        assertEquals(autoscaleConfiguration.getStackId(), deserialized.getStackId());
        assertEquals(autoscaleConfiguration.getTlsVerify(), deserialized.getTlsVerify());
        assertEquals(autoscaleConfiguration.getCertificatePath(), deserialized.getCertificatePath());
    }
}
