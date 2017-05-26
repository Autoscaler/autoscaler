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
package com.hpe.caf.autoscale.scaler.endpoint;

import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Objects;
import java.util.logging.Level;
import org.apache.http.HttpEntity;
import org.apache.http.HttpException;
import org.apache.http.HttpHost;
import org.apache.http.HttpRequest;
import org.apache.http.HttpRequestInterceptor;
import org.apache.http.NameValuePair;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.protocol.HttpContext;
import org.apache.http.util.EntityUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Support class, which allows people to easily construct and issue REST based requests to an endpoing.
 * 
 */
public class HttpClientSupport
{
    private static final Logger LOG = LoggerFactory.getLogger(HttpClient.class);

    private final HttpClientBuilder builder;
    private final URL url;

    public static class TypedList extends ArrayList<String>
    {
    }

    public static class ObjectList extends ArrayList<Object>
    {
    }

    public HttpClientSupport(final URL endpoint, final URL proxyEndpoint ) throws HttpClientException
    {
        Objects.requireNonNull(endpoint);
        url = endpoint;
        
        builder = HttpClientBuilder.create();

        try {
            // if we have a proxy setup in configuration then use it for our communication with docker.
            if (proxyEndpoint != null && proxyEndpoint.toURI() != null) {
                builder.setProxy(HttpHost.create(proxyEndpoint.toString()));
            }
        } catch (URISyntaxException ex) {
            // invalid proxy specified just log out this information, but continue on its not fatal to the workings!
            LOG.error("Invalid proxy endpoint configuration, so continuing on without it - Exception Details: ", ex);
        }

        builder.addInterceptorFirst(new HttpClientHeadersInterceptor());
    }

    /**
     * Add some standard headers to every request that is made.
     */
    static class HttpClientHeadersInterceptor implements HttpRequestInterceptor
    {
        @Override
        public void process(HttpRequest request, HttpContext context) throws HttpException, IOException
        {
            request.addHeader("Accept", "application/json");
            request.addHeader("Content-Type", "application/json");
        }
    }

    public <T> T getRequest(final RequestTemplate request, Class<T> responseType) throws HttpClientException
    {
        try (CloseableHttpClient client = builder.build()) {
            HttpGet get = new HttpGet(url + request.getRequestUrl());

            if (!request.getHeaders().isEmpty()) {
                for (NameValuePair header : request.getHeaders()) {
                    get.addHeader(header.getName(), header.getValue());
                }
            }

            try (CloseableHttpResponse response = client.execute(get)) {

                if (response.getStatusLine().getStatusCode() != 200) {
                    throw new HttpClientException(
                        String.format("Request failed with code: {%s} reason: {%s}",
                                      response.getStatusLine().getStatusCode(), response.getStatusLine().getReasonPhrase()));
                }
                HttpEntity entity = response.getEntity();

                // If the response does not enclose an entity, there is no need
                // to bother about connection release
                if (entity == null) {
                    throw new HttpClientException("Failed to receive a valid http entity in the response.");
                }

                String content = EntityUtils.toString(entity);

                // TREV TODO Decide if stream is very large to just process it as a stream, and dont keep the content about for debugging?
                if (entity.getContentLength() > 20000) {
                    LOG.info("Streaming directly as dealing with a large response of size: " + entity.getContentLength());
                    try (InputStream is = entity.getContent()) {
                        try {
                            return JsonSerialization.readValue(is, responseType);
                        } catch (IOException e) {

                            // catch the exception, log out info for debug reasons, and continue, only for repeatable stream, otherwise
                            // we can't request it at this point, its gone.
                            LOG.debug("IOException during http request, response stream which is too large to debug.");
                            throw e;
                        }
                    }
                }

                
                try {
                    return JsonSerialization.readValue(content, responseType);
                } catch (IOException e) {

                    // catch the exception, log out info for debug reasons, and continue, only for repeatable stream, otherwise
                    // we can't request it at this point, its gone.
                    LOG.debug("IOException during http request, response contains text: {%s}", content);
                    throw e;
                }

            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

    }
//
//    private void testParsers(String content) throws HttpClientException
//    {
//        // json-simple ( json parser with simple .get operations, fast...
//        JSONParser parser = new JSONParser();
//        try {
//            Object parseObj = parser.parse(content);
//            // Now what is it -> JsonArray / JsonObj ?
//            if (parseObj instanceof JSONArray) {
//                JSONArray jsonArray = (JSONArray) parseObj;
//                System.out.println("Got the jsonArray: " + jsonArray);
//                JSONObject objWithID = (JSONObject) jsonArray.get(0);
//                Object id = objWithID.get("ID");
//            } else if (parseObj instanceof JSONObject) {
//                JSONObject jsonObj = (JSONObject) parseObj;
//                System.out.println("Got the jsonObject: " + jsonObj);
//                // try query for number / id.
//            }
//            
//            // use this instead of Jackson?
//        } catch (ParseException ex) {
//            LOG.debug("IOException during http request, response contains text: {%s}", content);
//            throw new HttpClientException("Failed to parse the response into JSON.", ex);
//        }
//        
//        // jayway jsonPath using google default JsonProvider which is v quick ( under Apache license )
//        DocumentContext document = JsonPath.parse(content);
//        List<String> ids = document.read("$..ID");
//        Object jsonObj = document.json();
//        if ( jsonObj instanceof JSONArray )
//        {
//            JSONArray arrayNodes = ((JSONArray) jsonObj);
//            long numElements = arrayNodes.size();
//        }
//    }
}
