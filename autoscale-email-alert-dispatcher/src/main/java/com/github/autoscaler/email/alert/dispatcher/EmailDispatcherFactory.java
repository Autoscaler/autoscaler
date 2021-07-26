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
package com.github.autoscaler.email.alert.dispatcher;

import com.github.autoscaler.api.AlertDispatcher;
import com.github.autoscaler.api.AlertDispatcherFactory;
import com.github.autoscaler.api.ScalerException;
import com.hpe.caf.api.ConfigurationException;
import com.hpe.caf.api.ConfigurationSource;

public class EmailDispatcherFactory implements AlertDispatcherFactory
{
    @Override
    public String getAlertDispatcherName()
    {
        return EmailDispatcherConstants.CONFIGURATION_TYPE;
    }

    @Override
    public AlertDispatcher getAlertDispatcher(final ConfigurationSource configs) throws ScalerException
    {
        try {
            return new EmailDispatcher(configs.getConfiguration(EmailDispatcherConfiguration.class));
        } catch (final ConfigurationException ex) {
            throw new ScalerException("Unable to create email dispatcher, cannot find email dispatcher configuration.", ex);
        }
    }
}
