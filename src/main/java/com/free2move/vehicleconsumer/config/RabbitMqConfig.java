package com.free2move.vehicleconsumer.config;


import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.annotation.EnableRabbit;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableRabbit
@EnableConfigurationProperties(AppRabbitProps.class)
public class RabbitMqConfig {

  @Bean
  Jackson2JsonMessageConverter jackson2JsonMessageConverter(ObjectMapper objectMapper) {
    return new Jackson2JsonMessageConverter(objectMapper);
  }

  @Bean
  Declarables rabbitDeclarables(AppRabbitProps p) {
    if (!p.declareTopology()) return new Declarables();
    TopicExchange exchange = ExchangeBuilder.topicExchange(p.exchange()).durable(true).build();
    Queue queue = QueueBuilder.durable(p.queue()).build();
    Binding binding = BindingBuilder.bind(queue).to(exchange).with(p.routingKey());
    return new Declarables(exchange, queue, binding);
  }
}