
import com.hpe.caf.api.autoscale.InstanceInfo;
import com.hpe.caf.api.autoscale.ScalerException;
import com.hpe.caf.autoscale.scaler.marathon.MarathonServiceScaler;
import java.net.MalformedURLException;
import java.net.URL;
import mesosphere.marathon.client.Marathon;
import mesosphere.marathon.client.MarathonClient;
import org.junit.Assert;

/**
 * Copyright 2016 Hewlett Packard Enterprise.
 * Copyright 2017 Hewlett Packard Enterprise Development LP.
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

/**
 *
 * @author Trevor Getty <trevor.getty@hpe.com>
 */
public class MarathonServiceScalerIT {

    @Test
    public void testApp() throws ScalerException, MalformedURLException
    {
        URL url = new URL("http://192.168.56.10:8080");
        Marathon marathon = new MarathonClient(url, 5000);
        
        MarathonServiceScaler scaler = new MarathonServiceScaler(marathon, 100, url);
        // get service ref.
        InstanceInfo info = scaler.getInstanceInfo("jobservice");
        
        Assert.assertEquals(1, info.getInstancesRunning());
        Assert.assertEquals(0, info.getInstancesStaging());
        Assert.assertEquals(1, info.getTotalInstances());
        Assert.assertEquals(host1, info.getHosts().iterator().next().getHost());
        Assert.assertTrue(info.getHosts().iterator().next().getPorts().containsAll(ports1));
    }
}
