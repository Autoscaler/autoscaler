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

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.net.URLEncoder;
import static java.nio.charset.StandardCharsets.UTF_8;
import java.security.InvalidParameterException;
import java.util.ArrayList;
import java.util.Collection;
import org.apache.http.NameValuePair;

/**
 *
 */
public class RequestTemplate
{

    private Collection<NameValuePair> headers = new ArrayList<>();

    private Collection<NameValuePair> parameters = new ArrayList<>();

    private String requestUrl;

    public RequestTemplate(final String requestUrl)
    {
        if (requestUrl == null || requestUrl.isEmpty()) {
            throw new InvalidParameterException("Invalid blank request url");
        }
        this.requestUrl = requestUrl;

        validateUrl();
    }

    private static String urlDecode(String arg)
    {
        try {
            return URLDecoder.decode(arg, UTF_8.name());
        } catch (UnsupportedEncodingException e) {
            throw new RuntimeException(e);
        }
    }

    private static String urlEncode(Object arg)
    {
        try {
            return URLEncoder.encode(String.valueOf(arg), UTF_8.name());
        } catch (UnsupportedEncodingException e) {
            throw new RuntimeException(e);
        }
    }

    public String getRequestUrl()
    {
        return requestUrl;
    }

    public void setRequestUrl(String requestUrl)
    {
        this.requestUrl = requestUrl;

        validateUrl();
    }

    public Collection<NameValuePair> getHeaders()
    {
        return headers;
    }

    public void setHeaders(Collection<NameValuePair> headers)
    {
        this.headers = headers;
    }

    public Collection<NameValuePair> getParameters()
    {
        return parameters;
    }

    public void setParameters(Collection<NameValuePair> parameters)
    {
        this.parameters = parameters;
    }

    private void validateUrl()
    {
        if (requestUrl == null || requestUrl.isEmpty()) {
            return;
        }

        // always make sure the request url we return starts with a slash to make appending simpler.
        if (requestUrl.startsWith("/")) {
            return;
        }
        requestUrl = "/" + requestUrl;
    }
}
