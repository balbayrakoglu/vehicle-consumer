package com.free2move.vehicleconsumer.integrationtests;

import com.free2move.vehicleconsumer.integrationtests.config.AbstractIntegrationTestBase;
import com.free2move.vehicleconsumer.telemetry.events.DomainEventPublisher;
import com.free2move.vehicleconsumer.telemetry.events.GeofenceTransitionEvent;
import com.free2move.vehicleconsumer.telemetry.events.SpeedExceededEvent;
import com.free2move.vehicleconsumer.telemetry.events.TelemetryDomainEvent;
import com.free2move.vehicleconsumer.telemetry.geo.GeoJsonGeofenceService;
import com.free2move.vehicleconsumer.telemetry.model.domain.GeoPoint;
import org.awaitility.Awaitility;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.time.Duration;
import java.util.List;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class TelemetryIntegrationIT extends AbstractIntegrationTestBase {

  @MockitoBean(name = "geoJsonGeofenceService")
  private GeoJsonGeofenceService geofenceService;

  @MockitoBean
  private DomainEventPublisher domainEventPublisher;

  @BeforeEach
  void setupMocks() {
    when(geofenceService.contains(any(GeoPoint.class)))
        .thenAnswer(inv -> ((GeoPoint) inv.getArgument(0)).latitude() > 52.0005);

    doNothing().when(domainEventPublisher).publish(any(TelemetryDomainEvent.class));
  }

  @Test
  void messaging_shouldPublishExceeded_whenSpeedOverThreshold() {
    sendToVehicleExchange("VIN00001", "2025-01-01T00:00:00Z", 52.0000, 13.0000);
    sendToVehicleExchange("VIN00001", "2025-01-01T00:00:05Z", 52.0010, 13.0000);

    ArgumentCaptor<TelemetryDomainEvent> cap = ArgumentCaptor.forClass(TelemetryDomainEvent.class);
    Awaitility.await().atMost(Duration.ofSeconds(5)).untilAsserted(() -> {
      verify(domainEventPublisher, atLeastOnce()).publish(cap.capture());
      assertThat(cap.getAllValues().stream().anyMatch(SpeedExceededEvent.class::isInstance)).isTrue();
    });

    long exceededCount = cap.getAllValues().stream()
        .filter(SpeedExceededEvent.class::isInstance).count();
    assertThat(exceededCount).isEqualTo(1);
  }

  @Test
  void messaging_shouldDropOutOfOrder_andNotPublishExtraEvents() {
    sendToVehicleExchange("VIN00002", "2025-01-01T00:00:00Z", 52.0000, 13.0000);
    sendToVehicleExchange("VIN00002", "2025-01-01T00:00:05Z", 52.0010, 13.0000);

    ArgumentCaptor<TelemetryDomainEvent> cap = ArgumentCaptor.forClass(TelemetryDomainEvent.class);
    Awaitility.await().atMost(Duration.ofSeconds(5))
        .untilAsserted(() -> verify(domainEventPublisher, atLeastOnce()).publish(cap.capture()));

    int before = cap.getAllValues().size();

    sendToVehicleExchange("VIN00002", "2025-01-01T00:00:03Z", 52.0005, 13.0000);

    Awaitility.await().during(Duration.ofMillis(400)).atMost(Duration.ofSeconds(3))
        .untilAsserted(() -> assertThat(cap.getAllValues()).hasSize(before));
  }

  @Test
  void messaging_shouldPublishGeofenceEnter_whenInsideBecomesTrue() {
    sendToVehicleExchange("VIN00003", "2025-01-01T00:00:00Z", 52.0000, 13.0000);
    sendToVehicleExchange("VIN00003", "2025-01-01T00:00:03Z", 52.0010, 13.0000);

    ArgumentCaptor<TelemetryDomainEvent> cap = ArgumentCaptor.forClass(TelemetryDomainEvent.class);
    Awaitility.await().atMost(Duration.ofSeconds(5))
        .untilAsserted(() -> verify(domainEventPublisher, atLeastOnce()).publish(cap.capture()));

    List< GeofenceTransitionEvent > ge = cap.getAllValues().stream()
        .filter(GeofenceTransitionEvent.class::isInstance)
        .map(e -> (GeofenceTransitionEvent) e)
        .toList();

    assertThat(ge).isNotEmpty();
    assertThat(ge.get(0).type()).isEqualTo(GeofenceTransitionEvent.Transition.ENTER);
  }
}