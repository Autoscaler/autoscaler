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
/*global getenv */
({
    endpoint: getenv("DOCKER_HOST")
            || ("unix:///var/run/docker.sock"),
    maximumInstances: getenv("CAF_AUTOSCALER_MAXIMUM_INSTANCES") || 100,
    stackId: getenv("CAF_AUTOSCALER_DOCKER_SWARM_STACK") || undefined,
    timeoutInSecs: getenv("CAF_DOCKER_SWARM_TIMEOUT") || 30,
    healthCheckTimeoutInSecs: getenv("CAF_DOCKER_SWARM_HEALTHCHECK_TIMEOUT") || 5,

    /** optional https settings **/
    tlsVerify: getenv("DOCKER_TLS_VERIFY") || false,
    certificatePath: getenv("DOCKER_CERT_PATH") || undefined
});
