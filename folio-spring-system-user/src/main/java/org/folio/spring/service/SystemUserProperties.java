package org.folio.spring.service;

import org.apache.commons.lang3.StringUtils;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties("folio.system-user")
public record SystemUserProperties(String username, String password, String lastname, String permissionsFilePath) {
  public SystemUserProperties {
    if (StringUtils.isEmpty(password)) {
      throw new IllegalArgumentException("system user password must be configured to be non-empty");
    }
  }
}
