package com.free2move.vehicleconsumer.telemetry.events;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class LoggingDomainEventPublisher implements DomainEventPublisher {

  @Override
  public void publish(TelemetryDomainEvent  event) {
    log.info("domain-event type={} payload={}", event.getClass().getSimpleName(), event);
  }

}
