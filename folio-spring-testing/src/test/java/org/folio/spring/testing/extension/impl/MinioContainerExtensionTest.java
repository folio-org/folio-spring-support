package org.folio.spring.testing.extension.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import org.folio.spring.testing.type.UnitTest;
import org.junit.jupiter.api.Test;

@UnitTest
class MinioContainerExtensionTest {

  @Test
  void beforeAllAddSystemProperties_positive() {
    // Act
    MinioContainerExtension extension = new MinioContainerExtension();
    extension.beforeAll(null);
    // Assert

    assertThat(System.getProperty(MinioContainerExtension.URL_PROPERTY_NAME)).startsWith("http://");
    assertEquals("minioadmin", System.getProperty(MinioContainerExtension.ACCESS_KEY_PROPERTY_NAME));
    assertEquals("minioadmin", System.getProperty(MinioContainerExtension.SECRET_KEY_PROPERTY_NAME));
    assertEquals("region", System.getProperty(MinioContainerExtension.REGION_PROPERTY_NAME));
    assertEquals("test-bucket", System.getProperty(MinioContainerExtension.BUCKET_PROPERTY_NAME));
  }

  @Test
  void afterAllRemoveSystemProperties_positive() {
    // Act
    MinioContainerExtension extension = new MinioContainerExtension();
    extension.afterAll(null);

    // Assert
    assertNull(System.getProperty(MinioContainerExtension.URL_PROPERTY_NAME));
    assertNull(System.getProperty(MinioContainerExtension.ACCESS_KEY_PROPERTY_NAME));
    assertNull(System.getProperty(MinioContainerExtension.SECRET_KEY_PROPERTY_NAME));
    assertNull(System.getProperty(MinioContainerExtension.REGION_PROPERTY_NAME));
    assertNull(System.getProperty(MinioContainerExtension.BUCKET_PROPERTY_NAME));
  }
}
