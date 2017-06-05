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
({
    endpoint: getenv("CAF_DOCKER_SWARM_ENDPOINT")
            || ("http://" + (getenv("CAF_DOCKER_SWARM_HOST") || "localhost") + ":" + (getenv("CAF_DOCKER_SWARM_PORT") || "2375")),
    maximumInstances: getenv("CAF_AUTOSCALER_MAXIMUM_INSTANCES") || 100,
    groupId: getenv("CAF_AUTOSCALER_DOCKER_SWARM_STACK") || undefined,
    timeoutInSecs: getenv("CAF_DOCKER_SWARM_ENDPOINT_TIMEOUT_SECS") || 5,
    healthCheckTimeoutInSecs: getenv("CAF_DOCKER_SWARM_HEALTHCHECK_TIMEOUT_SECS") || 5,
    
    /* optional debugging on endpoint communication */
    proxyEndpoint: getenv("CAF_DOCKER_SWARM_ENDPOINT_PROXY"),
    
    /** optional https settings **/
    tlsVerify: getenv("CAF_DOCKER_SWARM_TLS_VERIFY") || false,
    certificatePath: getenv("CAF_DOCKER_SWARM_TLS_CERT_PATH") || undefined
});