/*
 * Copyright 2015-2017 EntIT Software LLC, a Micro Focus company.
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

import java.net.InetSocketAddress;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.Proxy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Assistance in creating a proxy object for use within our own connection contexts. This is so users can have no_proxy type settings and
 * the connections created within the HttpClient will adhere to it.
 */
public class HttpProxySupport
{
    private static final Logger LOG = LoggerFactory.getLogger(HttpProxySupport.class);
    private static final String NO_PROXY = "NO_PROXY";
    private static final String HTTP_PROXY = "HTTP_PROXY";
    private static final String HTTPS_PROXY = "HTTPS_PROXY";

    public static Proxy getProxy(final URL webServiceEndpointUrl)
    {
        URL proxyURL = getProxyAsUrl(webServiceEndpointUrl);
        if (!proxyURL.getProtocol().startsWith("http")) {
            return null;
        }

        InetSocketAddress proxyInet = new InetSocketAddress(proxyURL.getHost(),
                                                            proxyURL.getPort());
        return new Proxy(Proxy.Type.HTTP, proxyInet);
    }

    public static URL getProxyAsUrl(final URL webServiceEndpointUrl)
        throws HttpClientException
    {
        String webserviceEndpointUrlProtocol = webServiceEndpointUrl.getProtocol();
        //  If the webservice endpoint is not included in no-proxy, depending on the webservice endpoint protocol
        //  set, return http or https proxy object. Else return null.
        final String noProxyList = getNoProxyList();
        if (noProxyList == null || !noProxyList.contains(webServiceEndpointUrl.getHost())) {
            LOG.debug(webServiceEndpointUrl.getHost() + " is not included in the list of no_proxy hosts. "
                + "Attempting to create " + webserviceEndpointUrlProtocol.toUpperCase() + " proxy");
            if (webserviceEndpointUrlProtocol.equals("http")) {
                // If a HTTP Proxy has been set and the WS Endpoint Protocol is HTTP, return a Proxy based upon it
                final String httpProxy = getHttpProxy();
                if (httpProxy != null && !httpProxy.isEmpty()) {
                    URL httpProxyUrl;
                    try {
                        httpProxyUrl = new URL(httpProxy);
                    } catch (final MalformedURLException mue) {
                        String errorMessage = "Unable to create URL for HTTP Proxy: " + httpProxy;
                        throw new HttpClientException(errorMessage, mue);
                    }
                    return httpProxyUrl;
                }
            } else if (webserviceEndpointUrlProtocol.equals("https")) {
                // If a HTTPS Proxy has been set and the WS Endpoint Protocol is HTTPS, return a Proxy based upon it
                final String httpsProxy = getHttpsProxy();
                if (httpsProxy != null && !httpsProxy.isEmpty()) {
                    URL httpsProxyUrl;
                    try {
                        httpsProxyUrl = new URL(httpsProxy);
                    } catch (final MalformedURLException mue) {
                        String errorMessage = "Unable to create URL for HTTPS Proxy: " + httpsProxy;
                        throw new HttpClientException(errorMessage, mue);
                    }
                    return httpsProxyUrl;
                }
            }
        }
        LOG.debug(webServiceEndpointUrl.getHost() + " is included in the list of no_proxy hosts or there are no HTTP "
            + "or HTTPS proxies set to base one upon.");
        return null;
    }

    private static String getNoProxyList()
    {
        String noProxyList = System.getProperty(NO_PROXY, System.getenv(NO_PROXY));
        if (noProxyList == null) {
            noProxyList = System.getProperty(NO_PROXY.toLowerCase());
        }
        return noProxyList;
    }

    private static String getHttpProxy()
    {
        String httpProxy = System.getProperty(HTTP_PROXY, System.getenv(HTTP_PROXY));
        if (httpProxy == null) {
            httpProxy = System.getProperty(HTTP_PROXY.toLowerCase());
        }
        return httpProxy;
    }

    private static String getHttpsProxy()
    {
        String httpsProxy = System.getProperty(HTTPS_PROXY, System.getenv(HTTPS_PROXY));
        if (httpsProxy == null) {
            httpsProxy = System.getProperty(HTTPS_PROXY.toLowerCase());
        }
        return httpsProxy;
    }
}
