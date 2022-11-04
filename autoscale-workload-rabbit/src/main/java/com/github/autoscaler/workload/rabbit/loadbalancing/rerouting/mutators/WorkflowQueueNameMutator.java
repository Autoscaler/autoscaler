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
        if(fieldWorkflowName.hasValues()) {
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
