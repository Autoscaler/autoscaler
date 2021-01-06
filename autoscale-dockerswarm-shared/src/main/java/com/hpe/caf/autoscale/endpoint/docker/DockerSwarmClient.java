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
package com.hpe.caf.autoscale.endpoint.docker;

import com.hpe.caf.autoscale.DockerSwarmAutoscaleConfiguration;
import com.hpe.caf.autoscale.endpoint.Entity;
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
import java.lang.reflect.Field;
import java.lang.reflect.Parameter;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import org.apache.http.HttpEntity;
import org.apache.http.NameValuePair;
import org.apache.http.entity.StringEntity;
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

        httpClient = new HttpClientSupport(endpoint);
    }

    /**
     * For mocking, here is an alternative constructor for unit tests.
     *
     * @param httpClientSupport
     */
    private DockerSwarmClient(final HttpClientSupport httpClientSupport)
    {
        this.httpClient = httpClientSupport;
    }

    /**
     *
     * Start REST implementations
     *
     */
    /**
     * get a list of all services being run in the swarm.
     *
     * @return
     * @throws HttpClientException
     */
    @Override
    @RequestLine(responseType = DocumentContext.class, action = "GET", request = "/services")
    public DocumentContext
        getServices() throws HttpClientException
    {
        return invokeAction(this.getClass(), getCallingMethod(this.getClass()), DocumentContext.class
        );
    }

    /**
     *
     * @param filters
     * @return
     * @throws HttpClientException
     */
    @Override
    @RequestLine(responseType = DocumentContext.class, action = "GET", request = "/services")
    public DocumentContext getServicesFiltered(@Param("filters")
        final String filters) throws HttpClientException
    {
        return invokeAction(this.getClass(), getCallingMethod(this.getClass()), DocumentContext.class,
                            filters);
    }

    /**
     * Return details of the specific service specified by the serviceId.
     *
     * @param serviceId
     * @return
     * @throws HttpClientException
     */
    @Override
    @RequestLine(responseType = DocumentContext.class, action = "GET", request = "/services/{serviceId}")
    public DocumentContext getService(@Param("serviceId")
        final String serviceId) throws HttpClientException
    {
        return invokeAction(this.getClass(), getCallingMethod(this.getClass()), DocumentContext.class,
                            serviceId);
    }

    /**
     * Update / scale the specific service using the new specification given
     *
     * @param serviceId The service to update
     * @param versionId The previous specification version that we are updating to prevent race conditions.
     * @param serviceSpecification The new service specification
     * @throws HttpClientException
     */
    @Override
    @RequestLine(responseType = void.class, action = "POST", request = "/services/{serviceId}/update?version={versionId}")
    public void updateService(@Param("serviceId")
        final String serviceId, @Param("versionId")
                              final int versionId,
                              @Entity("specification")
                              final String serviceSpecification) throws HttpClientException
    {
        invokeAction(this.getClass(), getCallingMethod(this.getClass()), DocumentContext.class,
                     serviceId, versionId, serviceSpecification);
    }

    /**
     * get a list of all tasks being run in the swarm.
     *
     * @return
     * @throws HttpClientException
     */
    @Override
    @RequestLine(responseType = DocumentContext.class, action = "GET", request = "/tasks")
    public DocumentContext
        getTasks() throws HttpClientException
    {
        return invokeAction(this.getClass(), getCallingMethod(this.getClass()), DocumentContext.class
        );
    }

    /**
     * Get a list of tasks, filtered by some objects.
     *
     * @param filters
     * @return
     * @throws HttpClientException
     */
    @Override
    @RequestLine(responseType = DocumentContext.class, action = "GET", request = "/tasks")
    public DocumentContext getTasksFiltered(@Param("filters")
        final String filters) throws HttpClientException
    {
        return invokeAction(this.getClass(), getCallingMethod(this.getClass()), DocumentContext.class,
                            filters);
    }

    /**
     * Return details of the specific task specified by the taskId.
     *
     * @param taskId
     * @return
     * @throws HttpClientException
     */
    @Override
    @RequestLine(responseType = DocumentContext.class, action = "GET", request = "/tasks/{taskId}")
    public DocumentContext getTask(@Param("taskId")
        final String taskId) throws HttpClientException
    {
        return invokeAction(this.getClass(), getCallingMethod(this.getClass()), DocumentContext.class,
                            taskId);
    }

    /**
     * *
     * Private methods begin
     */
    private <T> T invokeAction(final Class<?> classToSearch, final Method callingMethod, Class<T> responseType,
                               final Object... requestParameterValues) throws InvalidParameterException, HttpClientException
    {
        final RequestLine requestLine = getRequestLineInformation(classToSearch, callingMethod);
        final List<NameValuePair> parameters = getRequestParameterInformation(classToSearch, callingMethod, requestParameterValues);

        RequestTemplate request = new RequestTemplate(requestLine.request());
        if (parameters != null && parameters.size() > 0) {
            request.setParameters(parameters);
        }

        final HttpEntity httpEntity = getRequestEntityInformation(classToSearch, callingMethod, requestParameterValues);
        if (httpEntity != null) {
            request.setEntity(httpEntity);
        }

        return httpClient.issueRequest(request, responseType, requestLine.action());

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
            Param reqParamInfo = param.getAnnotation(Param.class
            );

            if (reqParamInfo == null) {
                continue;
            }

            String reqParamName = reqParamInfo.value();
            if (requestParameterValues.length == 0 || currentParamIndex > requestParameterValues.length) {
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

    private HttpEntity getRequestEntityInformation(final Class<?> classToSearch, final Method methodToSearch,
                                                   final Object... requestParameterValues) throws InvalidParameterException, HttpClientException
    {
        if (methodToSearch.getParameterCount() == 0) {
            // no params on this object, return nothing.
            return null;
        }

        List<NameValuePair> requestParameters = new ArrayList<>();

        Parameter[] parameters = methodToSearch.getParameters();

        for (Parameter param : parameters) {
            // the correct parameter to be used as an entity will be marked with the Entity annotation.            
            final Entity reqEntityInfo = param.getAnnotation(Entity.class
            );

            if (reqEntityInfo == null) {
                continue;
            }

            final String reqEntityName = reqEntityInfo.value();

            // obtain the current index of this parameter in the method var args list.
            final int currentParamIndex = getPrivateField(param, "index");

            if (requestParameterValues.length == 0 || currentParamIndex > requestParameterValues.length) {
                throw new InvalidParameterException(
                    "Unable to find a match in the actual parameters supplied, for each parameter in the signature, namely item: " + reqEntityName);
            }

            Object reqParamValue = requestParameterValues[currentParamIndex];
            if (reqParamValue == null) {
                // not sure what to about null as a request value, maybe pass word null?
                // for now throwing, and can implement when needed.
                throw new InvalidParameterException(
                    "Unable to pass null parameter value for now unsupported see paramater name: " + reqEntityName);
            }

            // so now we have the name of the param, and the actual value of it, create the Entity object
            return new StringEntity(reqParamValue.toString(), StandardCharsets.UTF_8);
        }

        return null;
    }

    private int getPrivateField(final Parameter param, final String paramName) throws HttpClientException, SecurityException
    {
        Field indexField;
        try {
            indexField = param.getClass().getDeclaredField(paramName);
        } catch (NoSuchFieldException | SecurityException ex) {
            LOG.error("error recieved while trying to find out the current parameter index", ex);
            throw new HttpClientException("Unable to make the HttpRequest as we cannot find the required request entity information",
                                          ex);
        }
        indexField.setAccessible(true);
        try {
            return (int) indexField.get(param);
        } catch (IllegalArgumentException | IllegalAccessException ex) {
            LOG.error("error recieved while trying to get access to current parameter index", ex);
            throw new HttpClientException("Unable to make the HttpRequest as we cannot access the required request entity information",
                                          ex);
        }
    }

    private RequestLine getRequestLineInformation(final Class<?> classToSearch, final Method methodToSearch) throws InvalidParameterException
    {
        RequestLine requestLineInfo = methodToSearch.getAnnotation(RequestLine.class
        );

        // The request can be made up for a type / url / params.
        // GET /services "id=123"
        String gotInfo = requestLineInfo.toString();
        LOG.debug("Resolved request: " + gotInfo);
        return requestLineInfo;
    }

}
