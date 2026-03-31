package com.free2move.vehicleconsumer.integrationtests.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.LinkedHashMap;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.RabbitMQContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@SpringBootTest
@Testcontainers(disabledWithoutDocker = true)
public abstract class AbstractIntegrationTestBase {

  @Container
  static final RabbitMQContainer RABBITMQ =
          new RabbitMQContainer("rabbitmq:3.13-management");

  @DynamicPropertySource
  static void registerRabbitProperties(DynamicPropertyRegistry registry) {
    registry.add("spring.rabbitmq.host", RABBITMQ::getHost);
    registry.add("spring.rabbitmq.port", RABBITMQ::getAmqpPort);
    registry.add("spring.rabbitmq.username", RABBITMQ::getAdminUsername);
    registry.add("spring.rabbitmq.password", RABBITMQ::getAdminPassword);

    registry.add("app.rabbit.declare-topology", () -> true);
    registry.add("app.rabbit.queue", () -> "vehicle_queue");
    registry.add("app.rabbit.exchange", () -> "vehicle.exchange");
    registry.add("app.rabbit.routing-key", () -> "vehicle.*");
  }

  @Autowired
  protected RabbitTemplate rabbitTemplate;

  @Autowired
  protected ObjectMapper objectMapper;

  @BeforeEach
  void beforeEachBase() {
    rabbitTemplate.setExchange("vehicle.exchange");
  }

  protected void sendToVehicleExchange(String vin, String timestamp, double latitude, double longitude) {
    try {
      Map<String, Object> payload = new LinkedHashMap<>();
      payload.put("vin", vin);
      payload.put("timestamp", timestamp);
      payload.put("latitude", latitude);
      payload.put("longitude", longitude);

      String json = objectMapper.writeValueAsString(payload);
      rabbitTemplate.convertAndSend("vehicle.exchange", "vehicle.position", json);
    } catch (Exception e) {
      throw new RuntimeException("Failed to publish test vehicle event", e);
    }
  }
}