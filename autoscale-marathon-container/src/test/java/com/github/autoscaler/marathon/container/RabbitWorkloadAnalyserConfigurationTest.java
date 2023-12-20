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
package com.github.autoscaler.marathon.container;

import com.github.autoscaler.workload.rabbit.RabbitWorkloadAnalyserConfiguration;
import com.github.autoscaler.workload.rabbit.RabbitWorkloadProfile;
import com.hpe.caf.api.BootstrapConfiguration;
import com.hpe.caf.cipher.NullCipher;
import com.hpe.caf.codec.JsonCodec;
import com.hpe.caf.config.file.FileConfigurationSource;
import com.hpe.caf.naming.ServicePath;
import org.junit.Assert;
import org.junit.Test;
import org.mockito.Mockito;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;

/**
 * Test the deserialization
 */
public class RabbitWorkloadAnalyserConfigurationTest {

    @Test
    public void deserializeTest() throws Exception {
        RabbitWorkloadAnalyserConfiguration rabbitWorkloadAnalyserConfiguration = new RabbitWorkloadAnalyserConfiguration();

        Map<String, RabbitWorkloadProfile> profiles = new HashMap<>();
        RabbitWorkloadProfile rabbitWorkloadProfile = new RabbitWorkloadProfile();
        rabbitWorkloadProfile.setBacklogGoal(10);
        rabbitWorkloadProfile.setScalingDelay(60);
        profiles.put("default", rabbitWorkloadProfile);
        rabbitWorkloadAnalyserConfiguration.setProfiles(profiles);

        JsonCodec jsonCodec = new JsonCodec();
        
        byte[] serialized = jsonCodec.serialise(rabbitWorkloadAnalyserConfiguration);
        
        RabbitWorkloadAnalyserConfiguration deserialized = jsonCodec.deserialise(serialized, RabbitWorkloadAnalyserConfiguration.class);

        Assert.assertEquals(rabbitWorkloadAnalyserConfiguration.getRabbitManagementEndpoint(), deserialized.getRabbitManagementEndpoint());
        Assert.assertEquals(1, deserialized.getProfiles().size());

    }

    @Test
    public void readFileTest() throws Exception{
        Path currentRelativePath = Paths.get("");
        String s = currentRelativePath.toAbsolutePath().toString();
        System.out.println("Current relative path is: " + s);

        BootstrapConfiguration bootstrapConfiguration = Mockito.mock(BootstrapConfiguration.class);
        Mockito.when(bootstrapConfiguration.isConfigurationPresent("CAF_CONFIG_PATH")).thenReturn(true);
        Mockito.when(bootstrapConfiguration.getConfiguration("CAF_CONFIG_PATH")).thenReturn("src/test/resources");


        ServicePath servicePath = new ServicePath("/caf/autoscaler/autoscaler");
        JsonCodec jsonCodec = new JsonCodec();


        FileConfigurationSource fileConfigurationSource = new FileConfigurationSource(bootstrapConfiguration,
                new NullCipher(), servicePath, jsonCodec);

        RabbitWorkloadAnalyserConfiguration rabbitWorkloadAnalyserConfiguration = fileConfigurationSource.getConfiguration(RabbitWorkloadAnalyserConfiguration.class);

        Assert.assertNotNull("Configuration passed back should not be null", rabbitWorkloadAnalyserConfiguration);

        Assert.assertEquals("Expected Rabbit Endpoint should match endpoint read in","http://localhost:15672", rabbitWorkloadAnalyserConfiguration.getRabbitManagementEndpoint());
        Assert.assertEquals("Expected Rabbit User should match user read in","guest", rabbitWorkloadAnalyserConfiguration.getRabbitManagementUser());
        Assert.assertEquals("Expected Rabbit User Password should match user password read in","guest", rabbitWorkloadAnalyserConfiguration.getRabbitManagementPassword());

        int actualScalingDeplay = rabbitWorkloadAnalyserConfiguration.getProfiles().get("default").getScalingDelay();
        int actualBacklogGoal = rabbitWorkloadAnalyserConfiguration.getProfiles().get("default").getBacklogGoal();

        Assert.assertEquals("Expected scaling delay should match default Rabbit Workload Profile scaling delay read in", 10, actualScalingDeplay);
        Assert.assertEquals("Expected backlog goal should match default Rabbit Workload Profile backlog goal read in", 300, actualBacklogGoal);

    }
}
