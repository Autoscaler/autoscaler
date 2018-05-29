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
package com.hpe.caf.autoscale.tests;

import static org.junit.Assert.*;

import java.io.IOException;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.HttpClients;

import org.junit.Test;

public final class AutoscalerIT
{    
    @Test
    public void testSwarmHealthCheck() throws IOException {
        final String swarmRequestUrl = System.getenv("AUTOSCALE_SWARM_HEALTHCHECK_URL");
        assertTrue(healthCheck(swarmRequestUrl).getStatusLine().getStatusCode() == 200);
    }
    
    @Test
    public void testMarathonHealthCheck() throws IOException {
        final String marathonRequestUrl = System.getenv("AUTOSCALE_MARATHON_HEALTHCHECK_URL");
        assertTrue(healthCheck(marathonRequestUrl).getStatusLine().getStatusCode() == 200);
    }
    
    public HttpResponse healthCheck(final String requestUrl) throws IOException {
        final HttpGet request = new HttpGet(requestUrl);
        final HttpClient httpClient = HttpClients.createDefault();
        final HttpResponse response = httpClient.execute(request);
        request.releaseConnection();

        if (response.getEntity() == null) {
            fail("There was no content returned from the HealthCheck HTTP Get Request");
        }
        
        return response;
    }
}
