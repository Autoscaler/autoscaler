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
({
    rabbitManagementEndpoint: getenv("CAF_RABBITMQ_MGMT_URL")
            || ("http://" + (getenv("CAF_RABBITMQ_HOST") || "rabbitmq") + ":" + (getenv("CAF_RABBITMQ_MGMT_PORT") || "15672")),
    rabbitManagementUser: getenv("CAF_RABBITMQ_MGMT_USERNAME") || getenv("CAF_RABBITMQ_USERNAME") || "guest",
    rabbitManagementPassword: getenv("CAF_RABBITMQ_MGMT_PASSWORD") || getenv("CAF_RABBITMQ_PASSWORD") || "guest",
    memoryQueryRequestFrequency: getenv("CAF_AUTOSCALER_RABBITMQ_MEMORY_QUERY_FREQ") || 10,
    profiles: {
        default: {
            scalingDelay: getenv("CAF_AUTOSCALER_SCALING_DELAY") || 10,
            backlogGoal: getenv("CAF_AUTOSCALER_BACKLOG_GOAL") || 300
        }
    }
});
