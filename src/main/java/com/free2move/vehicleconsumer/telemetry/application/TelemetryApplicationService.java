package com.free2move.vehicleconsumer.telemetry.application;

import com.free2move.vehicleconsumer.config.BusinessProps;
import com.free2move.vehicleconsumer.telemetry.events.DomainEventPublishException;
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
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
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
    VehicleTelemetryState state = stateStore.getOrCreate(curr.vehicleId());
    var prevOpt = state.lastSample();

    if (prevOpt.isEmpty()) {
      initState(curr, state);
      meterRegistry.counter("telemetry.state_initialized").increment();
      return;
    }

    TelemetrySample prev = prevOpt.get();

    if (!curr.timestamp().isAfter(prev.timestamp())) {
      log.debug(
              "dropped.out_of_order vin={} currTs={} prevTs={}",
              curr.vehicleId(),
              curr.timestamp(),
              prev.timestamp()
      );
      meterRegistry.counter("telemetry.out_of_order").increment();
      return;
    }

    String vin = curr.vehicleId().value();
    double kmh = speedCalculator.kmhBetween(prev, curr);

    log.info("calc.speed vin={} kmh={}", vin, String.format(Locale.ROOT, "%.1f", kmh));
    meterRegistry.summary("telemetry.speed.kmh").record(kmh);

    boolean wasOver = state.lastOverThreshold();
    boolean isOver = kmh >= businessProps.speedThresholdKmh();

    boolean nowInside = geoJsonGeofenceService.contains(curr.position());
    boolean geofenceChanged = state.lastInside().isPresent() && state.lastInside().get() != nowInside;

    List<TelemetryDomainEvent> pendingEvents = buildEvents(curr, kmh, wasOver, isOver, nowInside, geofenceChanged);

    publishAllOrThrow(curr, pendingEvents);

    state.setLastOverThreshold(isOver);
    state.setLastInside(nowInside);
    state.setLastSample(curr);

    meterRegistry.counter("telemetry.state_updated").increment();

    log.info(
            "telemetry.state.updated vin={} isOver={} nowInside={} publishedEvents={}",
            vin,
            isOver,
            nowInside,
            pendingEvents.size()
    );
  }

  private List<TelemetryDomainEvent> buildEvents(
          TelemetrySample curr,
          double kmh,
          boolean wasOver,
          boolean isOver,
          boolean nowInside,
          boolean geofenceChanged
  ) {
    List<TelemetryDomainEvent> events = new ArrayList<>();

    if (isOver && !wasOver) {
      events.add(new SpeedExceededEvent(curr.vehicleId(), curr.timestamp(), kmh));
    } else if (isOver) {
      events.add(new SpeedOverThresholdUpdateEvent(curr.vehicleId(), curr.timestamp(), kmh));
    }

    if (geofenceChanged) {
      GeofenceTransitionEvent.Transition transition =
              nowInside ? GeofenceTransitionEvent.Transition.ENTER : GeofenceTransitionEvent.Transition.EXIT;

      events.add(new GeofenceTransitionEvent(curr.vehicleId(), curr.timestamp(), transition));
    }

    return events;
  }

  private void publishAllOrThrow(TelemetrySample curr, List<TelemetryDomainEvent> events) {
    for (TelemetryDomainEvent event : events) {
      try {
        eventPublisher.publish(event);
        meterRegistry.counter("telemetry.events_published", "type", event.getClass().getSimpleName()).increment();
      } catch (Exception e) {
        meterRegistry.counter("telemetry.events_publish_failed", "type", event.getClass().getSimpleName()).increment();

        log.error(
                "telemetry.event.publish_failed vin={} eventType={} ts={}",
                curr.vehicleId(),
                event.getClass().getSimpleName(),
                curr.timestamp(),
                e
        );

        throw new DomainEventPublishException(
                "Failed to publish domain event: " + event.getClass().getSimpleName(),
                e
        );
      }
    }
  }

  private void initState(TelemetrySample curr, VehicleTelemetryState state) {
    boolean nowInside = geoJsonGeofenceService.contains(curr.position());
    state.setLastInside(nowInside);
    state.setLastOverThreshold(false);
    state.setLastSample(curr);
  }
}