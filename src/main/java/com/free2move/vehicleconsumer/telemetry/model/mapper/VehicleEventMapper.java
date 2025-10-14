package com.free2move.vehicleconsumer.telemetry.model.mapper;

import com.free2move.vehicleconsumer.telemetry.model.api.VehicleEventIn;
import com.free2move.vehicleconsumer.telemetry.model.domain.TelemetrySample;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface VehicleEventMapper {

  @Mapping(target = "vehicleId", expression = "java(new VehicleId(in.vin()))")
  @Mapping(target = "timestamp", source = "timestamp")
  @Mapping(target = "position",  expression = "java(new GeoPoint(in.location().latitude(), in.location().longitude()))")
  TelemetrySample toDomain(VehicleEventIn in);
}