/*
 * Copyright 2015-2017 Hewlett Packard Enterprise Development LP.
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
package com.hpe.caf.autoscale.endpoint.docker;

/**
 * DockerSwarm filters class, groups together definitions for how and by what an object can be filtered.
 */
public class DockerSwarmFilters
{

    public class ServiceFilterByType
    {
        public final static String LABEL = "label";
        public final static String NAME = "name";
        public final static String ID = "id";
    }

    public class ServiceFilterKeys
    {
        public final static String DOCKER_STACK = "com.docker.stack.namespace";
    }
}
