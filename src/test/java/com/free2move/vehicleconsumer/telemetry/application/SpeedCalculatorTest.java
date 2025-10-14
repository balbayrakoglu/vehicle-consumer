package com.free2move.vehicleconsumer.telemetry.application;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.free2move.vehicleconsumer.telemetry.model.domain.GeoPoint;
import com.free2move.vehicleconsumer.telemetry.model.domain.TelemetrySample;
import com.free2move.vehicleconsumer.telemetry.model.domain.VehicleId;
import java.time.Instant;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class SpeedCalculatorTest {

  private final SpeedCalculator speedCalculator = new SpeedCalculator();

  @Test
  void kmhBetween_shouldReturnPositiveValue_whenGivenTwoCoordinates() {
    GeoPoint prevPt = new GeoPoint(48.775556, 9.182932);
    GeoPoint currPt = new GeoPoint(48.775846, 9.182932);
    VehicleId vid = new VehicleId("VIN00001");
    Instant t0 = Instant.parse("2025-01-01T00:00:00Z");

    TelemetrySample prev = new TelemetrySample(vid, t0, prevPt);
    TelemetrySample curr = new TelemetrySample(vid, t0.plusSeconds(10), currPt);

    double kmh = speedCalculator.kmhBetween(prev, curr);

    assertEquals(11.608766376368003, kmh);
  }

  @Test
  void kmhBetween_shouldReturnZero_whenGivenSameCoordinates() {
    GeoPoint pt = new GeoPoint(48.775846, 9.182932);
    VehicleId vid = new VehicleId("VIN00001");
    Instant t0 = Instant.parse("2025-01-01T00:00:00Z");

    TelemetrySample prev = new TelemetrySample(vid, t0, pt);
    TelemetrySample curr = new TelemetrySample(vid, t0.plusSeconds(10), pt);

    double kmh = speedCalculator.kmhBetween(prev, curr);

    assertEquals(0.0, kmh);
  }

  @Test
  void kmhBetween_shouldReturnZero_whenGivenSameTimestamp() {
    GeoPoint prevPt = new GeoPoint(48.775556, 9.182932);
    GeoPoint currPt = new GeoPoint(48.775846, 9.182932);
    VehicleId vid = new VehicleId("VIN00001");
    Instant t0 = Instant.parse("2025-01-01T00:00:00Z");

    TelemetrySample prev = new TelemetrySample(vid, t0, prevPt);
    TelemetrySample curr = new TelemetrySample(vid, t0, currPt);

    double kmh = speedCalculator.kmhBetween(prev, curr);

    assertEquals(0.0, kmh);
  }

  @Test
  void metersBetween_shouldReturnPositiveValue_whenGivenTwoCoordinates() {
    GeoPoint geoPointCurr = new GeoPoint(48.775846, 9.182932);
    GeoPoint geoPointPrev = new GeoPoint(48.775556, 9.182932);
    double result = speedCalculator.metersBetween(geoPointPrev, geoPointCurr);
    assertEquals(32.2465732676889, result);
  }

  @Test
  void metersBetween_shouldReturnZero_whenGivenSameCoordinates() {
    GeoPoint geoPoint = new GeoPoint(48.775846, 9.182932);
    double result = speedCalculator.metersBetween(geoPoint, geoPoint);
    assertEquals(0, result);
  }
}
