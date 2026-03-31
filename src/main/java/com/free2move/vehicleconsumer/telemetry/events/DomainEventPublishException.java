package com.free2move.vehicleconsumer.telemetry.events;

public class DomainEventPublishException extends RuntimeException {

    public DomainEventPublishException(String message, Throwable cause) {
        super(message, cause);
    }
}