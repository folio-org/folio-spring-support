package org.folio.spring.controller;

import static io.zonky.test.db.AutoConfigureEmbeddedDatabase.DatabaseProvider.ZONKY;
import static io.zonky.test.db.AutoConfigureEmbeddedDatabase.DatabaseType.POSTGRES;
import static org.folio.spring.integration.XOkapiHeaders.TENANT;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.startsWith;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.zonky.test.db.AutoConfigureEmbeddedDatabase;
import java.util.List;
import lombok.SneakyThrows;
import org.folio.spring.exception.TenantUpgradeException;
import org.folio.spring.filter.TenantOkapiHeaderValidationFilter;
import org.folio.tenant.domain.dto.Parameter;
import org.folio.tenant.domain.dto.TenantAttributes;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.flyway.FlywayAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest(
  properties = {
    "header.validation.x-okapi-tenant.exclude.base-paths=/admin,/swagger-ui"
  })
@AutoConfigureEmbeddedDatabase(beanName = "dataSource", type = POSTGRES, provider = ZONKY)
@EnableAutoConfiguration(exclude = {FlywayAutoConfiguration.class})
@AutoConfigureMockMvc
class TenantControllerIT {

  private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

  @Autowired
  private MockMvc mockMvc;

  @Test
  void canCallManagementEndpointWithoutTenantHeader(
    @Value("${header.validation.x-okapi-tenant.exclude.base-paths}") String[] excludeBasePaths) throws Exception {
    for (String basePath : excludeBasePaths) {
      mockMvc.perform(get(basePath))
        .andExpect(status().is(404))
        .andExpect(content().string(not(startsWith(TenantOkapiHeaderValidationFilter.ERROR_MSG))));
    }
  }

  @Test
  void cannotCallOtherEndpointsWithoutTenantHeader() throws Exception {
    mockMvc.perform(get("/_/tenant"))
      .andExpect(status().is(400))
      .andExpect(content().string(startsWith(TenantOkapiHeaderValidationFilter.ERROR_MSG)));
  }

  @Test
  void canEnableTenant() throws Exception {
    mockMvc.perform(post("/_/tenant")
        .contentType(APPLICATION_JSON)
        .header(TENANT, "can_enable_tenant")
        .content(toJsonString(new TenantAttributes().moduleTo("mod-example-1.0.0"))))
      .andExpect(status().isNoContent());
  }

  @Test
  void canDeleteTenant() throws Exception {
    mockMvc.perform(delete("/_/tenant/{operationId}", "NO_OP")
        .contentType(APPLICATION_JSON)
        .header(TENANT, "can_delete_tenant"))
      .andExpect(status().isNoContent());
  }

  @Test
  void canNotEnableTenantWithInvalidTenantName() throws Exception {
    mockMvc.perform(post("/_/tenant")
        .contentType(APPLICATION_JSON)
        .header(TENANT, "123_tenant")
        .content(toJsonString(new TenantAttributes().moduleTo("mod-example-1.0.0"))))
      .andExpect(status().isBadRequest())
      .andExpect(result -> assertThat(result.getResolvedException(), instanceOf(TenantUpgradeException.class)));
  }

  @Test
  void canUpgradeTenant() throws Exception {
    mockMvc.perform(post("/_/tenant")
        .contentType(APPLICATION_JSON)
        .header(TENANT, "tenant")
        .content(toJsonString(new TenantAttributes().moduleTo("mod-example-1.0.0"))))
      .andExpect(status().isNoContent());

    mockMvc.perform(post("/_/tenant")
        .contentType(APPLICATION_JSON)
        .header(TENANT, "tenant")
        .content(toJsonString(new TenantAttributes()
          .moduleTo("mod-example-1.0.0")
          .moduleFrom("mod-example-0.0.1")
          .parameters(List.of(new Parameter().key("loadReference").value("true"),
            new Parameter().key("loadSample").value("true"))))))
      .andExpect(status().isNoContent());
  }

  @Test
  void canDisableTenant() throws Exception {
    mockMvc.perform(post("/_/tenant")
        .contentType(APPLICATION_JSON)
        .header(TENANT, "tenant")
        .content(toJsonString(new TenantAttributes().moduleFrom("mod-example-1.0.0").purge(true))))
      .andExpect(status().isNoContent());
  }

  @Test
  void canDisableTenantThatExist() throws Exception {
    mockMvc.perform(post("/_/tenant")
        .contentType(APPLICATION_JSON)
        .header(TENANT, "tenant")
        .content(toJsonString(new TenantAttributes().moduleTo("mod-example-1.0.0"))))
      .andExpect(status().isNoContent());

    mockMvc.perform(post("/_/tenant")
        .contentType(APPLICATION_JSON)
        .header(TENANT, "tenant")
        .content(toJsonString(new TenantAttributes().moduleFrom("mod-example-1.0.0").purge(true))))
      .andExpect(status().isNoContent());
  }

  @SneakyThrows
  private String toJsonString(Object obj) {
    return OBJECT_MAPPER.writeValueAsString(obj);
  }

  @Configuration
  static class DbConfiguration {
  }
}
