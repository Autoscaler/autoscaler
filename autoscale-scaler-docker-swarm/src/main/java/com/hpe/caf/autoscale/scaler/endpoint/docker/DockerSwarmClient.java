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
package com.hpe.caf.autoscale.scaler.endpoint.docker;

import com.hpe.caf.autoscale.DockerSwarmAutoscaleConfiguration;
import com.hpe.caf.autoscale.scaler.endpoint.HttpClientException;
import com.hpe.caf.autoscale.scaler.endpoint.HttpClientSupport;
import com.hpe.caf.autoscale.scaler.endpoint.HttpClientSupport.ObjectList;
import java.lang.reflect.Method;
import java.security.InvalidParameterException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.hpe.caf.autoscale.scaler.endpoint.HttpClientSupport.TypedList;
import com.hpe.caf.autoscale.scaler.endpoint.RequestLine;
import com.hpe.caf.autoscale.scaler.endpoint.RequestTemplate;
import com.jayway.jsonpath.DocumentContext;
import java.net.MalformedURLException;
import java.net.URL;

/**
 * A simple class which contains the implementations of the DockerSwarm interface.
 *
 */
public class DockerSwarmClient implements DockerSwarm
{
    private final static Logger LOG = LoggerFactory.getLogger(DockerSwarmClient.class);

    public static DockerSwarm getInstance(final DockerSwarmAutoscaleConfiguration config)
    {
        return new DockerSwarmClient(config);
    }

    private final HttpClientSupport httpClient;

    private DockerSwarmClient(final DockerSwarmAutoscaleConfiguration config)
    {
        URL endpoint = null;
        try {
            endpoint = new URL(config.getEndpoint());
        } catch (MalformedURLException ex) {
            LOG.error("Unable to construct a valid URL from the DockerSwarmAutoscaleConfiguration endpoint.", ex);
            throw new HttpClientException("Unable to construct a valid URL from the DockerSwarmAutoscaleConfiguration", ex);
        }

        URL proxyEndpoint = null;

        try {
            if (config.getProxyEndpoint() != null && !config.getProxyEndpoint().isEmpty()) {
                proxyEndpoint = new URL(config.getProxyEndpoint());
            }
        } catch (MalformedURLException ex) {
            LOG.error("Unable to construct a valid URL from the DockerSwarmAutoscaleConfiguration proxyEndpoint.", ex);
            throw new HttpClientException("Unable to construct a valid URL from the DockerSwarmAutoscaleConfiguration", ex);
        }

        httpClient = new HttpClientSupport(endpoint, proxyEndpoint);
    }

    @Override
    @RequestLine(responseType = DocumentContext.class, action = "GET", request = "/services")
    public DocumentContext getServices() throws HttpClientException
    {
        return invokeAction(this.getClass(), getCallingMethod(this.getClass()), DocumentContext.class);
    }

    public void getServicesFiltered(final String filters) throws HttpClientException
    {
        // TODO:
        throw new UnsupportedOperationException("Not supported yet.");
    }

    /**
     * *
     * Private methods begin
     */
    /**
     * Assists in finding out the name / method of the calling object for use with reflection.
     *
     * @param classToSearch
     * @return
     */
    private Method getCallingMethod(final Class<?> classToSearch)
    {
        // TREV TODO Decide -> work out method from call stack, or pass in
        // we want the method name of the caller of invokeAction / getRequestLineInfo
        final String methodName = Thread.currentThread().getStackTrace()[2].getMethodName();

        Method methodToSearch = null;
        try {
            methodToSearch = classToSearch.getMethod(methodName);
        } catch (NoSuchMethodException ex) {
            throw new RuntimeException("No such method found.", ex);
        } catch (SecurityException ex) {
            throw new RuntimeException(ex);
        }

        return methodToSearch;
    }

    private <T> T invokeAction(final Class<?> classToSearch, final Method callingMethod, Class<T> responseType) throws InvalidParameterException, HttpClientException
    {
        RequestLine requestLine = getRequestLineInformation(classToSearch, callingMethod);
        RequestTemplate request = new RequestTemplate(requestLine.request());
        switch (requestLine.action()) {
            case "GET":
                return httpClient.getRequest(request, responseType);
            default:
                throw new HttpClientException("Invalid request action type: " + requestLine.action());
        }
    }

    private RequestLine getRequestLineInformation(final Class<?> classToSearch, final Method methodToSearch) throws InvalidParameterException
    {
        RequestLine requestLineInfo = methodToSearch.getAnnotation(RequestLine.class);

        // The request can be made up for a type / url / params.
        // GET /services "id=123"
        String gotInfo = requestLineInfo.toString();
        LOG.debug("Resolved request: " + gotInfo);
        return requestLineInfo;
    }

}
