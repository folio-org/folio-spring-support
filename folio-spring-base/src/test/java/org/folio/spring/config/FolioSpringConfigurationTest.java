package org.folio.spring.config;

import static org.assertj.core.api.Assertions.assertThat;

import org.folio.spring.testing.type.UnitTest;
import org.junit.jupiter.api.Test;

@UnitTest
class FolioSpringConfigurationTest {

  private final FolioSpringConfiguration configuration = new FolioSpringConfiguration();

  @Test
  void folioModuleMetadata_positive_usesSpringApplicationVersion() {
    var metadata = configuration.folioModuleMetadata("mod-foo", "1.2.3");

    assertThat(metadata.getModuleName()).isEqualTo("mod-foo");
    assertThat(metadata.getModuleVersion()).contains("1.2.3");
    assertThat(metadata.getModuleId()).isEqualTo("mod-foo-1.2.3");
  }

  @Test
  void folioModuleMetadata_positive_usesModuleNameWhenVersionIsMissing() {
    var metadata = configuration.folioModuleMetadata("mod-foo", "");

    assertThat(metadata.getModuleName()).isEqualTo("mod-foo");
    assertThat(metadata.getModuleVersion()).isEmpty();
    assertThat(metadata.getModuleId()).isEqualTo("mod-foo");
  }
}
