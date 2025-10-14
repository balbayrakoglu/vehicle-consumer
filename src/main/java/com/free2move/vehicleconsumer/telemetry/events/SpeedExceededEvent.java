package com.free2move.vehicleconsumer.telemetry.events;

import com.free2move.vehicleconsumer.telemetry.model.domain.VehicleId;
import java.time.Instant;

public record SpeedExceededEvent(
    VehicleId vehicleId,
    Instant at,
    double speedKmh ) implements TelemetryDomainEvent { }
