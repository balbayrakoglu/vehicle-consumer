package com.free2move.vehicleconsumer.telemetry.geo;

import com.free2move.vehicleconsumer.config.GeofenceProps;
import com.free2move.vehicleconsumer.telemetry.model.domain.GeoPoint;
import jakarta.annotation.PostConstruct;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class GeoJsonGeofenceService implements GeofenceService {

  private final GeoJsonLoader loader;
  private final GeofenceProps props;
  private List<List<GeoPoint>> polygons;

  @PostConstruct
  void init() {
    this.polygons = loader.loadPolygons(props.geojson());
  }

  @Override
  public boolean contains(GeoPoint p) {
    for (var poly : polygons) {
      if (pointInPolygon(p, poly)) return true;
    }
    return false;
  }

  private boolean pointInPolygon(GeoPoint p, List<GeoPoint> polygon) {
    boolean inside = false;
    for (int i = 0, j = polygon.size() - 1; i < polygon.size(); j = i++) {
      double xi = polygon.get(i).longitude();
      double yi = polygon.get(i).latitude();
      double xj = polygon.get(j).longitude();
      double yj = polygon.get(j).latitude();
      boolean intersect = ((yi > p.latitude()) != (yj > p.latitude())) &&
          (p.longitude() < (xj - xi) * (p.latitude() - yi) / (yj - yi + 0.0) + xi);
      if (intersect) {
        inside = !inside;
      }
    }
    return inside;
  }
}