package com.free2move.vehicleconsumer.integrationtests.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.free2move.vehicleconsumer.VehicleConsumerApplication;
import java.util.Map;
import java.util.function.Supplier;
import lombok.Getter;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;

@Getter
@AutoConfigureMockMvc
@SpringBootTest(
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
    properties = {"spring.application.name=telemetryIT"},
    classes = VehicleConsumerApplication.class
)
public abstract class AbstractIntegrationTestBase extends IntegrationTestContainer {

  private static final Supplier<Object> rabbitHost =
      () -> rabbitContainer == null ? "localhost" : rabbitContainer.getHost();

  private static final Supplier<Object> rabbitPort =
      () -> rabbitContainer == null ? 5672 : rabbitContainer.getAmqpPort();

  @Autowired
  private MockMvc mockMvc;
  @Autowired
  private TestRestTemplate restTemplate;
  @Autowired
  private ObjectMapper objectMapper;
  @Autowired
  protected RabbitTemplate rabbitTemplate;

  @DynamicPropertySource
  static void configureProperties(final DynamicPropertyRegistry r) {

    r.add("spring.rabbitmq.host", rabbitHost);
    r.add("spring.rabbitmq.port", rabbitPort);
    r.add("spring.rabbitmq.username", () -> "guest");
    r.add("spring.rabbitmq.password", () -> "guest");
    r.add("spring.rabbitmq.listener.simple.concurrency", () -> "1");
    r.add("spring.rabbitmq.listener.simple.max-concurrency", () -> "1");
    r.add("spring.rabbitmq.listener.simple.prefetch", () -> "10");
    r.add("spring.rabbitmq.listener.simple.missing-queues-fatal", () -> "false");
    r.add("app.rabbit.declare-topology", () -> "true");
    r.add("app.rabbit.exchange", () -> "vehicle.exchange");
    r.add("app.rabbit.queue", () -> "vehicle.queue");
    r.add("app.rabbit.routing-key", () -> "vehicle.*");
    r.add("business.speed-threshold-kmh", () -> "50");
  }

  protected void sendToVehicleExchange(String vin, String isoTs, double lat, double lon) {
    Map<String, Object> payload = Map.of(
        "vin", vin,
        "timestamp", isoTs,
        "location", Map.of("lat", lat, "lon", lon)
    );
    rabbitTemplate.convertAndSend("vehicle.exchange", "vehicle.test", payload);
  }
}