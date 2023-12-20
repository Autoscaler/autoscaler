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
package com.github.autoscaler.dockerswarm.shared.endpoint.docker;

import java.util.HashMap;
import java.util.Map;

/**
 *
 * Utility class used to represent a Docker Swarm service or application, regardless of which method was used to obtain the information.
 * This simplifies the autoscale query logic and keeps the query / building JSON Query logic close to the REST method being used.
 */
public class DockerSwarmApp
{
    private String id;

    private Map<String, String> labels = new HashMap<>();

    public String getId()
    {
        return id;
    }

    public void setId(String id)
    {
        this.id = id;
    }

    public Map<String, String> getLabels()
    {
        return labels;
    }

    public void setLabels(Map<String, String> labels)
    {
        this.labels = labels;
    }

}
