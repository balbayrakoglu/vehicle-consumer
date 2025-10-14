package com.free2move.vehicleconsumer;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication
@ConfigurationPropertiesScan(basePackages = "com.free2move.vehicleconsumer.config")
public class VehicleConsumerApplication {

	public static void main(String[] args) {
		SpringApplication.run(VehicleConsumerApplication.class, args);
	}

}
