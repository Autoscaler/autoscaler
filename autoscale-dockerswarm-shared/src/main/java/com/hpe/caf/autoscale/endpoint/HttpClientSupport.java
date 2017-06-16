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
package com.hpe.caf.autoscale.endpoint;

import java.io.IOException;
import java.io.InputStream;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Objects;
import org.apache.http.HttpEntity;
import org.apache.http.HttpException;
import org.apache.http.HttpHost;
import org.apache.http.HttpRequest;
import org.apache.http.HttpRequestInterceptor;
import org.apache.http.NameValuePair;
import org.apache.http.ParseException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpEntityEnclosingRequestBase;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.client.utils.URIBuilder;
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

    public HttpClientSupport(final URL endpoint) throws HttpClientException
    {
        Objects.requireNonNull(endpoint);
        url = endpoint;

        builder = HttpClientBuilder.create();

        try {
            // Adhere to the standard NO_PROXY / HTTP_PROXY / HTTPS_PROXY environment info.
            // and use it for communication with our http endpoint.
            final URL proxyEndpoint = HttpProxySupport.getProxyAsUrl(endpoint);
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

    public <T> T issueRequest(final RequestTemplate requestInformation, Class<T> responseType, final String requestAction) throws HttpClientException
    {
        try (CloseableHttpClient client = builder.build()) {

            // do a first pass, and update the requestInformation information place holders, IF any with params
            final URIBuilder uriBuilder = buildURI(requestInformation, url.toString());

            HttpUriRequest uriRequest = getUriRequest(requestAction, uriBuilder);

            if (!requestInformation.getHeaders().isEmpty()) {
                for (NameValuePair header : requestInformation.getHeaders()) {
                    uriRequest.addHeader(header.getName(), header.getValue());
                }
            }

            // in addition, some request types support entities as well, check now.
            if (requestInformation.getEntity() != null) {
                HttpEntityEnclosingRequestBase entityRequestType = ((HttpEntityEnclosingRequestBase) uriRequest);
                if (entityRequestType == null) {
                    throw new HttpClientException(
                        "Unsupport request action type is being used which doesn't support HttpEntityRequests action: " + requestAction);
                }
                ((HttpEntityEnclosingRequestBase) uriRequest).setEntity(requestInformation.getEntity());
            }

            try (CloseableHttpResponse response = client.execute(uriRequest)) {

                checkForError(response);
                HttpEntity entity = response.getEntity();

                // If the response does not enclose an entity, there is no need
                // to bother about connection release
                if (entity == null) {
                    throw new HttpClientException("Failed to receive a valid http entity in the response.");
                }

                String content = EntityUtils.toString(entity);

                // Stream looks too large for memory so leaving in steam, and forget leaving the content about for debugging
                if (entity.getContentLength() > 20000) {
                    LOG.info("Streaming directly as dealing with a large response of size: " + entity.getContentLength());
                    try (InputStream is = entity.getContent()) {
                        try {
                            return JsonSerialization.readValue(is, responseType);
                        } catch (IOException e) {

                            // catch the exception, log out info for debug reasons, and continue, only for repeatable stream, otherwise
                            // we can't requestInformation it at this point, its gone.
                            LOG.debug("IOException during http request, response stream which is too large to debug.");
                            throw e;
                        }
                    }
                }

                try {
                    return JsonSerialization.readValue(content, responseType);
                } catch (IOException e) {

                    // catch the exception, log out info for debug reasons, and continue, only for repeatable stream, otherwise
                    // we can't requestInformation it at this point, its gone.
                    LOG.debug("IOException during http request, response contains text: {%s}", content);
                    throw e;
                }

            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        } catch (URISyntaxException ex) {
            throw new RuntimeException(ex);
        }

    }

    private void checkForError(final CloseableHttpResponse response) throws IOException, ParseException, HttpClientException
    {
        if (response.getStatusLine().getStatusCode() != 200) {

            // Check if we have  a valid http error message in the response
            HttpEntity entity = response.getEntity();
            if (entity != null) {
                String content = EntityUtils.toString(entity);
                LOG.error("HttpRequest failed with code: {} reason: {}",
                          response.getStatusLine().getStatusCode(), response.getStatusLine().getReasonPhrase());
                LOG.error("Http error response: {}", content);
            }
            throw new HttpClientException(
                String.format("Request failed with code: {%s} reason: {%s}",
                              response.getStatusLine().getStatusCode(), response.getStatusLine().getReasonPhrase()));
        }
    }

    /**
     * Get the appropriate HttpRequest type object depending upon the request action type e.g. GET / POST etc
     *
     * @param requestAction
     * @param uriBuilder
     * @return
     * @throws HttpClientException
     * @throws URISyntaxException
     */
    private HttpUriRequest getUriRequest(final String requestAction, final URIBuilder uriBuilder) throws HttpClientException, URISyntaxException
    {
        switch (requestAction) {
            case "POST":
                return new HttpPost(uriBuilder.build());

            case "GET":
                return new HttpGet(uriBuilder.build());
            default:
                throw new HttpClientException("Invalid request action type: " + requestAction);
        }
    }

    /**
     * Create a uriBuilder to be used by HttpRequest which contain correctly encoded parameters either in the path, or as query
     * parameters, with correct encoding.
     *
     * @param request
     * @return
     * @throws URISyntaxException
     */
    public static URIBuilder buildURI(final RequestTemplate request, final String url) throws URISyntaxException
    {
        String requestInfo = request.getRequestUrl();

        Collection<NameValuePair> paramsAlreadyProcessed = new ArrayList<>();

        URIBuilder uriBuilder = null;

        if (request.getParameters().isEmpty()) {
            // if we have no params, then just return a simple builder now.
            return new URIBuilder(url + requestInfo);
        }

        // Run a first pass for path type params.
        for (NameValuePair param : request.getParameters()) {

            // Some parameters are to be part of the URL instead of passed as part of query string.
            // check which this is now.
            if (!requestInfo.contains(String.format("{%s}", param.getName()))) {
                continue;
            }

            // the url contains this parameter, as such replace it now instead of adding as a
            // query param
            requestInfo = requestInfo.replace(String.format("{%s}", param.getName()), param.getValue());
            // now the param has been processed remove it from the list of params.
            paramsAlreadyProcessed.add(param);
        }

        // Now create a URIBuilder, and add the rest of the parameters as query params.
        uriBuilder = new URIBuilder(url + requestInfo);

        Collection<NameValuePair> remainingParams = request.getParameters();
        // skip all params already processed.
        remainingParams.removeAll(paramsAlreadyProcessed);

        // use paramsToProcess as any path type params have already been removed.
        for (NameValuePair param : remainingParams) {

            uriBuilder.setParameter(param.getName(), param.getValue());
        }

        return uriBuilder;
    }

}
