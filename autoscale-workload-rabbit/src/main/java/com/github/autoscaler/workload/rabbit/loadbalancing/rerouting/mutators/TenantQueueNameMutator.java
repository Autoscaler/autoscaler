/*
 * Copyright 2015-2022 Micro Focus or one of its affiliates.
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
package com.github.autoscaler.workload.rabbit.loadbalancing.rerouting.mutators;

import com.google.common.base.Strings;
import com.hpe.caf.worker.document.model.Document;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TenantQueueNameMutator extends QueueNameMutator {
    
    private static final Logger LOGGER = LoggerFactory.getLogger(TenantQueueNameMutator.class);

    @Override
    public void mutateSuccessQueueName(final Document document) {

        final String tenantId = document.getCustomData("tenantId");
        
        if(Strings.isNullOrEmpty(tenantId)) {
            LOGGER.trace("No tenantId, unable to mutate queueName {}.",
                    document.getTask().getResponse().getSuccessQueue().getName());
            return;
        }

        setCurrentSuccessQueueName(document, getCurrentSuccessQueueName(document) + "/" + tenantId);

    }
}
