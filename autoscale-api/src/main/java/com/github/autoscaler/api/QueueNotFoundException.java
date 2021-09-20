package com.github.autoscaler.api;

/**
 * Thrown by the autoscaler application or components when a Queue is not found.
 */
public class QueueNotFoundException extends ScalerException{

    public QueueNotFoundException(String message) {
        super(message);
    }
}
