package org.folio.spring.testing.extension.impl;

import org.junit.jupiter.api.extension.AfterAllCallback;
import org.junit.jupiter.api.extension.BeforeAllCallback;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.testcontainers.containers.PostgreSQLContainer;

/**
 * JUnit 5 extension for managing a PostgreSQL Docker container.
 * This extension implements {@link BeforeAllCallback} and {@link AfterAllCallback}
 * to start the container before all tests and stop it after all tests have executed.
 */
public class PostgresContainerExtension implements BeforeAllCallback, AfterAllCallback {

  private static final String URL_PROPERTY_NAME = "spring.datasource.url";
  private static final String USERNAME_PROPERTY_NAME = "spring.datasource.username";
  private static final String PASSWORD_PROPERTY_NAME = "spring.datasource.password";
  private static final String POSTGRES_IMAGE = "postgres:12-alpine";
  private static final PostgreSQLContainer<?> CONTAINER = new PostgreSQLContainer<>(POSTGRES_IMAGE)
    .withDatabaseName("folio_test").withUsername("folio_admin").withPassword("password");

  /**
   * Starts the PostgreSQL container before all tests if it's not already running.
   * Sets system properties for datasource URL, username, and password required by Spring.
   *
   * @param context The extension context
   */
  @Override
  public void beforeAll(ExtensionContext context) {
    if (!CONTAINER.isRunning()) {
      CONTAINER.start();
    }

    System.setProperty(URL_PROPERTY_NAME, CONTAINER.getJdbcUrl());
    System.setProperty(USERNAME_PROPERTY_NAME, CONTAINER.getUsername());
    System.setProperty(PASSWORD_PROPERTY_NAME, CONTAINER.getPassword());
  }

  /**
   * Clears the system properties related to the PostgreSQL container
   * after all tests have been executed.
   *
   * @param context The extension context
   */
  @Override
  public void afterAll(ExtensionContext context) {
    System.clearProperty(URL_PROPERTY_NAME);
    System.clearProperty(USERNAME_PROPERTY_NAME);
    System.clearProperty(PASSWORD_PROPERTY_NAME);
  }
}
