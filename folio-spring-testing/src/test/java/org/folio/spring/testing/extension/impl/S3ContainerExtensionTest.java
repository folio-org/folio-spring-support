package org.folio.spring.testing.extension.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import org.folio.spring.testing.type.UnitTest;
import org.junit.jupiter.api.Test;

@UnitTest
class S3ContainerExtensionTest {

  @Test
  void beforeAllAddSystemProperties_positive() {
    // Act
    S3ContainerExtension extension = new S3ContainerExtension();
    extension.beforeAll(null);
    // Assert

    assertThat(System.getProperty(S3ContainerExtension.URL_PROPERTY_NAME)).startsWith("http://");
    assertEquals("test", System.getProperty(S3ContainerExtension.ACCESS_KEY_PROPERTY_NAME));
    assertEquals("test", System.getProperty(S3ContainerExtension.SECRET_KEY_PROPERTY_NAME));
    assertEquals("us-east-1", System.getProperty(S3ContainerExtension.REGION_PROPERTY_NAME));
    assertEquals("test-bucket", System.getProperty(S3ContainerExtension.BUCKET_PROPERTY_NAME));
  }

  @Test
  void afterAllRemoveSystemProperties_positive() {
    // Act
    S3ContainerExtension extension = new S3ContainerExtension();
    extension.afterAll(null);

    // Assert
    assertNull(System.getProperty(S3ContainerExtension.URL_PROPERTY_NAME));
    assertNull(System.getProperty(S3ContainerExtension.ACCESS_KEY_PROPERTY_NAME));
    assertNull(System.getProperty(S3ContainerExtension.SECRET_KEY_PROPERTY_NAME));
    assertNull(System.getProperty(S3ContainerExtension.REGION_PROPERTY_NAME));
    assertNull(System.getProperty(S3ContainerExtension.BUCKET_PROPERTY_NAME));
  }
}
