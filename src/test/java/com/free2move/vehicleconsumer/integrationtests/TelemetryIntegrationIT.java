package com.free2move.vehicleconsumer.integrationtests;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.free2move.vehicleconsumer.integrationtests.config.AbstractIntegrationTestBase;
import com.free2move.vehicleconsumer.telemetry.events.DomainEventPublisher;
import com.free2move.vehicleconsumer.telemetry.events.GeofenceTransitionEvent;
import com.free2move.vehicleconsumer.telemetry.events.SpeedExceededEvent;
import com.free2move.vehicleconsumer.telemetry.events.TelemetryDomainEvent;
import com.free2move.vehicleconsumer.telemetry.geo.GeoJsonGeofenceService;
import com.free2move.vehicleconsumer.telemetry.model.domain.GeoPoint;
import java.time.Duration;
import java.util.List;
import org.awaitility.Awaitility;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

class TelemetryIntegrationIT extends AbstractIntegrationTestBase {

  @MockitoBean(name = "geoJsonGeofenceService")
  private GeoJsonGeofenceService geofenceService;

  @MockitoBean
  private DomainEventPublisher domainEventPublisher;

  @BeforeEach
  void setupMocks() {
    when(geofenceService.contains(any(GeoPoint.class)))
            .thenAnswer(invocation -> {
              GeoPoint point = invocation.getArgument(0);
              return point.latitude() > 52.0005;
            });

    doNothing().when(domainEventPublisher).publish(any(TelemetryDomainEvent.class));
  }

  @Test
  void messaging_shouldPublishExceeded_whenSpeedOverThreshold() {
    sendToVehicleExchange("VIN00001", "2025-01-01T00:00:00Z", 52.0000, 13.0000);
    sendToVehicleExchange("VIN00001", "2025-01-01T00:00:05Z", 52.0010, 13.0000);

    ArgumentCaptor<TelemetryDomainEvent> captor =
            ArgumentCaptor.forClass(TelemetryDomainEvent.class);

    Awaitility.await()
            .atMost(Duration.ofSeconds(5))
            .untilAsserted(() -> {
              verify(domainEventPublisher, atLeastOnce()).publish(captor.capture());
              assertThat(captor.getAllValues().stream().anyMatch(SpeedExceededEvent.class::isInstance))
                      .isTrue();
            });

    long exceededCount = captor.getAllValues().stream()
            .filter(SpeedExceededEvent.class::isInstance)
            .count();

    assertThat(exceededCount).isEqualTo(1);
  }

  @Test
  void messaging_shouldDropOutOfOrder_andNotPublishExtraEvents() {
    sendToVehicleExchange("VIN00002", "2025-01-01T00:00:00Z", 52.0000, 13.0000);
    sendToVehicleExchange("VIN00002", "2025-01-01T00:00:05Z", 52.0010, 13.0000);

    ArgumentCaptor<TelemetryDomainEvent> captor =
            ArgumentCaptor.forClass(TelemetryDomainEvent.class);

    Awaitility.await()
            .atMost(Duration.ofSeconds(5))
            .untilAsserted(() -> verify(domainEventPublisher, atLeastOnce()).publish(captor.capture()));

    int before = captor.getAllValues().size();

    sendToVehicleExchange("VIN00002", "2025-01-01T00:00:03Z", 52.0005, 13.0000);

    Awaitility.await()
            .during(Duration.ofMillis(400))
            .atMost(Duration.ofSeconds(3))
            .untilAsserted(() -> assertThat(captor.getAllValues()).hasSize(before));
  }

  @Test
  void messaging_shouldPublishGeofenceEnter_whenInsideBecomesTrue() {
    sendToVehicleExchange("VIN00003", "2025-01-01T00:00:00Z", 52.0000, 13.0000);
    sendToVehicleExchange("VIN00003", "2025-01-01T00:00:03Z", 52.0010, 13.0000);

    ArgumentCaptor<TelemetryDomainEvent> captor =
            ArgumentCaptor.forClass(TelemetryDomainEvent.class);

    Awaitility.await()
            .atMost(Duration.ofSeconds(5))
            .untilAsserted(() -> verify(domainEventPublisher, atLeastOnce()).publish(captor.capture()));

    List<GeofenceTransitionEvent> geofenceEvents = captor.getAllValues().stream()
            .filter(GeofenceTransitionEvent.class::isInstance)
            .map(event -> (GeofenceTransitionEvent) event)
            .toList();

    assertThat(geofenceEvents).isNotEmpty();
    assertThat(geofenceEvents.get(0).type())
            .isEqualTo(GeofenceTransitionEvent.Transition.ENTER);
  }
}