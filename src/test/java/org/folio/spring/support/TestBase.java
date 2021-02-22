package org.folio.spring.support;

import static org.folio.spring.integration.XOkapiHeaders.TENANT;
import static org.folio.spring.integration.XOkapiHeaders.URL;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.util.SocketUtils.findAvailableTcpPort;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.tomakehurst.wiremock.WireMockServer;
import io.zonky.test.db.AutoConfigureEmbeddedDatabase;
import lombok.SneakyThrows;
import org.folio.tenant.domain.dto.TenantAttributes;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest(classes = DbConfiguration.class)
@AutoConfigureEmbeddedDatabase(beanName = "dataSource")
@AutoConfigureMockMvc
public abstract class TestBase {
  private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
  private static final int WIRE_MOCK_PORT = findAvailableTcpPort();
  public static final String TENANT_NAME = "test_tenant";
  protected static WireMockServer wireMockServer;

  @Autowired
  protected MockMvc mockMvc;

  @BeforeAll
  static void testSetup(@Autowired MockMvc mockMvc) throws Exception {
    wireMockServer = new WireMockServer(WIRE_MOCK_PORT);
    wireMockServer.start();

    mockMvc.perform(post("/_/tenant")
      .headers(defaultHeaders(TENANT_NAME))
      .content(toJsonString(new TenantAttributes()
        .moduleTo("folio-spring-base-1.0.0"))))
      .andExpect(status().isOk())
      .andExpect(content().string("true"));
  }

  @AfterAll
  static void tearDown() {
    wireMockServer.stop();
  }

  public static HttpHeaders defaultHeaders(String tenant) {
    var headers = new HttpHeaders();
    headers.setContentType(APPLICATION_JSON);
    headers.add(TENANT, tenant);
    headers.add(URL, getOkapiUrl());

    return headers;
  }

  @SneakyThrows
  public static String toJsonString(Object obj) {
    return OBJECT_MAPPER.writeValueAsString(obj);
  }

  public static String getOkapiUrl() {
    return String.format("http://localhost:%s", WIRE_MOCK_PORT);
  }
}
