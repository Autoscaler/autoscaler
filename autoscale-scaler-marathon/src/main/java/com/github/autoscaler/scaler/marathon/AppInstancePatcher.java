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
package com.github.autoscaler.scaler.marathon;

import com.github.autoscaler.api.ScalerException;
import com.google.gson.JsonObject;
import com.google.gson.JsonArray;
import java.io.IOException;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPatch;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Arrays;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.message.BasicNameValuePair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class AppInstancePatcher {

    private final static Logger LOG = LoggerFactory.getLogger(AppInstancePatcher.class);
    private final URI marathonUri;

    public AppInstancePatcher(final URI marathonUri){
        this.marathonUri = marathonUri;
    }

    public void patchInstances(final String appId, final int instances) throws ScalerException {
        patchInstances(appId, instances, false);
    }

    private void patchInstances(final String appId, final int instances, final boolean force) throws ScalerException {
        final JsonObject details = new JsonObject();
        details.addProperty("id", appId);
        details.addProperty("instances", instances);

        final JsonArray appArray = new JsonArray();
        appArray.add(details);
        patchInstance(appArray, appId, instances, force);
    }
    
    private void patchInstance(final JsonArray appArray, final String appId, final int instances, final boolean force)
        throws ScalerException
    {
        int count = 0;
        boolean patched = false;
        while (!patched) {
            try (final CloseableHttpClient client = HttpClientBuilder.create().build()) {
                final URIBuilder uriBuilder = new URIBuilder(marathonUri).setPath("/v2/apps");
                uriBuilder.setParameters(Arrays.asList(new BasicNameValuePair("force", Boolean.toString(force))));
                final HttpPatch patch = new HttpPatch(uriBuilder.build());
                patch.setEntity(new StringEntity(appArray.toString(), ContentType.APPLICATION_JSON));
                try(final CloseableHttpResponse response = client.execute(patch)) {
                    if (!force && response.getStatusLine().getStatusCode() == 409) {
                        patchInstances(appId, instances, true);
                        return;
                    }
                    if (response.getStatusLine().getStatusCode() != 200) {
                        if (response.getStatusLine().getStatusCode() == 503) {
                            LOG.error(String.format("A temporary error occured while attempting to patch service %s. "
                                    + "Patch request returned a 503 status, patching will be reattempted.", appId));
                            count++;
                            final int sleepTime = count * 1000;
                            final int timeToSleep = sleepTime < 10000 ? sleepTime : 10000;
                            Thread.sleep(timeToSleep);
                        } else {
                            LOG.error("Response code: " + response.getStatusLine().getStatusCode());
                            /*
                            SCMOD-6524 - FALSE POSITIVE on FORTIFY SCAN for Log forging. The StatusLine object has
                            already some constraints on what it accepts, and it consists of the protocol version
                            followed by a numeric status code and its associated textual phrase, with each element
                            separated by SP characters. No CR or LF is allowed except in the final CRLF sequence.
                            <pre>
                                Status-Line = HTTP-Version SP Status-Code SP Reason-Phrase CRLF
                            </pre>
                             */
                            LOG.error("Response Phrase: " + response.getStatusLine().getReasonPhrase());
                            throw new ScalerException(response.getStatusLine().getReasonPhrase());
                        }
                    }
                }
            } catch (final URISyntaxException | IOException ex) {
                throw new ScalerException(String.format("Exception patching %s to %s instances.", appId, instances), ex);
            } catch (final InterruptedException ex) {
                // Set interupted flag
                Thread.currentThread().interrupt();
                // Throw exception to suppress further calls from current scaler thread until after scaler thread refresh
                throw new ScalerException("An error occured during an attempt to have the main thread sleep beofre retrying patch command",
                                          ex);
            }
            LOG.debug(String.format("Service %s patched successfully.", appId));
            patched = true;
        }
    }
}
