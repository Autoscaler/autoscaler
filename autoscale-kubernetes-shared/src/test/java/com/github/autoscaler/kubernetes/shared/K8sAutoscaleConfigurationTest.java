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
package com.github.autoscaler.kubernetes.shared;

import static org.junit.jupiter.api.Assertions.assertEquals;
import com.hpe.caf.codec.JsonCodec;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;

public class K8sAutoscaleConfigurationTest
{
    final int maxInstances = 1;
    final List<String> namespacesArray = Arrays.asList("1","2","3","4");
    final String namespaces = " 1,  2, 3 ,4";
    final String groupId = "myGroup";
    @Test
    public void ctorTest() {
        final K8sAutoscaleConfiguration k8SAutoscaleConfiguration = new K8sAutoscaleConfiguration();
        k8SAutoscaleConfiguration.setMaximumInstances(maxInstances);
        k8SAutoscaleConfiguration.setNamespaces(namespaces);
        k8SAutoscaleConfiguration.setGroupId(groupId);
        assertEquals(maxInstances, k8SAutoscaleConfiguration.getMaximumInstances());
        assertEquals(namespacesArray, k8SAutoscaleConfiguration.getNamespacesArray());
        assertEquals(groupId, k8SAutoscaleConfiguration.getGroupId());
    }

    @Test
    public void deserializeTest() throws Exception
    {
        K8sAutoscaleConfiguration autoscaleConfiguration = new K8sAutoscaleConfiguration();

        autoscaleConfiguration.setNamespaces("private");
        autoscaleConfiguration.setGroupId("managed-queue-workers");
        autoscaleConfiguration.setMaximumInstances(4);

        JsonCodec jsonCodec = new JsonCodec();

        byte[] serialized = jsonCodec.serialise(autoscaleConfiguration);

        K8sAutoscaleConfiguration deserialized = jsonCodec.deserialise(serialized, K8sAutoscaleConfiguration.class);

        assertEquals(autoscaleConfiguration.getNamespacesArray(), deserialized.getNamespacesArray());
        assertEquals(autoscaleConfiguration.getGroupId(), deserialized.getGroupId());
        assertEquals(autoscaleConfiguration.getMaximumInstances(), deserialized.getMaximumInstances());
    }
}
