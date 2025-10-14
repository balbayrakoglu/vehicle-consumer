package com.free2move.vehicleconsumer.telemetry.geo;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.free2move.vehicleconsumer.telemetry.model.domain.GeoPoint;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class GeoJsonLoader {

  public static final String GEOMETRY = "geometry";
  private final ObjectMapper objectMapper;
  private final ResourceLoader resourceLoader;

  public List<List<GeoPoint>> loadPolygons(String location) {
    try {
      Resource resource = resourceLoader.getResource(location);
      try (InputStream is = resource.getInputStream()) {
        JsonNode root = objectMapper.readTree(is);
        List<List<GeoPoint>> result = new ArrayList<>();
        if (root.has("features")) {
          for (JsonNode f : root.get("features")) {
            parseGeometry(f.get(GEOMETRY), result);
          }
        } else if (root.has(GEOMETRY)) {
          parseGeometry(root.get(GEOMETRY), result);
        } else {
          parseGeometry(root, result);
        }
        return result;
      }
    } catch (Exception e) {
      throw new IllegalStateException("Failed to load GeoJSON from " + location, e);
    }
  }

  private void parseGeometry(JsonNode geometry, List<List<GeoPoint>> out) {
    if (geometry == null || geometry.isNull()) return;
    String type = geometry.get("type").asText();
    if ("Polygon".equalsIgnoreCase(type)) {
      parsePolygon(geometry.get("coordinates"), out);
    } else if ("MultiPolygon".equalsIgnoreCase(type)) {
      for (JsonNode poly : geometry.get("coordinates")) {
        parsePolygon(poly, out);
      }
    }
  }

  private void parsePolygon(JsonNode coordinates, List<List<GeoPoint>> out) {
    if (coordinates == null || !coordinates.isArray() || coordinates.isEmpty()) return;
    JsonNode outer = coordinates.get(0);
    List<GeoPoint> ring = new ArrayList<>(outer.size());
    for (JsonNode p : outer) {
      double lon = p.get(0).asDouble();
      double lat = p.get(1).asDouble();
      ring.add(new GeoPoint(lat, lon));
    }
    out.add(ring);
  }
}