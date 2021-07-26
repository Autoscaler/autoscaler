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
package com.github.autoscaler.api;


import com.hpe.caf.api.HealthReporter;


/**
 * A ServiceScaler is a class that actively triggers or performs the up or
 * down scaling of a service on a platform.
 */
public interface ServiceScaler extends HealthReporter
{
    /**
     * Scale up a service.
     * @param service the service to scale up, by reference
     * @param amount the number of instances to scale up by
     * @throws ScalerException if the scaling operation cannot be performed
     */
    void scaleUp(String service, int amount)
        throws ScalerException;


    /**
     * Scale down a service.
     * @param service the service to scale down, by reference
     * @param amount the number of instances to scale down by
     * @throws ScalerException if the scaling operation cannot be performed
     */
    void scaleDown(String service, int amount)
        throws ScalerException;


    /**
     * Get information about the currently running instances of a service.
     * @param service the service to retrieve information on, by reference
     * @return an object containing information about the instances running
     * @throws ScalerException if the information could not be retrieved
     */
    InstanceInfo getInstanceInfo(String service)
        throws ScalerException;
}
