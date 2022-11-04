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
