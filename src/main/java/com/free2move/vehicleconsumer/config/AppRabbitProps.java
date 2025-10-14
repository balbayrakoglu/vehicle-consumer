package com.free2move.vehicleconsumer.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.rabbit")
public record AppRabbitProps(
    String queue,
    String exchange,
    String routingKey,
    boolean declareTopology
) {}
