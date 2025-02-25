package org.folio.spring.testing.extension.impl;

import static org.testcontainers.utility.DockerImageName.parse;

import org.junit.jupiter.api.extension.AfterAllCallback;
import org.junit.jupiter.api.extension.BeforeAllCallback;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.testcontainers.kafka.KafkaContainer;
import org.testcontainers.utility.DockerImageName;

/**
 * JUnit 5 extension for managing a Kafka Docker container.
 * This extension implements {@link BeforeAllCallback} and {@link AfterAllCallback}
 * to start the Kafka container before all tests and clear properties after all tests have executed.
 */
public class KafkaContainerExtension implements BeforeAllCallback, AfterAllCallback {

  private static final String SPRING_PROPERTY_NAME = "spring.kafka.bootstrap-servers";
  private static final DockerImageName KAFKA_IMAGE = parse("apache/kafka-native:3.8.0");
  private static final KafkaContainer CONTAINER = new KafkaContainer(KAFKA_IMAGE)
    .withEnv("KAFKA_AUTO_CREATE_TOPICS_ENABLE", "false")
    .withStartupAttempts(3);

  /**
   * Starts the Kafka container before all tests if it's not already running.
   * Sets the Spring property for Kafka bootstrap servers required for Kafka integration.
   *
   * @param context The extension context
   */
  @Override
  public void beforeAll(ExtensionContext context) {
    if (!CONTAINER.isRunning()) {
      CONTAINER.start();
    }

    System.setProperty(SPRING_PROPERTY_NAME, CONTAINER.getBootstrapServers());
  }

  /**
   * Clears the Spring property related to Kafka after all tests have executed.
   *
   * @param context The extension context
   */
  @Override
  public void afterAll(ExtensionContext context) {
    System.clearProperty(SPRING_PROPERTY_NAME);
  }
}
