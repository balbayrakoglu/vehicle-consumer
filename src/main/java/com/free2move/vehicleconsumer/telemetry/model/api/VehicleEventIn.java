package com.free2move.vehicleconsumer.telemetry.model.api;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.Instant;

@JsonIgnoreProperties(ignoreUnknown = true)
public record VehicleEventIn(
    @NotBlank
    @JsonAlias({"vin", "vehicle_id"})
    String vin,

    @NotNull
    Instant timestamp,

    @NotNull
    Location location
) {}