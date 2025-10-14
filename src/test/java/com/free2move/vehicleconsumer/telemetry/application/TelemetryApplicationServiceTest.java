package com.free2move.vehicleconsumer.telemetry.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.isA;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.free2move.vehicleconsumer.config.BusinessProps;
import com.free2move.vehicleconsumer.telemetry.events.DomainEventPublisher;
import com.free2move.vehicleconsumer.telemetry.events.GeofenceTransitionEvent;
import com.free2move.vehicleconsumer.telemetry.events.SpeedExceededEvent;
import com.free2move.vehicleconsumer.telemetry.events.SpeedOverThresholdUpdateEvent;
import com.free2move.vehicleconsumer.telemetry.events.TelemetryDomainEvent;
import com.free2move.vehicleconsumer.telemetry.geo.GeoJsonGeofenceService;
import com.free2move.vehicleconsumer.telemetry.model.domain.GeoPoint;
import com.free2move.vehicleconsumer.telemetry.model.domain.TelemetrySample;
import com.free2move.vehicleconsumer.telemetry.model.domain.VehicleId;
import com.free2move.vehicleconsumer.telemetry.state.TelemetryStateStore;
import com.free2move.vehicleconsumer.telemetry.state.VehicleTelemetryState;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

class TelemetryApplicationServiceTest {

  @Mock
  SpeedCalculator speedCalculator;
  @Mock
  BusinessProps businessProps;
  @Mock
  TelemetryStateStore stateStore;
  @Mock
  DomainEventPublisher eventPublisher;
  @Mock
  GeoJsonGeofenceService geoJsonGeofenceService;
  @InjectMocks
  TelemetryApplicationService telemetryApplicationService;

  @BeforeEach
  void setUp() {
    MockitoAnnotations.openMocks(this);
  }

  @Test
  void process_crossingThreshold_publishesExceeded_once() {

    VehicleId vid = new VehicleId("VIN00001");
    GeoPoint prevPt = new GeoPoint(48.775556, 9.182932);
    GeoPoint currPt = new GeoPoint(48.775846, 9.182932);
    Instant t0 = Instant.parse("2025-01-01T00:00:00Z");

    var state = new VehicleTelemetryState();
    state.setLastSample(new TelemetrySample(vid, t0, prevPt));
    state.setLastInside(true);
    state.setLastOverThreshold(false);

    var curr = new TelemetrySample(vid, t0.plusSeconds(10), currPt);

    when(stateStore.getOrCreate(vid)).thenReturn(state);
    when(speedCalculator.kmhBetween(state.lastSample().get(), curr)).thenReturn(10.0);
    when(businessProps.speedThresholdKmh()).thenReturn(5.0);
    when(geoJsonGeofenceService.contains(currPt)).thenReturn(true);

    telemetryApplicationService.process(curr);

    ArgumentCaptor<TelemetryDomainEvent> cap = ArgumentCaptor.forClass(TelemetryDomainEvent.class);
    verify(eventPublisher, times(1)).publish(cap.capture());

    TelemetryDomainEvent event = cap.getValue();
    assertThat(event).isInstanceOf(SpeedExceededEvent.class);
    SpeedExceededEvent e = (SpeedExceededEvent) event;
    assertThat(e.vehicleId()).isEqualTo(vid);
    assertThat(e.at()).isEqualTo(curr.timestamp());
    assertThat(e.speedKmh()).isEqualTo(10.0);

    assertThat(state.lastOverThreshold()).isTrue();
    assertThat(state.lastInside()).contains(true);
    assertThat(state.lastSample()).contains(curr);
  }


  @Test
  void process_shouldSetStateAndPublishNoEvents_onInitialSample() {
    var vid = new VehicleId("VIN00001");
    var pt = new GeoPoint(52.0, 13.0);
    var t0 = Instant.parse("2025-01-01T00:00:00Z");
    var state = new VehicleTelemetryState();
    when(stateStore.getOrCreate(vid)).thenReturn(state);
    when(geoJsonGeofenceService.contains(pt)).thenReturn(true);

    var curr = new TelemetrySample(vid, t0, pt);
    telemetryApplicationService.process(curr);

    verifyNoInteractions(speedCalculator, eventPublisher);
    assertTrue(state.lastSample().isPresent());
    assertEquals(curr, state.lastSample().get());
    assertTrue(state.lastInside().isPresent());
    assertTrue(state.lastInside().get());
    assertFalse(state.lastOverThreshold());
  }

  @Test
  void process_shouldDropMessageAndNotChangeState_whenTimestampNotIncreasing() {
    var vid = new VehicleId("VIN00001");
    var prevPt = new GeoPoint(52.0, 13.0);
    var currPt = new GeoPoint(52.001, 13.0);
    var t0 = Instant.parse("2025-01-01T00:00:00Z");
    var prev = new TelemetrySample(vid, t0, prevPt);
    var state = new VehicleTelemetryState();
    state.setLastSample(prev);
    state.setLastInside(true);
    state.setLastOverThreshold(false);
    when(stateStore.getOrCreate(vid)).thenReturn(state);

    var curr = new TelemetrySample(vid, t0, currPt);
    telemetryApplicationService.process(curr);

    verifyNoInteractions(speedCalculator, eventPublisher, geoJsonGeofenceService);
    assertEquals(prev, state.lastSample().get());
    assertTrue(state.lastInside().get());
    assertFalse(state.lastOverThreshold());
  }

  @Test
  void process_shouldPublishExceededAndUpdateState_whenCrossingThreshold() {
    var vid = new VehicleId("VIN00001");
    var prevPt = new GeoPoint(48.775556, 9.182932);
    var currPt = new GeoPoint(48.775846, 9.182932);
    var t0 = Instant.parse("2025-01-01T00:00:00Z");
    var prev = new TelemetrySample(vid, t0, prevPt);
    var curr = new TelemetrySample(vid, t0.plusSeconds(10), currPt);
    var state = new VehicleTelemetryState();
    state.setLastSample(prev);
    state.setLastInside(true);
    state.setLastOverThreshold(false);
    when(stateStore.getOrCreate(vid)).thenReturn(state);
    when(speedCalculator.kmhBetween(prev, curr)).thenReturn(10.0);
    when(businessProps.speedThresholdKmh()).thenReturn(5.0);
    when(geoJsonGeofenceService.contains(currPt)).thenReturn(true);

    telemetryApplicationService.process(curr);

    var cap = ArgumentCaptor.forClass(TelemetryDomainEvent.class);
    verify(eventPublisher, times(1)).publish(cap.capture());
    assertInstanceOf(SpeedExceededEvent.class, cap.getValue());
    assertTrue(state.lastOverThreshold());
    assertTrue(state.lastInside().get());
    assertEquals(curr, state.lastSample().get());
  }

  @Test
  void process_shouldPublishUpdateOnly_whenAlreadyOverThreshold() {
    var vid = new VehicleId("VIN00001");
    var prevPt = new GeoPoint(52.0, 13.0);
    var currPt = new GeoPoint(52.001, 13.0);
    var t0 = Instant.parse("2025-01-01T00:00:00Z");
    var prev = new TelemetrySample(vid, t0, prevPt);
    var curr = new TelemetrySample(vid, t0.plusSeconds(5), currPt);
    var state = new VehicleTelemetryState();
    state.setLastSample(prev);
    state.setLastInside(false);
    state.setLastOverThreshold(true);
    when(stateStore.getOrCreate(vid)).thenReturn(state);
    when(speedCalculator.kmhBetween(prev, curr)).thenReturn(80.0);
    when(businessProps.speedThresholdKmh()).thenReturn(50.0);
    when(geoJsonGeofenceService.contains(currPt)).thenReturn(false);

    telemetryApplicationService.process(curr);

    var cap = ArgumentCaptor.forClass(TelemetryDomainEvent.class);
    verify(eventPublisher, times(1)).publish(cap.capture());
    assertInstanceOf(SpeedOverThresholdUpdateEvent.class, cap.getValue());
    assertTrue(state.lastOverThreshold());
    assertFalse(state.lastInside().get());
    assertEquals(curr, state.lastSample().get());
  }

  @Test
  void process_shouldPublishExceeded_whenSpeedEqualsThresholdAndWasNotOver() {
    var vid = new VehicleId("VIN00001");
    var prevPt = new GeoPoint(52.0, 13.0);
    var currPt = new GeoPoint(52.0005, 13.0);
    var t0 = Instant.parse("2025-01-01T00:00:00Z");
    var prev = new TelemetrySample(vid, t0, prevPt);
    var curr = new TelemetrySample(vid, t0.plusSeconds(2), currPt);
    var state = new VehicleTelemetryState();
    state.setLastSample(prev);
    state.setLastInside(true);
    state.setLastOverThreshold(false);
    when(stateStore.getOrCreate(vid)).thenReturn(state);
    when(speedCalculator.kmhBetween(prev, curr)).thenReturn(50.0);
    when(businessProps.speedThresholdKmh()).thenReturn(50.0);
    when(geoJsonGeofenceService.contains(currPt)).thenReturn(true);

    telemetryApplicationService.process(curr);

    var cap = ArgumentCaptor.forClass(TelemetryDomainEvent.class);
    verify(eventPublisher, times(1)).publish(cap.capture());
    assertInstanceOf(SpeedExceededEvent.class, cap.getValue());
    assertTrue(state.lastOverThreshold());
  }

  @Test
  void process_shouldResetOverFlagAndPublishNoSpeedEvents_whenDroppingBelowThreshold() {
    var vid = new VehicleId("VIN00001");
    var prevPt = new GeoPoint(52.0, 13.0);
    var currPt = new GeoPoint(52.0001, 13.0);
    var t0 = Instant.parse("2025-01-01T00:00:00Z");
    var prev = new TelemetrySample(vid, t0, prevPt);
    var curr = new TelemetrySample(vid, t0.plusSeconds(10), currPt);
    var state = new VehicleTelemetryState();
    state.setLastSample(prev);
    state.setLastInside(false);
    state.setLastOverThreshold(true);
    when(stateStore.getOrCreate(vid)).thenReturn(state);
    when(speedCalculator.kmhBetween(prev, curr)).thenReturn(20.0);
    when(businessProps.speedThresholdKmh()).thenReturn(50.0);
    when(geoJsonGeofenceService.contains(currPt)).thenReturn(false);

    telemetryApplicationService.process(curr);

    verifyNoInteractions(eventPublisher);
    assertFalse(state.lastOverThreshold());
    assertFalse(state.lastInside().get());
    assertEquals(curr, state.lastSample().get());
  }

  @Test
  void process_shouldPublishGeofenceEnter_whenInsideBecomesTrue() {
    var vid = new VehicleId("VIN00001");
    var prevPt = new GeoPoint(52.0, 13.0);
    var currPt = new GeoPoint(52.0001, 13.0);
    var t0 = Instant.parse("2025-01-01T00:00:00Z");
    var prev = new TelemetrySample(vid, t0, prevPt);
    var curr = new TelemetrySample(vid, t0.plusSeconds(5), currPt);
    var state = new VehicleTelemetryState();
    state.setLastSample(prev);
    state.setLastInside(false);
    state.setLastOverThreshold(false);
    when(stateStore.getOrCreate(vid)).thenReturn(state);
    when(speedCalculator.kmhBetween(prev, curr)).thenReturn(10.0);
    when(businessProps.speedThresholdKmh()).thenReturn(50.0);
    when(geoJsonGeofenceService.contains(currPt)).thenReturn(true);

    telemetryApplicationService.process(curr);

    var cap = ArgumentCaptor.forClass(TelemetryDomainEvent.class);
    verify(eventPublisher, times(1)).publish(cap.capture());
    assertInstanceOf(GeofenceTransitionEvent.class, cap.getValue());
    var e = (GeofenceTransitionEvent) cap.getValue();
    assertEquals(GeofenceTransitionEvent.Transition.ENTER, e.type());
    assertTrue(state.lastInside().get());
  }

  @Test
  void process_shouldPublishGeofenceExit_whenInsideBecomesFalse() {
    var vid = new VehicleId("VIN00001");
    var prevPt = new GeoPoint(52.0001, 13.0);
    var currPt = new GeoPoint(52.0, 13.0);
    var t0 = Instant.parse("2025-01-01T00:00:00Z");
    var prev = new TelemetrySample(vid, t0, prevPt);
    var curr = new TelemetrySample(vid, t0.plusSeconds(5), currPt);
    var state = new VehicleTelemetryState();
    state.setLastSample(prev);
    state.setLastInside(true);
    state.setLastOverThreshold(false);
    when(stateStore.getOrCreate(vid)).thenReturn(state);
    when(speedCalculator.kmhBetween(prev, curr)).thenReturn(10.0);
    when(businessProps.speedThresholdKmh()).thenReturn(50.0);
    when(geoJsonGeofenceService.contains(currPt)).thenReturn(false);

    telemetryApplicationService.process(curr);

    var cap = ArgumentCaptor.forClass(TelemetryDomainEvent.class);
    verify(eventPublisher, times(1)).publish(cap.capture());
    assertInstanceOf(GeofenceTransitionEvent.class, cap.getValue());
    var e = (GeofenceTransitionEvent) cap.getValue();
    assertEquals(GeofenceTransitionEvent.Transition.EXIT, e.type());
    assertFalse(state.lastInside().get());
  }

  @Test
  void process_shouldNotPublishGeofenceEvent_whenInsideStatusUnchanged() {
    var vid = new VehicleId("VIN00001");
    var prevPt = new GeoPoint(52.0, 13.0);
    var currPt = new GeoPoint(52.00005, 13.0);
    var t0 = Instant.parse("2025-01-01T00:00:00Z");
    var prev = new TelemetrySample(vid, t0, prevPt);
    var curr = new TelemetrySample(vid, t0.plusSeconds(3), currPt);
    var state = new VehicleTelemetryState();
    state.setLastSample(prev);
    state.setLastInside(true);
    state.setLastOverThreshold(false);
    when(stateStore.getOrCreate(vid)).thenReturn(state);
    when(speedCalculator.kmhBetween(prev, curr)).thenReturn(30.0);
    when(businessProps.speedThresholdKmh()).thenReturn(50.0);
    when(geoJsonGeofenceService.contains(currPt)).thenReturn(true);

    telemetryApplicationService.process(curr);

    verify(eventPublisher, never()).publish(isA(GeofenceTransitionEvent.class));
    assertTrue(state.lastInside().get());
  }

  @Test
  void process_shouldPublishExceededThenEnter_inSameCall_whenCrossingThresholdAndEnteringGeofence() {
    var vid = new VehicleId("VIN00001");
    var prevPt = new GeoPoint(52.0, 13.0);
    var currPt = new GeoPoint(52.001, 13.0);
    var t0 = Instant.parse("2025-01-01T00:00:00Z");
    var prev = new TelemetrySample(vid, t0, prevPt);
    var curr = new TelemetrySample(vid, t0.plusSeconds(5), currPt);
    var state = new VehicleTelemetryState();
    state.setLastSample(prev);
    state.setLastInside(false);
    state.setLastOverThreshold(false);
    when(stateStore.getOrCreate(vid)).thenReturn(state);
    when(speedCalculator.kmhBetween(prev, curr)).thenReturn(70.0);
    when(businessProps.speedThresholdKmh()).thenReturn(50.0);
    when(geoJsonGeofenceService.contains(currPt)).thenReturn(true);

    telemetryApplicationService.process(curr);

    var cap = ArgumentCaptor.forClass(TelemetryDomainEvent.class);
    verify(eventPublisher, times(2)).publish(cap.capture());
    List<TelemetryDomainEvent> events = cap.getAllValues();
    assertInstanceOf(SpeedExceededEvent.class, events.get(0));
    assertInstanceOf(GeofenceTransitionEvent.class, events.get(1));
    assertTrue(state.lastOverThreshold());
    assertTrue(state.lastInside().get());
  }

  @Test
  void process_shouldUpdateStateWithoutEvents_whenUnderThreshold() {
    var vid = new VehicleId("VIN00001");
    var prevPt = new GeoPoint(52.0, 13.0);
    var currPt = new GeoPoint(52.0001, 13.0);
    var t0 = Instant.parse("2025-01-01T00:00:00Z");
    var prev = new TelemetrySample(vid, t0, prevPt);
    var curr = new TelemetrySample(vid, t0.plusSeconds(5), currPt);
    var state = new VehicleTelemetryState();
    state.setLastSample(prev);
    state.setLastInside(false);
    state.setLastOverThreshold(false);
    when(stateStore.getOrCreate(vid)).thenReturn(state);
    when(speedCalculator.kmhBetween(prev, curr)).thenReturn(15.0);
    when(businessProps.speedThresholdKmh()).thenReturn(50.0);
    when(geoJsonGeofenceService.contains(currPt)).thenReturn(false);

    telemetryApplicationService.process(curr);

    verifyNoInteractions(eventPublisher);
    assertFalse(state.lastOverThreshold());
    assertFalse(state.lastInside().get());
    assertEquals(curr, state.lastSample().get());
  }

  @Test
  void process_shouldPublishUpdate_whenAlreadyOverAndEqualsThreshold() {
    var vid = new VehicleId("VIN00001");
    var prevPt = new GeoPoint(52.0, 13.0);
    var currPt = new GeoPoint(52.0005, 13.0);
    var t0 = Instant.parse("2025-01-01T00:00:00Z");
    var prev = new TelemetrySample(vid, t0, prevPt);
    var curr = new TelemetrySample(vid, t0.plusSeconds(2), currPt);
    var state = new VehicleTelemetryState();
    state.setLastSample(prev);
    state.setLastInside(true);
    state.setLastOverThreshold(true);
    when(stateStore.getOrCreate(vid)).thenReturn(state);
    when(speedCalculator.kmhBetween(prev, curr)).thenReturn(50.0);
    when(businessProps.speedThresholdKmh()).thenReturn(50.0);
    when(geoJsonGeofenceService.contains(currPt)).thenReturn(true);

    telemetryApplicationService.process(curr);

    var cap = ArgumentCaptor.forClass(TelemetryDomainEvent.class);
    verify(eventPublisher, times(1)).publish(cap.capture());
    assertInstanceOf(SpeedOverThresholdUpdateEvent.class, cap.getValue());
    assertTrue(state.lastOverThreshold());
  }
}


