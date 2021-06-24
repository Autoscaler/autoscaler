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
    resourceLimitOne: getenv("CAF_AUTOSCALER_MESSAGING_RESOURCE_LIMIT_STAGE_1") || 70,
    resourceLimitTwo: getenv("CAF_AUTOSCALER_MESSAGING_RESOURCE_LIMIT_STAGE_2") || 80,
    resourceLimitThree: getenv("CAF_AUTOSCALER_MESSAGING_RESOURCE_LIMIT_STAGE_3") || 90,
    resourceLimitOneShutdownThreshold: getenv("CAF_AUTOSCALER_MESSAGING_STAGE_1_SHUTDOWN_THRESHOLD") || 1,
    resourceLimitTwoShutdownThreshold: getenv("CAF_AUTOSCALER_MESSAGING_STAGE_2_SHUTDOWN_THRESHOLD") || 3,
    resourceLimitThreeShutdownThreshold: getenv("CAF_AUTOSCALER_MESSAGING_STAGE_3_SHUTDOWN_THRESHOLD") || 5,
    alertDispatchThreshold: getenv("CAF_AUTOSCALER_ALERT_DISPATCH_THRESHOLD") 
            || getenv("CAF_AUTOSCALER_MESSAGING_RESOURCE_LIMIT_STAGE_1")
            || 70
});
