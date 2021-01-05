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
package com.hpe.caf.autoscale.core;

import com.hpe.caf.api.autoscale.InstanceInfo;
import com.hpe.caf.api.autoscale.ScalingAction;
import com.hpe.caf.api.autoscale.ScalingConfiguration;

/**
 * The Governor serves to prevent services from monopolizing the resources of a particular environment.
 * Prior to the introduction of the Governor it was possible for one service to prevent others from reaching their minimum instance
 * requirements by consuming resource for instances over and above the minimum
 *
 * As services are scheduled for scaling their scaling configuration is registered with the governor.
 * When information regarding the number of running instances is obtained it is recorded with with the governor.
 * A ScalingThread will determine the ScalingAction and then ask the Governor to review this scaling request.
 * 
 * If the ScalingTread is unable to scale up another instance of its service that was approved by the governor due to lack of resources
 * the scaling thread can request that the governor make room for the new service by requesting that another service reduces its 
 * instances.
 */
interface Governor {
    /**
     *
     * @param serviceRef the named reference to the service the instances refers to
     * @param instanceInfo the current instanceInfo for for the service
     */
    void recordInstances(String serviceRef, InstanceInfo instanceInfo);

    /**
     *
     * @param serviceRef the named reference to the service for which the scaling action will be governed
     * @param action the scaling action to review
     * @param currentMemoryLimitStage the current memory limit stage that the message broker is currently at
     * @return a governed ScalingAction that respects the minimum requirements of all services
     */
    ScalingAction govern(String serviceRef, ScalingAction action, int currentMemoryLimitStage);

    /**
     *
     * When called the Governor will attempt to reduce the resources being consumed by other applications to free them up for the 
     * increased number of the supplied service. The Governor will determine which applications have the lowest relative difference 
     * between the number of instances the service would like to have based on workload and the current number of running instances. 
     * The service with the lowest relative difference will be scaled down to make room for the new service to start.
     * 
     * @param serviceRef the named reference to the service
     * @return True or False based on if the governor was able to make room for the service
     */
    boolean freeUpResourcesForService(String serviceRef);

    /**
     *
     * @param scalingConfiguration record the scalingConfiguration for a service
     */
    void register(ScalingConfiguration scalingConfiguration);

    /**
     * 
     * @param serviceRef the name of the service to register
     * @param thread the scaler thread to register as a lister
     */
    void registerListener(String serviceRef, ScalerThread thread);

    /**
     *
     * @param serviceRef the service name to remove and stop scaling operations for
     */
    void remove(String serviceRef);
}
