package com.github.autoscaler.workload.rabbit.loadbalancing.rerouting.mutators;

import com.google.common.base.Strings;
import com.hpe.caf.worker.document.model.Document;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Objects;

public abstract class QueueNameMutator {

    private static final Logger LOGGER = LoggerFactory.getLogger(QueueNameMutator.class);    
    
    public abstract void mutateSuccessQueueName(final Document document);

    protected String getCurrentSuccessQueueName(final Document document) {
        return document.getTask().getResponse().getSuccessQueue().getName();
    }
    
    protected void setCurrentSuccessQueueName(final Document document, final String name) {
        Objects.requireNonNull(document);
        
        if(Strings.isNullOrEmpty(name)) {
            LOGGER.error("Cannot change success queue from {} to null or empty string.", 
                    document.getTask().getResponse().getSuccessQueue().getName());
            return;
        }
        
        if(name.length() > 255) {
            LOGGER.error(
                "Cannot change success queue from {} to to {} as it will exceed the maximum queue name length of 255.",
                document.getTask().getResponse().getSuccessQueue().getName(), name);
            return;
        }
        
        document.getTask().getResponse().getSuccessQueue().set(name);        
    }
}
