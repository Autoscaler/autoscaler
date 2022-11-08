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
