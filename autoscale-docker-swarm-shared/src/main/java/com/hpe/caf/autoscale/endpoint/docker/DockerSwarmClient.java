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
package com.hpe.caf.autoscale.endpoint.docker;

import com.hpe.caf.autoscale.DockerSwarmAutoscaleConfiguration;
import com.hpe.caf.autoscale.endpoint.HttpClientException;
import com.hpe.caf.autoscale.endpoint.HttpClientSupport;
import com.hpe.caf.autoscale.endpoint.Param;
import com.hpe.caf.autoscale.endpoint.RequestLine;
import com.hpe.caf.autoscale.endpoint.RequestTemplate;
import com.jayway.jsonpath.DocumentContext;
import java.lang.reflect.Method;
import java.security.InvalidParameterException;
import java.net.MalformedURLException;
import java.net.URL;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static com.hpe.caf.autoscale.reflection.ReflectionAssistance.getCallingMethod;
import java.lang.reflect.Parameter;
import java.util.ArrayList;
import java.util.List;
import org.apache.http.NameValuePair;
import org.apache.http.message.BasicNameValuePair;

/**
 * A simple class which contains the implementations of the DockerSwarm interface.
 *
 */
public class DockerSwarmClient implements DockerSwarm
{
    private final static Logger LOG = LoggerFactory.getLogger(DockerSwarmClient.class);
    private final HttpClientSupport httpClient;

    public static DockerSwarm getInstance(final DockerSwarmAutoscaleConfiguration config)
    {
        return new DockerSwarmClient(config);
    }

    public static DockerSwarm getInstance(final HttpClientSupport httpClient)
    {
        return new DockerSwarmClient(httpClient);
    }

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

    // For mocking, here is an alternative constructor for unit tests.
    private DockerSwarmClient(final HttpClientSupport httpClientSupport)
    {
        this.httpClient = httpClientSupport;
    }

    /**
     * *
     * Start REST implementations
     */
    /**
     * get a list of all services being run in the swarm.
     *
     * @return
     * @throws HttpClientException
     */
    @Override
    @RequestLine(responseType = DocumentContext.class, action = "GET", request = "/services")
    public DocumentContext getServices() throws HttpClientException
    {
        return invokeAction(this.getClass(), getCallingMethod(this.getClass()), DocumentContext.class);
    }

    /**
     *
     * @param filters
     * @return
     * @throws HttpClientException
     */
    @Override
    @RequestLine(responseType = DocumentContext.class, action = "GET", request = "/services")
    public DocumentContext getServicesFiltered(@Param("filters") final String filters) throws HttpClientException
    {
        return invokeAction(this.getClass(), getCallingMethod(this.getClass()), DocumentContext.class, filters);
    }

    /**
     *
     * @param filterByType
     * @param filterKeyName
     * @param filterKeyValue
     * @return
     */
    @Override
    public String buildServiceFilter(final String filterByType, final String filterKeyName, final String filterKeyValue)
    {
        String filterByLabel = String.format("{\"%s\":{\"%s=%s\":true}}", filterByType, filterKeyName, filterKeyValue);

        LOG.trace("Built service filter: " + filterByLabel);
        return filterByLabel;
    }

    /**
     * *
     * Private methods begin
     */
    private <T> T invokeAction(final Class<?> classToSearch, final Method callingMethod, Class<T> responseType,
                               final Object... requestParameterValues) throws InvalidParameterException, HttpClientException
    {
        RequestLine requestLine = getRequestLineInformation(classToSearch, callingMethod);
        List<NameValuePair> parameters = getRequestParameterInformation(classToSearch, callingMethod, requestParameterValues);

        RequestTemplate request = new RequestTemplate(requestLine.request());
        if (parameters != null && parameters.size() > 0) {
            request.setParameters(parameters);
        }

        switch (requestLine.action()) {
            case "GET":
                return httpClient.getRequest(request, responseType);
            default:
                throw new HttpClientException("Invalid request action type: " + requestLine.action());
        }
    }

    private List<NameValuePair> getRequestParameterInformation(final Class<?> classToSearch, final Method methodToSearch,
                                                               final Object... requestParameterValues) throws InvalidParameterException
    {
        if (methodToSearch.getParameterCount() == 0) {
            // no params on this object, return nothing.
            return null;
        }

        List<NameValuePair> requestParameters = new ArrayList<>();

        Parameter[] parameters = methodToSearch.getParameters();

        int currentParamIndex = 0;
        for (Parameter param : parameters) {
            // all params that are for the actual query are marked with the @Param annotation 
            // TREV TODO ensure headers aren't picked up here also!
            Param reqParamInfo = param.getAnnotation(Param.class);

            if (reqParamInfo == null) {
                continue;
            }

            String reqParamName = reqParamInfo.value();
            if (currentParamIndex > requestParameterValues.length) {
                throw new InvalidParameterException(
                    "Unable to find a match in the actual parameters supplied, for each parameter in the signature, namely item: " + reqParamName);
            }

            Object reqParamValue = requestParameterValues[currentParamIndex++];
            if (reqParamValue == null) {
                // not sure what to about null as a request value, maybe pass word null?
                // for now throwing, and can implement when needed.
                throw new InvalidParameterException(
                    "Unable to pass null parameter value for now unsupported see paramater name: " + reqParamName);
            }

            // so now we have the name of the param, and the actual value of it, create the parameter object
            requestParameters.add(new BasicNameValuePair(reqParamName, reqParamValue.toString()));
        }

        return requestParameters;
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
