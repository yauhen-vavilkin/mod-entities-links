package org.folio.support.extension.impl;

import static org.testcontainers.utility.DockerImageName.parse;

import org.junit.jupiter.api.extension.AfterAllCallback;
import org.junit.jupiter.api.extension.BeforeAllCallback;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;

public class PostgresContainerExtension implements BeforeAllCallback, AfterAllCallback {

  private static final String DATABASE_URL_PROPERTY_NAME = "spring.datasource.url";
  private static final String DATABASE_USERNAME_PROPERTY_NAME = "spring.datasource.username";
  private static final String DATABASE_PASSWORD_PROPERTY_NAME = "spring.datasource.password";

  private static final DockerImageName DOCKER_IMAGE = parse("postgres:12-alpine");

  private static final String DATABASE_NAME = "folio_test";
  private static final String DATABASE_USERNAME = "folio_admin";
  private static final String DATABASE_PASSWORD = "password";

  private static final PostgreSQLContainer<?> CONTAINER =
    new PostgreSQLContainer<>(DOCKER_IMAGE).withDatabaseName(DATABASE_NAME)
      .withUsername(DATABASE_USERNAME)
      .withPassword(DATABASE_PASSWORD);

  @Override
  public void beforeAll(ExtensionContext context) {
    if (!CONTAINER.isRunning()) {
      CONTAINER.start();
    }

    System.setProperty(DATABASE_URL_PROPERTY_NAME, CONTAINER.getJdbcUrl());
    System.setProperty(DATABASE_USERNAME_PROPERTY_NAME, CONTAINER.getUsername());
    System.setProperty(DATABASE_PASSWORD_PROPERTY_NAME, CONTAINER.getPassword());
  }

  @Override
  public void afterAll(ExtensionContext context) {
    System.clearProperty(DATABASE_URL_PROPERTY_NAME);
    System.clearProperty(DATABASE_USERNAME_PROPERTY_NAME);
    System.clearProperty(DATABASE_PASSWORD_PROPERTY_NAME);
  }
}
