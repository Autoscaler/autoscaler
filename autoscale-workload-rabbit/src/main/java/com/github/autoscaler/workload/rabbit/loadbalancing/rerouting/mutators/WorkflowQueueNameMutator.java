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
import com.hpe.caf.worker.document.model.Field;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class WorkflowQueueNameMutator extends QueueNameMutator {

    private static final Logger LOGGER = LoggerFactory.getLogger(WorkflowQueueNameMutator.class);

    @Override
    public void mutateSuccessQueueName(Document document) {
        final String customDataWorkflowName = document.getCustomData("workflowName");
        final Field fieldWorkflowName = document.getField("CAF_WORKFLOW_NAME");

        final String workflowName;
        if(fieldWorkflowName != null && fieldWorkflowName.hasValues()) {
            workflowName = fieldWorkflowName.getStringValues().get(0);
        }
        else {
            workflowName = customDataWorkflowName;
        }
        
        if(Strings.isNullOrEmpty(workflowName)) {
            LOGGER.trace("No workflow name, unable to mutate queueName {}.",
                    document.getTask().getResponse().getSuccessQueue().getName());
            return;
        }

        setCurrentSuccessQueueName(document, getCurrentSuccessQueueName(document) + "/" + workflowName);

    }
}
