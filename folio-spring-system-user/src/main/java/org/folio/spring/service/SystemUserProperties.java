package org.folio.spring.service;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties("folio.system-user")
public record SystemUserProperties(String username, String password, String lastname, String permissionsFilePath) {
}
