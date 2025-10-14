package com.free2move.vehicleconsumer.telemetry.events;

public sealed interface TelemetryDomainEvent
    permits SpeedExceededEvent, SpeedOverThresholdUpdateEvent, GeofenceTransitionEvent { }
