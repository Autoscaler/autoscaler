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

import com.hpe.caf.api.Configuration;
import jakarta.validation.constraints.NotNull;

@Configuration
public final class AlertDispatchConfiguration
{
    /**
     * A boolean that represent if the alert dispatching functionality should be used or not.
     * Defaults to false
     */
    @NotNull
    private boolean disableAlertDispatch;

    /**
     * How long the dispatcher should wait before dispatching another alert.
     * Defaults to 20 minutes.
     */
    @NotNull
    private int alertDispatchFrequency;

    /**
     * @return the disableAlertDispatch
     */
    public boolean isDisableAlertDispatch()
    {
        return disableAlertDispatch;
    }

    /**
     * @return the alertDispatchFrequency
     */
    public int getAlertDispatchFrequency()
    {
        return alertDispatchFrequency;
    }

    @Override
    public String toString()
    {
        return "AlertDispatchConfiguration{" +
                "disableAlertDispatch=" + disableAlertDispatch +
                ", alertDispatchFrequency=" + alertDispatchFrequency +
                '}';
    }
}
