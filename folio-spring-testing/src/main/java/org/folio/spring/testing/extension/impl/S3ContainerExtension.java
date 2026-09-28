package org.folio.spring.testing.extension.impl;

import org.jspecify.annotations.NullMarked;
import org.junit.jupiter.api.extension.AfterAllCallback;
import org.junit.jupiter.api.extension.BeforeAllCallback;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.testcontainers.localstack.LocalStackContainer;
import org.testcontainers.utility.DockerImageName;

@NullMarked
public class S3ContainerExtension implements BeforeAllCallback, AfterAllCallback {

  static final String URL_PROPERTY_NAME = "folio.remote-storage.endpoint";
  static final String REGION_PROPERTY_NAME = "folio.remote-storage.region";
  static final String BUCKET_PROPERTY_NAME = "folio.remote-storage.bucket";
  static final String ACCESS_KEY_PROPERTY_NAME = "folio.remote-storage.accessKey";
  static final String SECRET_KEY_PROPERTY_NAME = "folio.remote-storage.secretKey";
  private static final LocalStackContainer CONTAINER =
    new LocalStackContainer(DockerImageName.parse("localstack/localstack:s3-community-archive"))
      .withServices("s3");

  public void beforeAll(ExtensionContext context) {
    if (!CONTAINER.isRunning()) {
      CONTAINER.start();
    }

    System.setProperty(URL_PROPERTY_NAME, CONTAINER.getEndpoint().toString());
    System.setProperty(ACCESS_KEY_PROPERTY_NAME, CONTAINER.getAccessKey());
    System.setProperty(SECRET_KEY_PROPERTY_NAME, CONTAINER.getSecretKey());
    System.setProperty(REGION_PROPERTY_NAME, CONTAINER.getRegion());
    System.setProperty(BUCKET_PROPERTY_NAME, "test-bucket");
  }

  public void afterAll(ExtensionContext context) {
    System.clearProperty(URL_PROPERTY_NAME);
    System.clearProperty(ACCESS_KEY_PROPERTY_NAME);
    System.clearProperty(SECRET_KEY_PROPERTY_NAME);
    System.clearProperty(REGION_PROPERTY_NAME);
    System.clearProperty(BUCKET_PROPERTY_NAME);
  }
}
