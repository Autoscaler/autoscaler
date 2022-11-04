package com.github.autoscaler.workload.rabbit.loadbalancing.redistribution;

import java.util.ArrayList;
import java.util.List;

public class MessageTarget {
    private String targetQueue;
//    private List<MessageSource> messageSources = new ArrayList<>();
    private List<String> messageSources = new ArrayList<>();

    public MessageTarget(final String targetQueue) {
        this.targetQueue = targetQueue;
    }
    
    public String getTargetQueue() {
        return targetQueue;
    }

    public List<String> getMessageSources() {
        return messageSources;
    }
}
