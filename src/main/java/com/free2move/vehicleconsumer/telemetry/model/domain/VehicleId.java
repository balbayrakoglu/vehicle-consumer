package com.free2move.vehicleconsumer.telemetry.model.domain;

import java.util.Locale;
import java.util.Objects;

public record VehicleId(String value) {

  public VehicleId {
    Objects.requireNonNull(value, "vin");
    String normalized = value.trim();
    if (normalized.isEmpty()) {
      throw new IllegalArgumentException("vin must not be blank");
    }
    value = normalized.toUpperCase(Locale.ROOT);
  }

  @Override
  public String toString() {
    return value;
  }
}
