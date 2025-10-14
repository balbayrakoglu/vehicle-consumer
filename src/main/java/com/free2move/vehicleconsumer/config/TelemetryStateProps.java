package com.free2move.vehicleconsumer.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "state.telemetry")
public record TelemetryStateProps(long expireAfterAccessMinutes, long maximumSize) {}