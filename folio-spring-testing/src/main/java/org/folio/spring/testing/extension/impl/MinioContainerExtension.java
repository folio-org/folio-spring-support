package org.folio.spring.testing.extension.impl;

import org.junit.jupiter.api.extension.AfterAllCallback;
import org.junit.jupiter.api.extension.BeforeAllCallback;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.testcontainers.containers.MinIOContainer;

public class MinioContainerExtension implements BeforeAllCallback, AfterAllCallback {
  static final String URL_PROPERTY_NAME = "folio.remote-storage.endpoint";
  static final String REGION_PROPERTY_NAME = "folio.remote-storage.region";
  static final String BUCKET_PROPERTY_NAME = "folio.remote-storage.bucket";
  static final String ACCESS_KEY_PROPERTY_NAME = "folio.remote-storage.accessKey";
  static final String SECRET_KEY_PROPERTY_NAME = "folio.remote-storage.secretKey";
  private static final String MINIO_IMAGE = "minio/minio:RELEASE.2024-01-18T22-51-28Z";
  private static final MinIOContainer CONTAINER = new MinIOContainer(MINIO_IMAGE);

  public void beforeAll(ExtensionContext context) {
    if (!CONTAINER.isRunning()) {
      CONTAINER.start();
    }

    System.setProperty(URL_PROPERTY_NAME, CONTAINER.getS3URL());
    System.setProperty(ACCESS_KEY_PROPERTY_NAME, CONTAINER.getUserName());
    System.setProperty(SECRET_KEY_PROPERTY_NAME, CONTAINER.getPassword());
    System.setProperty(REGION_PROPERTY_NAME, "region");
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
