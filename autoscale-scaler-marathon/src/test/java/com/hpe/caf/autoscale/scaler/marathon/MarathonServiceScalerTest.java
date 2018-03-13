/*
 * Copyright 2015-2017 EntIT Software LLC, a Micro Focus company.
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
package com.hpe.caf.autoscale.scaler.marathon;


import com.hpe.caf.api.autoscale.InstanceInfo;
import com.hpe.caf.api.autoscale.ScalerException;
import mesosphere.marathon.client.Marathon;
import mesosphere.marathon.client.MarathonException;
import mesosphere.marathon.client.model.v2.GetAppResponse;
import mesosphere.marathon.client.model.v2.Task;
import mesosphere.marathon.client.model.v2.VersionedApp;
import org.junit.Assert;
import org.junit.Test;
import org.mockito.Mockito;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;


public class MarathonServiceScalerTest
{
    private static final String SERVICE = "TestService";


    @Test
    public void scaleUpTest()
        throws MarathonException, ScalerException, MalformedURLException
    {
        VersionedApp app = Mockito.mock(VersionedApp.class);
        Mockito.when(app.getTasksRunning()).thenReturn(1);
        Mockito.when(app.getTasksStaged()).thenReturn(0);
        GetAppResponse appResponse = Mockito.mock(GetAppResponse.class);
        Mockito.when(appResponse.getApp()).thenReturn(app);
        Marathon marathon = Mockito.mock(Marathon.class);
        Mockito.when(marathon.getApp(SERVICE)).thenReturn(appResponse);
        URL url = new URL("http://localhost:8080");
        MarathonServiceScaler scaler = new MarathonServiceScaler(marathon, 100, url);
        scaler.scaleUp(SERVICE, 1);
        Mockito.verify(app, Mockito.times(1)).setInstances(2);
        Mockito.verify(marathon, Mockito.times(1)).updateApp(Mockito.eq(SERVICE), Mockito.eq(app), Mockito.eq(true));
    }


    @Test(expected = ScalerException.class)
    public void scaleUpExceptionTest()
        throws MarathonException, MalformedURLException, ScalerException
    {
        Marathon marathon = Mockito.mock(Marathon.class);
        Mockito.when(marathon.getApp(SERVICE)).thenThrow(MarathonException.class);
        URL url = new URL("http://localhost:8080");
        MarathonServiceScaler scaler = new MarathonServiceScaler(marathon, 100, url);
        scaler.scaleUp(SERVICE, 1);
    }


    @Test
    public void scaleDownTest()
        throws MarathonException, ScalerException, MalformedURLException
    {
        VersionedApp app = Mockito.mock(VersionedApp.class);
        Mockito.when(app.getTasksRunning()).thenReturn(1);
        Mockito.when(app.getTasksStaged()).thenReturn(0);
        GetAppResponse appResponse = Mockito.mock(GetAppResponse.class);
        Mockito.when(appResponse.getApp()).thenReturn(app);
        Marathon marathon = Mockito.mock(Marathon.class);
        Mockito.when(marathon.getApp(SERVICE)).thenReturn(appResponse);
        URL url = new URL("http://localhost:8080");
        MarathonServiceScaler scaler = new MarathonServiceScaler(marathon, 100, url);
        scaler.scaleDown(SERVICE, 1);
        Mockito.verify(app, Mockito.times(1)).setInstances(0);
        Mockito.verify(marathon, Mockito.times(1)).updateApp(Mockito.eq(SERVICE), Mockito.eq(app), Mockito.eq(true));
    }


    @Test(expected = ScalerException.class)
    public void scaleDownExceptionTest()
        throws MarathonException, MalformedURLException, ScalerException
    {
        Marathon marathon = Mockito.mock(Marathon.class);
        Mockito.when(marathon.getApp(SERVICE)).thenThrow(MarathonException.class);
        URL url = new URL("http://localhost:8080");
        MarathonServiceScaler scaler = new MarathonServiceScaler(marathon, 100, url);
        scaler.scaleDown(SERVICE, 1);
    }


    @Test
    public void scaleUpTestMax()
        throws MarathonException, ScalerException, MalformedURLException
    {
        VersionedApp app = Mockito.mock(VersionedApp.class);
        Mockito.when(app.getTasksRunning()).thenReturn(1);
        Mockito.when(app.getTasksStaged()).thenReturn(0);
        GetAppResponse appResponse = Mockito.mock(GetAppResponse.class);
        Mockito.when(appResponse.getApp()).thenReturn(app);
        Marathon marathon = Mockito.mock(Marathon.class);
        Mockito.when(marathon.getApp(SERVICE)).thenReturn(appResponse);
        URL url = new URL("http://localhost:8080");
        MarathonServiceScaler scaler = new MarathonServiceScaler(marathon, 1, url);
        scaler.scaleUp(SERVICE, 1);
        Mockito.verify(app, Mockito.times(0)).setInstances(Mockito.anyInt());
        Mockito.verify(marathon, Mockito.times(0)).updateApp(Mockito.eq(SERVICE), Mockito.eq(app), Mockito.eq(true));
    }


    @Test
    public void scaleDownTestMin()
        throws MarathonException, ScalerException, MalformedURLException
    {
        VersionedApp app = Mockito.mock(VersionedApp.class);
        Mockito.when(app.getTasksRunning()).thenReturn(0);
        Mockito.when(app.getTasksStaged()).thenReturn(0);
        GetAppResponse appResponse = Mockito.mock(GetAppResponse.class);
        Mockito.when(appResponse.getApp()).thenReturn(app);
        Marathon marathon = Mockito.mock(Marathon.class);
        Mockito.when(marathon.getApp(SERVICE)).thenReturn(appResponse);
        URL url = new URL("http://localhost:8080");
        MarathonServiceScaler scaler = new MarathonServiceScaler(marathon, 100, url);
        scaler.scaleDown(SERVICE, 1);
        Mockito.verify(app, Mockito.times(0)).setInstances(Mockito.anyInt());
        Mockito.verify(marathon, Mockito.times(0)).updateApp(Mockito.eq(SERVICE), Mockito.eq(app), Mockito.eq(true));
    }


    @Test
    public void getInstanceInfoTest()
        throws MarathonException, ScalerException, MalformedURLException
    {
        Task task1 = Mockito.mock(Task.class);
        String host1 = "testHost1";
        Collection<Integer> ports1 = Arrays.asList(1,2,3);
        Mockito.when(task1.getHost()).thenReturn(host1);
        Mockito.when(task1.getPorts()).thenReturn(ports1);

        VersionedApp app = Mockito.mock(VersionedApp.class);
        Mockito.when(app.getTasks()).thenReturn(Collections.singletonList(task1));
        Mockito.when(app.getTasksRunning()).thenReturn(1);
        Mockito.when(app.getTasksStaged()).thenReturn(0);
        GetAppResponse appResponse = Mockito.mock(GetAppResponse.class);
        Mockito.when(appResponse.getApp()).thenReturn(app);
        Marathon marathon = Mockito.mock(Marathon.class);
        Mockito.when(marathon.getApp(SERVICE)).thenReturn(appResponse);

        URL url = new URL("http://localhost:8080");
        MarathonServiceScaler scaler = new MarathonServiceScaler(marathon, 100, url);
        InstanceInfo info = scaler.getInstanceInfo(SERVICE);
        Assert.assertEquals(1, info.getInstancesRunning());
        Assert.assertEquals(0, info.getInstancesStaging());
        Assert.assertEquals(1, info.getTotalInstances());
        Assert.assertEquals(host1, info.getHosts().iterator().next().getHost());
        Assert.assertTrue(info.getHosts().iterator().next().getPorts().containsAll(ports1));
    }


    @Test(expected = ScalerException.class)
    public void getInstanceInfoExceptionTest()
        throws MarathonException, ScalerException, MalformedURLException
    {
        Marathon marathon = Mockito.mock(Marathon.class);
        Mockito.when(marathon.getApp(SERVICE)).thenThrow(MarathonException.class);
        URL url = new URL("http://localhost:8080");
        MarathonServiceScaler scaler = new MarathonServiceScaler(marathon, 100, url);
        scaler.getInstanceInfo(SERVICE);
    }
}
