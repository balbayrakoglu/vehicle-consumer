package com.free2move.vehicleconsumer.telemetry.messaging;

import com.free2move.vehicleconsumer.telemetry.application.TelemetryApplicationService;
import com.free2move.vehicleconsumer.telemetry.model.api.VehicleEventIn;
import com.free2move.vehicleconsumer.telemetry.model.mapper.VehicleEventMapper;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Service;

@Service
@Slf4j
@RequiredArgsConstructor
public class VehicleEventListener {

    private final VehicleEventMapper vehicleEventMapper;
    private final TelemetryApplicationService telemetryApplicationService;

    @RabbitListener(
            queues = "${app.rabbit.queue}",
            containerFactory = "rabbitListenerContainerFactory"
    )
    public void onMessage(@Valid VehicleEventIn in) {
        var sample = vehicleEventMapper.toDomain(in);

        log.info(
                "telemetry.received vin={} ts={} lat={} lon={}",
                sample.vehicleId(),
                sample.timestamp(),
                sample.position().latitude(),
                sample.position().longitude()
        );

        telemetryApplicationService.process(sample);

        log.info("telemetry.processed vin={} ts={}", sample.vehicleId(), sample.timestamp());
    }
}