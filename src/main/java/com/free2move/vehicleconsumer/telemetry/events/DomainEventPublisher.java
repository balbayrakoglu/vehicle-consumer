package com.free2move.vehicleconsumer.telemetry.events;

public interface DomainEventPublisher {
  void publish(TelemetryDomainEvent  event);
}
