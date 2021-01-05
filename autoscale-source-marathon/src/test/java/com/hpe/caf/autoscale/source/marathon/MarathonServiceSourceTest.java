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
package com.hpe.caf.autoscale.source.marathon;


import com.hpe.caf.api.autoscale.ScalerException;
import com.hpe.caf.api.autoscale.ScalingConfiguration;
import mesosphere.marathon.client.Marathon;
import mesosphere.marathon.client.MarathonException;
import mesosphere.marathon.client.model.v2.Group;
import mesosphere.marathon.client.model.v2.VersionedApp;
import org.hamcrest.CoreMatchers;
import org.hamcrest.Matchers;
import org.junit.Assert;
import org.junit.Test;
import org.mockito.Mockito;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.*;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.Matchers.hasProperty;


public class MarathonServiceSourceTest
{
    private static final String GROUP = "/my/test/group";
    private static final String APP_ID = "UnitTestApp";
    private static final String BAD_APP_ID = "UnitTestApp2";
    private static final String METRIC = "UnitTestMetric";
    private static final String TARGET = "UnitTestTarget";
    private static final String PROFILE = "UnitTestProfile";
    private static final int INTERVAL = 30;
    private static final int MAX = 10;
    private static final int MIN = 2;
    private static final int BACKOFF = 5;


    @Test
    public void getServicesTest()
        throws MarathonException, MalformedURLException, ScalerException
    {
        VersionedApp app = Mockito.mock(VersionedApp.class);
        Mockito.when(app.getId()).thenReturn(APP_ID);
        Map<String, String> labels = new HashMap<>();
        labels.put(ScalingConfiguration.KEY_WORKLOAD_METRIC, METRIC);
        labels.put(ScalingConfiguration.KEY_SCALING_TARGET, TARGET);
        labels.put(ScalingConfiguration.KEY_SCALING_PROFILE, PROFILE);
        labels.put(ScalingConfiguration.KEY_INTERVAL, String.valueOf(INTERVAL));
        labels.put(ScalingConfiguration.KEY_MAX_INSTANCES, String.valueOf(MAX));
        labels.put(ScalingConfiguration.KEY_MIN_INSTANCES, String.valueOf(MIN));
        labels.put(ScalingConfiguration.KEY_BACKOFF_AMOUNT, String.valueOf(BACKOFF));
        Mockito.when(app.getLabels()).thenReturn(labels);

        VersionedApp app2 = Mockito.mock(VersionedApp.class);
        Mockito.when(app2.getId()).thenReturn(BAD_APP_ID);
        Mockito.when(app2.getLabels()).thenReturn(new HashMap<>());

        Group group = Mockito.mock(Group.class);
        Mockito.when(group.getApps()).thenReturn(Arrays.asList(app, app2));
        Marathon marathon = Mockito.mock(Marathon.class);
        Mockito.when(marathon.getGroup(GROUP)).thenReturn(group);
        MarathonServiceSource source = new MarathonServiceSource(marathon, GROUP, new URL("http://localhost:8080"));
        Set<ScalingConfiguration> res = source.getServices();
        Assert.assertEquals(1, res.size());
        Assert.assertEquals(APP_ID, res.iterator().next().getId());
        Assert.assertEquals(METRIC, res.iterator().next().getWorkloadMetric());
        Assert.assertEquals(TARGET, res.iterator().next().getScalingTarget());
        Assert.assertEquals(PROFILE, res.iterator().next().getScalingProfile());
        Assert.assertEquals(INTERVAL, res.iterator().next().getInterval());
        Assert.assertEquals(MAX, res.iterator().next().getMaxInstances());
        Assert.assertEquals(MIN, res.iterator().next().getMinInstances());
        Assert.assertEquals(BACKOFF, res.iterator().next().getBackoffAmount());
    }

    /**
     * This tests that a CSV of groups paths will be correctly processed
     */
    @Test
    public void multipleGroupPathTest() throws MarathonException, ScalerException, MalformedURLException {
        VersionedApp app1 = new VersionedApp();
        app1.setLabels(new HashMap<>());
        app1.getLabels().put(ScalingConfiguration.KEY_WORKLOAD_METRIC, "somemetric");
        app1.setId("/group1/app1");

        VersionedApp app2 = new VersionedApp();
        app2.setLabels(new HashMap<>());
        app2.getLabels().put(ScalingConfiguration.KEY_WORKLOAD_METRIC, "somemetric");
        app2.setId("/group2/app2");

        Group group1 = new Group();
        group1.setGroups(new ArrayList<>());
        group1.setApps(new ArrayList<>());
        group1.getApps().add(app1);

        Group group2 = new Group();
        group2.setGroups(new ArrayList<>());
        group2.setApps(new ArrayList<>());
        group2.getApps().add(app2);

        Marathon marathon = Mockito.mock(Marathon.class);
        Mockito.when(marathon.getGroup("/group1")).thenReturn(group1);
        Mockito.when(marathon.getGroup("/group2")).thenReturn(group2);

        MarathonServiceSource marathonServiceSource = new MarathonServiceSource(marathon, "/group1,/group2", new URL("http://notneeded.com"));
        Set<ScalingConfiguration> services = marathonServiceSource.getServices();

        Assert.assertEquals(2, services.size());
        Assert.assertThat(services, Matchers.containsInAnyOrder(
                hasProperty("id", is(app1.getId())),
                hasProperty("id", is(app2.getId()))
        ));
    }

    @Test(expected = ScalerException.class)
    public void getServicesExceptionTest()
        throws MarathonException, MalformedURLException, ScalerException
    {
        Marathon marathon = Mockito.mock(Marathon.class);
        Mockito.when(marathon.getGroup(GROUP)).thenThrow(MarathonException.class);
        MarathonServiceSource source = new MarathonServiceSource(marathon, GROUP, new URL("http://localhost:8080"));
        source.getServices();
    }
}
