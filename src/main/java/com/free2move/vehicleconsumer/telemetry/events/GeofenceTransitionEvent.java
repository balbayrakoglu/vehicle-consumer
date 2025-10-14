package com.free2move.vehicleconsumer.telemetry.events;

import com.free2move.vehicleconsumer.telemetry.model.domain.VehicleId;
import java.time.Instant;

public record GeofenceTransitionEvent (
    VehicleId vehicleId,
    Instant at,
    Transition type
) implements TelemetryDomainEvent {
  public enum Transition { ENTER, EXIT }
}
