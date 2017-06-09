/*
 * Copyright 2015-2017 Hewlett Packard Enterprise Development LP.
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
package com.hpe.caf.autoscale.shared.endpoint;

import com.hpe.caf.autoscale.DockerSwarmAutoscaleConfiguration;
import com.hpe.caf.autoscale.endpoint.HttpClientSupport;
import com.hpe.caf.autoscale.endpoint.RequestTemplate;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Collection;
import org.apache.http.NameValuePair;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.message.BasicNameValuePair;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.runners.MockitoJUnitRunner;

/**
 * Unit tests for the HttpClientSupport class.
 */
@RunWith(MockitoJUnitRunner.class)
public class HttpClientSupportTest {
    
    @Test
    public void TestURIBuilder() throws URISyntaxException
    {

        // try to see removal
        Collection<NameValuePair> paramsToProcess = new ArrayList<>();
        paramsToProcess.add(new BasicNameValuePair("testMe", "123"));
        paramsToProcess.add(new BasicNameValuePair("testMe2", "123"));
        paramsToProcess.add(new BasicNameValuePair("testMe3", "12344"));
        paramsToProcess.add(new BasicNameValuePair("testMe4", "1234567"));
        final String requestInfo = "/services/{testMe}/anything";

        RequestTemplate requestTemplate = new RequestTemplate(requestInfo);
        requestTemplate.setParameters(paramsToProcess);
        requestTemplate.setRequestUrl(requestInfo);

        DockerSwarmAutoscaleConfiguration config = buildConfiguration();
        
        URIBuilder uri = HttpClientSupport.buildURI(requestTemplate, config.getEndpoint());
        
        Assert.assertFalse("Should have no {} in path.", uri.toString().contains("{"));

    }
    
    
    private static DockerSwarmAutoscaleConfiguration buildConfiguration()
    {
        DockerSwarmAutoscaleConfiguration config = new DockerSwarmAutoscaleConfiguration();
        config.setEndpoint("http://192.168.56.10:2375");
        config.setProxyEndpoint("http://getty5:8888");
        config.setTimeoutInSecs(new Long(10));
        return config;
    }

}