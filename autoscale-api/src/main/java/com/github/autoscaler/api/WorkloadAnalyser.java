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
package com.github.autoscaler.api;


/**
 * A WorkloadAnalyser examines the workload of a service and makes
 * recommendations upon how to scale it at a given time.
 */
public interface WorkloadAnalyser
{
    /**
     * Analyse the workload of a service given information on its
     * current instances, and make a recommendation on how to scale it.
     * @param instanceInfo information on the currently running instances of a service
     * @return a recommendation on how to scale this service (if at all)
     * @throws ScalerException if the workload analysis fails
     */
    ScalingAction analyseWorkload(InstanceInfo instanceInfo)
        throws ScalerException;

    /**
     * This method will determine and return the current resource utilisation of RabbitMQ
     *
     * @return the current resource utilisation
     * @throws ScalerException if it fails to determine resource utilisation due to not being able to connect to messaging
     * platform's api.
     */
    ResourceUtilisation getCurrentResourceUtilisation() throws ScalerException;

    /**
     * This method will return the content to send in an email when reporting a memory overload issue with the messaging platform.
     * @param percentageMem The percentage of the messaging platform's allowed memory that has been used. This is passed as a string so
     * that it can be added to the email body.
     * @return The email body
     */
    String getMemoryOverloadWarning(String percentageMem);

    /**
     * This method will return the content to send in an email when reporting a disk space low issue with the messaging platform.
     * @param diskFreeMb The amount of disk space (MB) that is remaining on the messaging platform. This is passed as a string so that
     * it can be added to the email body.
     * @return The email body
     */
    String getDiskSpaceLowWarning(String diskFreeMb);
}
