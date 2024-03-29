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
package com.github.autoscaler.core;

import com.github.autoscaler.api.AlertDispatcher;
import com.github.autoscaler.api.ScalerException;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class Alerter
{
    private volatile long lastTime;
    private final Map<String, AlertDispatcher> dispatchers;
    private final int dispatchFrequency;
    private final boolean alertDispatchDisabled;
    private final Object messageDispatchLock;
    private static final Logger LOG = LoggerFactory.getLogger(Alerter.class);

    public Alerter(final Map<String, AlertDispatcher> dispatchers, final AlertDispatchConfiguration alertConfig)
    {
        this.lastTime = 0;
        this.dispatchers = dispatchers;
        this.dispatchFrequency = alertConfig.getAlertDispatchFrequency();
        this.alertDispatchDisabled = alertConfig.isDisableAlertDispatch();
        this.messageDispatchLock = new Object();
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
        LOG.debug("Attempting to dispatch alert containing message body: {}....", messageBody);
        if (dispatchApproved()) {
            synchronized (messageDispatchLock) {
                if (dispatchApproved()) {
                    for (final Map.Entry<String, AlertDispatcher> dispatcherEntry : dispatchers.entrySet()) {
                        final AlertDispatcher dispatcher = dispatcherEntry.getValue();
                        LOG.debug("Dispatching Alert using {}", dispatcher.getClass().getSimpleName());
                        dispatcher.dispatch(messageBody);
                        LOG.debug("Alert dispatched....");
                        lastTime = System.currentTimeMillis();
                    }
                }
            }
        }
    }
    
    private boolean dispatchApproved()
    {
        if (lastTime == 0) {
            LOG.debug("lastTime still zero. Returning true to send first alert.");
            return true;
        }
        return (System.currentTimeMillis() - lastTime) >= (dispatchFrequency * 60 * 1000);
    }
}
