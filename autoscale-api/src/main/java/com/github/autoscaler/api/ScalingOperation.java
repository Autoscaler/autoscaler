/*
 * Copyright 2015-2023 Open Text.
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
 * The type of recommendations a WorkloadAnalyser can make.
 */
public enum ScalingOperation
{
    /**
     * No scaling operation needs to be performed.
     */
    NONE,
    /**
     * The algorithm indicates the scaler should scale to more instances of the service.
     */
    SCALE_UP,
    /**
     * The algorithm indicates the scale should scale down the number of instances of the service.
     */
    SCALE_DOWN;
}
