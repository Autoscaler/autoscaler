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

import com.google.gson.JsonObject;
import com.google.gson.JsonArray;
import com.hpe.caf.api.autoscale.ScalerException;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpPatch;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;

import java.net.URI;

public class AppInstancePatcher {

    private final URI marathonUri;

    public AppInstancePatcher(final URI marathonUri){
        this.marathonUri = marathonUri;
    }

    public void patchInstances(final String appId, int instances) throws ScalerException {
        patchInstances(appId, instances, false);
    }

    private void patchInstances(final String appId, final int instances, final boolean force) throws ScalerException {

        final JsonObject details = new JsonObject();
        details.addProperty("id", appId);
        details.addProperty("instances", instances);

        final JsonArray appArray = new JsonArray();
        appArray.add(details);
                
        try(final CloseableHttpClient client = HttpClientBuilder.create().build()){
            final HttpPatch patch = new HttpPatch(new Url(marathonUri, "/v2/apps"));
            patch.setEntity(new StringEntity(appArray.toString(), ContentType.APPLICATION_JSON));
            final HttpResponse response = client.execute(patch);
            if(!force && response.getStatusLine().getStatusCode()==409){
                patchInstances(appId, instances, true);
                return;
            }
            if(response.getStatusLine().getStatusCode()!=200){
                throw new ScalerException(response.getStatusLine().getReasonPhrase());
            }
        }
        catch (Exception ex){
            throw new ScalerException(String.format("Exception patching %s to %s instances.", appId, instances), ex);
        }
    }
}
