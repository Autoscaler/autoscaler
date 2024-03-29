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


import com.hpe.caf.api.HealthReporter;


/**
 * Creates WorkloadAnalyser instances for a specific target and profile.
 */
public interface WorkloadAnalyserFactory extends HealthReporter
{
    /**
     * Instantiate a new WorkloadAnalyser for a specific target and profile.
     * @param scalingTarget the reference to the target used for analysing workloads
     * @param scalingProfile the name of the profile to use for scaling
     * @return a new WorkloadAnalyser instance for the given scaling target and profile
     */
    WorkloadAnalyser getAnalyser(String scalingTarget, String scalingProfile);
}
