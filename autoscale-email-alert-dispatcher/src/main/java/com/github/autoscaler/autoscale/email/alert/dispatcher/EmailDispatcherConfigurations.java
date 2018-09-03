/*
 * Copyright 2015-2018 Micro Focus or one of its affiliates.
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
package com.github.autoscaler.autoscale.email.alert.dispatcher;

import javax.validation.constraints.NotNull;

public final class EmailDispatcherConfigurations
{
    /**
     * SMTP Server host address
     */
    @NotNull
    private String host;
    /**
     * SMTP Server Port
     */
    @NotNull
    private String port;
    /**
     * SMTP Server Username
     * defaults to empty string
     */
    @NotNull
    private String username;
    /**
     *  SMTP Server Password
     *  defaults to empty string
     */
    @NotNull
    private String password;
    /**
     *  Email address to send alert messages to.
     */
    @NotNull
    private String emailAddressTo;
    /**
     * Email address to send alert messages from.
     * defaults to apollo-autoscaler@microfocus.com
     */
    @NotNull
    private String emailAddressFrom;

    /**
     * @return the host
     */
    public String getHost()
    {
        return host;
    }

    /**
     * @return the port
     */
    public String getPort()
    {
        return port;
    }

    /**
     * @return the username
     */
    public String getUsername()
    {
        return username;
    }

    /**
     * @return the password
     */
    public String getPassword()
    {
        return password;
    }

    /**
     * @return the emailAddressTo
     */
    public String getEmailAddressTo()
    {
        return emailAddressTo;
    }

    /**
     * @return the emailAddressFrom
     */
    public String getEmailAddressFrom()
    {
        return emailAddressFrom;
    }

}
