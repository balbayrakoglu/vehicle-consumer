package com.free2move.vehicleconsumer.telemetry.state;

import com.free2move.vehicleconsumer.config.TelemetryStateProps;
import com.free2move.vehicleconsumer.telemetry.model.domain.VehicleId;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.Optional;

@Component
public class CaffeineTelemetryStateStore implements TelemetryStateStore {

  private final Cache<String, VehicleTelemetryState> cache;

  public CaffeineTelemetryStateStore(TelemetryStateProps props) {
    this.cache = Caffeine.newBuilder()
        .expireAfterAccess(Duration.ofMinutes(props.expireAfterAccessMinutes()))
        .maximumSize(props.maximumSize())
        .build();
  }

  @Override
  public Optional<VehicleTelemetryState> get(VehicleId vehicleId) {
    return Optional.ofNullable(cache.getIfPresent(vehicleId.value()));
  }

  @Override
  public VehicleTelemetryState getOrCreate(VehicleId vehicleId) {
    return cache.get(vehicleId.value(), k -> new VehicleTelemetryState());
  }
}