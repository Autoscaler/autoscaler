package com.github.autoscaler.workload.rabbit.loadbalancing.redistribution.management;

public class Queue {
    private String name;
    private long messages;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public long getMessages() {
        return messages;
    }

    public void setMessages(long messages) {
        this.messages = messages;
    }
}
