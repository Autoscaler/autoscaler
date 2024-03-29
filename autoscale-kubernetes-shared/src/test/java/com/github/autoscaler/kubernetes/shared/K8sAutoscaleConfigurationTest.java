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

import org.junit.Assert;
import org.junit.Test;

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
        Assert.assertEquals(maxInstances, k8SAutoscaleConfiguration.getMaximumInstances());
        Assert.assertEquals(namespacesArray, k8SAutoscaleConfiguration.getNamespacesArray());
        Assert.assertEquals(groupId, k8SAutoscaleConfiguration.getGroupId());
    }
}
