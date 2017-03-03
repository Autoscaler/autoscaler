package com.hpe.caf.autoscale.scaler.marathon;


import com.hpe.caf.api.autoscale.InstanceInfo;
import com.hpe.caf.api.autoscale.ScalerException;
import mesosphere.marathon.client.Marathon;
import mesosphere.marathon.client.model.v2.App;
import mesosphere.marathon.client.model.v2.GetAppResponse;
import mesosphere.marathon.client.model.v2.Task;
import mesosphere.marathon.client.utils.MarathonException;
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
        App app = Mockito.mock(App.class);
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
        App app = Mockito.mock(App.class);
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
        App app = Mockito.mock(App.class);
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
        App app = Mockito.mock(App.class);
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

        App app = Mockito.mock(App.class);
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
