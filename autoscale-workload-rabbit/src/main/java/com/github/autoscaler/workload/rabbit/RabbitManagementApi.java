/*
 * Copyright 2015-2024 Open Text.
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
package com.github.autoscaler.workload.rabbit;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

import javax.ws.rs.ProcessingException;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.Invocation;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import com.github.autoscaler.api.QueueNotFoundException;
import com.github.autoscaler.api.ScalerException;
import com.google.common.net.UrlEscapers;

final class RabbitManagementApi
{
    private final Client client;
    private final String endpoint;
    private final String authorizationHeaderValue;

    public RabbitManagementApi(final Client restClient, final String restEndpoint, final String restAuthorizationHeaderValue)
    {
        this.client = restClient;
        this.endpoint = restEndpoint;
        this.authorizationHeaderValue = restAuthorizationHeaderValue;
    }

    public Response getNodeStatus() throws ScalerException
    {
        try {
            final Invocation.Builder builder = client.target(endpoint + "/api/nodes/")
                    .request(MediaType.APPLICATION_JSON)
                    .header(HttpHeaders.AUTHORIZATION, authorizationHeaderValue);
            return builder.get();
        } catch (final ProcessingException e) {
            throw new ScalerException("Failed to contact RabbitMQ management API", e);
        }
    }

    public Response getQueueStatus(final String vhost, final String queueName)
        throws ScalerException
    {
        final String url
            = endpoint
                + "/api/queues/"
                + UrlEscapers.urlPathSegmentEscaper().escape(vhost) + "/"
                + UrlEscapers.urlPathSegmentEscaper().escape(queueName);
        try {
            final Invocation.Builder builder
                = client
                    .target(url)
                    .request(MediaType.APPLICATION_JSON)
                    .header(HttpHeaders.AUTHORIZATION, authorizationHeaderValue);

            final Response response = builder.get();

            final int status = response.getStatus();
            if (status == 200) {
                return response;
            } else if (status == 404) {
                throw new QueueNotFoundException(url);
            } else {
                throw new ScalerException(
                        "Failed to contact RabbitMQ management API using url " + url + ", status " + status);
            }
        } catch (final ProcessingException e) {
            throw new ScalerException("Failed to contact RabbitMQ management API using url " + url
                   + ". RabbitMQ could be unavailable, will retry.", e);
        }
    }

    // The `use_regex` query param only works if pagination is used as well.
    // See: https://groups.google.com/g/rabbitmq-users/c/Lgad24orwog/m/E_zoUtB3BQAJ
    public PagedQueues getPagedQueues(
            final String vhost,
            final String nameRegex,
            final int page,
            final int pageSize,
            final String columnsCsvString)
            throws ScalerException
    {
        final String url
            = endpoint + "/api/queues/" + UrlEscapers.urlPathSegmentEscaper().escape(vhost) + "?use_regex=true";

        try {
            final Invocation.Builder builder = client.target(url)
                    .queryParam("name", URLEncoder.encode(nameRegex, StandardCharsets.UTF_8.name()))
                    .queryParam("page", page).queryParam("page_size", pageSize)
                    .queryParam("columns", URLEncoder.encode(columnsCsvString, StandardCharsets.UTF_8.name()))
                    .request(MediaType.APPLICATION_JSON).header(HttpHeaders.AUTHORIZATION, authorizationHeaderValue);

            final Response response = builder.get();

            final int status = response.getStatus();
            if (status == 200) {
                return response.readEntity(PagedQueues.class);
            } else if (status == 404) {
                throw new QueueNotFoundException(url);
            } else {
                throw new ScalerException(
                        "Failed to contact RabbitMQ management API using url " + url + ", status " + status);
            }
        } catch (final ProcessingException | UnsupportedEncodingException e) {
            throw new ScalerException("Failed to contact RabbitMQ management API using url " + url
                    + ". RabbitMQ could be unavailable, will retry.", e);
        }
    }
}
