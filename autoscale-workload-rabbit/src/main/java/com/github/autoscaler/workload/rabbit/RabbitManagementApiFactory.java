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

import java.nio.charset.StandardCharsets;
import java.util.Base64;

import jakarta.ws.rs.client.Client;
import jakarta.ws.rs.client.ClientBuilder;

import org.glassfish.jersey.client.ClientProperties;

final class RabbitManagementApiFactory
{
    private static final int READ_TIMEOUT_MILLISECONDS = 10000;
    private static final int CONNECT_TIMEOUT_MILLISECONDS = 10000;

    public static RabbitManagementApi create(final String endpoint, final String user, final String password)
    {

        final Client client = ClientBuilder.newClient();
        client.register(JacksonConfigurator.class);
        client.property(ClientProperties.CONNECT_TIMEOUT, CONNECT_TIMEOUT_MILLISECONDS);
        client.property(ClientProperties.READ_TIMEOUT, READ_TIMEOUT_MILLISECONDS);
        final String credentials = user + ":" + password;
        final String authorizationHeaderValue
            = "Basic " + Base64.getEncoder().encodeToString(credentials.getBytes(StandardCharsets.UTF_8));
        return new RabbitManagementApi(client, endpoint, authorizationHeaderValue);
    }

    private RabbitManagementApiFactory()
    {
    }
}
