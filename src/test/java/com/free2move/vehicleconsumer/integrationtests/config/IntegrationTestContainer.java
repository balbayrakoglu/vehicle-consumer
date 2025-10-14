package com.free2move.vehicleconsumer.integrationtests.config;

import org.testcontainers.containers.RabbitMQContainer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.utility.DockerImageName;

public class IntegrationTestContainer {

  public static RabbitMQContainer rabbitContainer;

  static {
    if (System.getProperty("jenkins") == null) {
      DockerImageName rabbitImage = DockerImageName.parse("rabbitmq:3.13-management")
          .asCompatibleSubstituteFor("rabbitmq");
      rabbitContainer = new RabbitMQContainer(rabbitImage)
          .withReuse(false)
          .waitingFor(Wait.forLogMessage(".*Server startup complete.*\\n", 1));
      rabbitContainer.start();
    }
  }
}