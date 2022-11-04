package com.github.autoscaler.workload.rabbit.loadbalancing.redistribution;

import java.util.List;

public abstract class MessageSource {
    private List<MessageSource> messageSources;

    public List<MessageSource> getMessageSources() {
        return messageSources;
    }
}
