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

import javax.validation.constraints.NotNull;

class AlertDispatchConfiguration
{
    /**
     * A boolean that represent if the alert dispatching functionality should be used or not.
     * Defaults to false
     */
    @NotNull
    private boolean disableAlertDispatch;
    /**
     * What stage of resource limit an alert should be sent.
     * Defaults to 0.
     */
    @NotNull
    private int alertDispatchStage;
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
     * @return the alertDispatchStage
     */
    public int getAlertDispatchStage()
    {
        return alertDispatchStage;
    }

    /**
     * @return the alertDispatchFrequency
     */
    public int getAlertDispatchFrequency()
    {
        return alertDispatchFrequency;
    }
}
