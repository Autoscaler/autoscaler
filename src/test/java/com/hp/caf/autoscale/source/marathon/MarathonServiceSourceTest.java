package com.hp.caf.autoscale.source.marathon;


import com.hp.caf.api.autoscale.ScalerException;
import com.hp.caf.api.autoscale.ScalingConfiguration;
import mesosphere.marathon.client.Marathon;
import mesosphere.marathon.client.model.v2.App;
import mesosphere.marathon.client.model.v2.Group;
import mesosphere.marathon.client.utils.MarathonException;
import org.junit.Assert;
import org.junit.Test;
import org.mockito.Mockito;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;


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
        App app = Mockito.mock(App.class);
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

        App app2 = Mockito.mock(App.class);
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
