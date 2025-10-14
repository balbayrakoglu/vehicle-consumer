package com.free2move.vehicleconsumer.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties({BusinessProps.class, TelemetryStateProps.class, GeofenceProps.class})
public class DomainConfig {}