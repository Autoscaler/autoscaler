/*
 * Copyright 2015-2020 Micro Focus or one of its affiliates.
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
    endpoint: getenv("CAF_MARATHON_URL")
            || ("http://" + (getenv("CAF_MARATHON_HOST") || "marathon") + ":" + (getenv("CAF_MARATHON_PORT") || "8080")),
    maximumInstances: getenv("CAF_AUTOSCALER_MAXIMUM_INSTANCES") || 100,
    groupId: getenv("CAF_AUTOSCALER_MARATHON_GROUP") || undefined
});
