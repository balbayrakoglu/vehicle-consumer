package com.free2move.vehicleconsumer.telemetry.application;

import com.free2move.vehicleconsumer.config.BusinessProps;
import com.free2move.vehicleconsumer.telemetry.events.DomainEventPublisher;
import com.free2move.vehicleconsumer.telemetry.events.GeofenceTransitionEvent;
import com.free2move.vehicleconsumer.telemetry.events.SpeedExceededEvent;
import com.free2move.vehicleconsumer.telemetry.events.SpeedOverThresholdUpdateEvent;
import com.free2move.vehicleconsumer.telemetry.events.TelemetryDomainEvent;
import com.free2move.vehicleconsumer.telemetry.geo.GeoJsonGeofenceService;
import com.free2move.vehicleconsumer.telemetry.model.domain.TelemetrySample;
import com.free2move.vehicleconsumer.telemetry.state.TelemetryStateStore;
import com.free2move.vehicleconsumer.telemetry.state.VehicleTelemetryState;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@RequiredArgsConstructor
@Service
@Slf4j
public class TelemetryApplicationService {

  private final SpeedCalculator speedCalculator;
  private final BusinessProps businessProps;
  private final TelemetryStateStore stateStore;
  private final DomainEventPublisher eventPublisher;
  private final GeoJsonGeofenceService geoJsonGeofenceService;

  private final MeterRegistry meterRegistry;


  public void process(TelemetrySample curr) {
    var state = stateStore.getOrCreate(curr.vehicleId());
    var prevOpt = state.lastSample();

    if (prevOpt.isEmpty()) {
      initState(curr, state);
      return;
    }

    var prev = prevOpt.get();
    if (!curr.timestamp().isAfter(prev.timestamp())) {
      log.debug("dropped.out_of_order vin={} currTs={} prevTs={}", curr.vehicleId(),
          curr.timestamp(), prev.timestamp());
      meterRegistry.counter("telemetry.out_of_order").increment();
      return;
    }

    var v = curr.vehicleId().value();
    double kmh = speedCalculator.kmhBetween(prev, curr);
    log.info("calc.speed vin={} kmh={}", v, String.format(java.util.Locale.ROOT, "%.1f", kmh));
    meterRegistry.summary("telemetry.speed.kmh").record(kmh);

    boolean wasOver = state.lastOverThreshold();
    boolean isOver = kmh >= businessProps.speedThresholdKmh();

    if (isOver && !wasOver) {
      publishSafely(new SpeedExceededEvent(curr.vehicleId(), curr.timestamp(), kmh));
    } else if (isOver) {
      publishSafely(new SpeedOverThresholdUpdateEvent(curr.vehicleId(), curr.timestamp(), kmh));
    }

    boolean nowInside = geoJsonGeofenceService.contains(curr.position());
    var prevInside = state.lastInside();
    if (prevInside.isPresent() && Boolean.TRUE.equals(prevInside.get() != nowInside)) {
      var t = nowInside ? GeofenceTransitionEvent.Transition.ENTER
          : GeofenceTransitionEvent.Transition.EXIT;
      eventPublisher.publish(new GeofenceTransitionEvent(curr.vehicleId(), curr.timestamp(), t));
    }

    state.setLastOverThreshold(isOver);
    state.setLastInside(nowInside);
    state.setLastSample(curr);
  }

  private void initState(TelemetrySample curr, VehicleTelemetryState state) {
    boolean nowInside = geoJsonGeofenceService.contains(curr.position());
    state.setLastInside(nowInside);
    state.setLastOverThreshold(false);
    state.setLastSample(curr);
  }

  private void publishSafely(TelemetryDomainEvent event) {
    try {
      eventPublisher.publish(event);
      meterRegistry.counter("telemetry.events_published").increment();
    } catch (Exception e) {
      log.error("Failed to publish domain event {}", event.getClass().getSimpleName(), e);
    }
  }
}