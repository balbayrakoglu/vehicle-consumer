package com.free2move.vehicleconsumer.telemetry.state;

import com.free2move.vehicleconsumer.telemetry.model.domain.TelemetrySample;

import java.util.Optional;
import lombok.Setter;

@Setter
public final class VehicleTelemetryState {

  private TelemetrySample lastSample;
  private boolean lastOverThreshold;
  private Boolean lastInside;

  public Optional<TelemetrySample> lastSample() { return Optional.ofNullable(lastSample); }

  public boolean lastOverThreshold() { return lastOverThreshold; }

  public Optional<Boolean> lastInside() { return Optional.ofNullable(lastInside); }
}