package com.free2move.vehicleconsumer.telemetry.application;

import com.free2move.vehicleconsumer.telemetry.model.domain.GeoPoint;
import com.free2move.vehicleconsumer.telemetry.model.domain.TelemetrySample;
import java.time.Duration;
import org.springframework.stereotype.Component;

@Component
public class SpeedCalculator {

  private static final double EARTH_RADIUS_M = 6_371_008.8;

  public double kmhBetween(TelemetrySample prev, TelemetrySample curr) {
    double seconds = Duration.between(prev.timestamp(), curr.timestamp()).toNanos() * 1e-9;
    if (seconds <= 0.0) {
      return 0.0;
    }
    double meters = metersBetween(prev.position(), curr.position());
    return (meters / 1000.0) / (seconds / 3600.0);
  }

  public double metersBetween(GeoPoint prev, GeoPoint curr) {
    double dLat = Math.toRadians(curr.latitude() - prev.latitude());
    double dLon = Math.toRadians(curr.longitude() - prev.longitude());
    if (dLat == 0.0 && dLon == 0.0) {
      return 0.0;
    }
    double lat1 = Math.toRadians(prev.latitude());
    double lat2 = Math.toRadians(curr.latitude());
    double sinH =
        Math.sin(dLat / 2) * Math.sin(dLat / 2) + Math.cos(lat1) * Math.cos(lat2) * Math.sin(
            dLon / 2) * Math.sin(dLon / 2);
    double c = 2 * Math.atan2(Math.sqrt(sinH), Math.sqrt(1.0 - sinH));
    return EARTH_RADIUS_M * c;
  }
}