package com.free2move.vehicleconsumer.telemetry.geo;

import com.free2move.vehicleconsumer.telemetry.model.domain.GeoPoint;

public interface GeofenceService {
  boolean contains(GeoPoint p);
}
