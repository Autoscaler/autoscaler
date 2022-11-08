/*
 * Copyright 2015-2022 Micro Focus or one of its affiliates.
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
package com.github.autoscaler.workload.rabbit.loadbalancing.redistribution.management;

import com.squareup.okhttp.OkHttpClient;
import retrofit.ErrorHandler;
import retrofit.RestAdapter;
import retrofit.RetrofitError;
import retrofit.client.OkClient;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.concurrent.TimeUnit;

public class RabbitManagementApi <T> {

    private static final int READ_TIMEOUT_SECONDS = 10;
    private static final int CONNECT_TIMEOUT_SECONDS = 10;
    
    private T api;

    public RabbitManagementApi(final Class<T> apiType, final String endpoint, final String user, 
                               final String password) {

        final OkHttpClient okHttpClient = new OkHttpClient();
        okHttpClient.setReadTimeout(READ_TIMEOUT_SECONDS, TimeUnit.SECONDS);
        okHttpClient.setConnectTimeout(CONNECT_TIMEOUT_SECONDS, TimeUnit.SECONDS);
        final RestAdapter.Builder restAdapterBuilder
                = new RestAdapter.Builder().setEndpoint(endpoint).setClient(new OkClient(okHttpClient));
        restAdapterBuilder.setRequestInterceptor(requestFacade -> {
            final String credentials = user + ":" + password;
            final String authorizationHeaderValue
                    = "Basic " + Base64.getEncoder().encodeToString(credentials.getBytes(StandardCharsets.UTF_8));
            requestFacade.addHeader("Accept", "application/json");
            requestFacade.addHeader("Authorization", authorizationHeaderValue);
        });
        restAdapterBuilder.setErrorHandler(new RabbitManagementApi.RabbitApiErrorHandler());
        final RestAdapter restAdapter = restAdapterBuilder.build();
        api = restAdapter.create(apiType);
    }
    
    public T getApi() {
        return api;
    }

    private static class RabbitApiErrorHandler implements ErrorHandler
    {
        @Override
        public Throwable handleError(final RetrofitError retrofitError)
        {
            return new RuntimeException("RabbitMQ management API error ", retrofitError);
        }
    }

}
