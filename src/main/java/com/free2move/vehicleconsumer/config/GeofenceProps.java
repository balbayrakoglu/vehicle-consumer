package com.free2move.vehicleconsumer.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "geofence")
public record GeofenceProps(String geojson) {}
