package com.free2move.vehicleconsumer.telemetry.model.api;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.validation.constraints.NotNull;

@JsonIgnoreProperties(ignoreUnknown = true)
public record Location(
    @JsonAlias({"lat", "latitude"})
    @NotNull
    Double latitude,

    @JsonAlias({"lon", "lng", "longitude"})
    @NotNull
    Double longitude
) {

}
