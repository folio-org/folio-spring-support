package org.folio.spring.testing.extension.impl;

import com.github.tomakehurst.wiremock.WireMockServer;

/**
 * Represents the configuration for an Okapi WireMock server.
 * This record encapsulates the WireMock server and its port, providing a method to retrieve the Okapi URL.
 */
public record OkapiConfiguration(WireMockServer wireMockServer, int port) {

  public String getOkapiUrl() {
    return "http://localhost:" + port;
  }
}
