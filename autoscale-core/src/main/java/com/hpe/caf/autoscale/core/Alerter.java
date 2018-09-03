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
package com.hpe.caf.autoscale.core;

import com.hpe.caf.api.autoscale.AlertDispatcher;
import com.hpe.caf.api.autoscale.ScalerException;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class Alerter
{
    private long lastTime = 0;
    private final Map<String, AlertDispatcher> dispatchers;
    private final int dispatchFrequency;
    private final boolean alertDispatchDisabled;
    private static final Object MESSAGE_DISPATCH_LOCK = new Object();
    private static final Logger LOG = LoggerFactory.getLogger(Alerter.class);

    public Alerter(final Map<String, AlertDispatcher> dispatchers, final AlertDispatchConfiguration alertConfig)
    {
        this.dispatchers = dispatchers;
        this.dispatchFrequency = alertConfig.getAlertDispatchFrequency();
        this.alertDispatchDisabled = alertConfig.isDisableAlertDispatch();
    }

    /**
     * Determines if an alert should be sent based on how long ago the last alert message was sent out.
     *
     * @param messageBody The content of the alert to dispatch.
     * @throws ScalerException If it is unable to send the alert for any reason.
     */
    public void dispatchAlert(final String messageBody) throws ScalerException
    {
        if (alertDispatchDisabled) {
            return;
        }

        if (lastTime == 0 || (lastTime - System.currentTimeMillis()) == (dispatchFrequency * 60 * 1000)) {
            synchronized (MESSAGE_DISPATCH_LOCK) {
                for (final Map.Entry<String, AlertDispatcher> dispatcherEntry : dispatchers.entrySet()) {
                    final AlertDispatcher dispatcher = dispatcherEntry.getValue();
                    LOG.debug("Dispatching Alert using {}", dispatcher.getClass().getSimpleName());
                    dispatcher.dispatch(messageBody);
                    lastTime = System.currentTimeMillis();
                }
            }
        }
    }
}
