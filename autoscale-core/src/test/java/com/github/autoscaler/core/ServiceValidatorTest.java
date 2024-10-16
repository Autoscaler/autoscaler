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
package com.github.autoscaler.core;


import com.github.autoscaler.api.ScalingConfiguration;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.Test;

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;


public class ServiceValidatorTest
{
    private static final String VALID_METRIC = "a";
    private static final String INVALID_METRIC = "b";
    private static final Collection<String> METRIC_NAMES = Collections.singletonList(VALID_METRIC);


    @Test
    public void testAppName()
    {
        ServiceValidator validator = new ServiceValidator(METRIC_NAMES);
        ScalingConfiguration sc1 = new ScalingConfiguration();
        sc1.setId("unitTest1");
        sc1.setWorkloadMetric(VALID_METRIC);
        ScalingConfiguration sc2 = new ScalingConfiguration();
        sc2.setId("unitTest2");
        sc2.setWorkloadMetric(VALID_METRIC);
        Set<ScalingConfiguration> in = new HashSet<>();
        in.add(sc1);
        in.add(sc2);
        Set<ScalingConfiguration> out = validator.getValidatedServices(in);
        assertTrue(out.contains(sc2));
        assertTrue(out.contains(sc1));
    }


    @Test
    public void testInvalidId()
    {
        ServiceValidator validator = new ServiceValidator(METRIC_NAMES);
        ScalingConfiguration sc1 = new ScalingConfiguration();
        sc1.setWorkloadMetric(VALID_METRIC);
        Set<ScalingConfiguration> in = new HashSet<>();
        in.add(sc1);
        Set<ScalingConfiguration> out = validator.getValidatedServices(in);
        assertFalse(out.contains(sc1));
    }


    @Test
    public void testInvalidMetric()
    {
        ServiceValidator validator = new ServiceValidator(METRIC_NAMES);
        ScalingConfiguration sc1 = new ScalingConfiguration();
        sc1.setId("unitTest");
        Set<ScalingConfiguration> in = new HashSet<>();
        in.add(sc1);
        Set<ScalingConfiguration> out = validator.getValidatedServices(in);
        assertFalse(out.contains(sc1));
    }


    @Test
    public void testUnrecognisedMetric()
    {
        ServiceValidator validator = new ServiceValidator(METRIC_NAMES);
        ScalingConfiguration sc1 = new ScalingConfiguration();
        sc1.setId("unitTest");
        sc1.setWorkloadMetric(INVALID_METRIC);
        Set<ScalingConfiguration> in = new HashSet<>();
        in.add(sc1);
        Set<ScalingConfiguration> out = validator.getValidatedServices(in);
        assertFalse(out.contains(sc1));
    }


    @Test
    public void testInvalidScale()
    {
        ServiceValidator validator = new ServiceValidator(METRIC_NAMES);
        ScalingConfiguration sc1 = new ScalingConfiguration();
        sc1.setId("unitTest1");
        sc1.setWorkloadMetric(VALID_METRIC);
        sc1.setMinInstances(-1);
        sc1.setMaxInstances(5);
        ScalingConfiguration sc2 = new ScalingConfiguration();
        sc2.setId("unitTest2");
        sc2.setWorkloadMetric(VALID_METRIC);
        sc2.setMinInstances(0);
        sc2.setMaxInstances(-1);
        Set<ScalingConfiguration> in = new HashSet<>();
        in.add(sc1);
        Set<ScalingConfiguration> out = validator.getValidatedServices(in);
        assertFalse(out.contains(sc1));
        assertFalse(out.contains(sc2));
    }
}
