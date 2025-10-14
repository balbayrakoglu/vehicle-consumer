package com.free2move.vehicleconsumer.telemetry.model.domain;

import java.time.Instant;
import java.util.Objects;

public record TelemetrySample(
    VehicleId vehicleId,
    Instant timestamp,
    GeoPoint position
) {
  public TelemetrySample {
    Objects.requireNonNull(vehicleId, "vehicleId");
    Objects.requireNonNull(timestamp, "timestamp");
    Objects.requireNonNull(position, "position");
  }
}
