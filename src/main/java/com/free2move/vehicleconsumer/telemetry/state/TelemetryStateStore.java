package com.free2move.vehicleconsumer.telemetry.state;

import com.free2move.vehicleconsumer.telemetry.model.domain.VehicleId;
import java.util.Optional;

public interface TelemetryStateStore {

  Optional<VehicleTelemetryState> get(VehicleId vehicleId);

  VehicleTelemetryState getOrCreate(VehicleId vehicleId);
}