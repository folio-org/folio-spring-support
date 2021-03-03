package org.folio.spring.controller;

import static org.folio.spring.integration.XOkapiHeaders.TENANT;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.startsWith;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.zonky.test.db.AutoConfigureEmbeddedDatabase;
import javax.sql.DataSource;
import lombok.SneakyThrows;
import org.folio.tenant.domain.dto.TenantAttributes;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.flyway.FlywayAutoConfiguration;
import org.springframework.boot.jdbc.DataSourceBuilder;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureEmbeddedDatabase(beanName = "dataSource")
@EnableAutoConfiguration(exclude = FlywayAutoConfiguration.class)
@AutoConfigureMockMvc
class TenantControllerIT {
  private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

  @Autowired
  private MockMvc mockMvc;

  @Configuration
  static class DbConfiguration {
    @Bean
    DataSource dataSource() {
      return DataSourceBuilder.create().build();
    }
  }

  @Test
  void canCallManagementEndpointWithoutTenantHeader(
      @Value("${management.endpoints.web.base-path:/admin}") String mgmtBasePath) throws Exception {
    mockMvc.perform(get(mgmtBasePath + "/health"))
      .andExpect(status().is(404)) // will be 200 if actuator is added
      .andExpect(content().string(not(startsWith("x-okapi-tenant header must be provided"))));
  }

  @Test
  void cannotCallOtherEndpointsWithoutTenantHeader() throws Exception {
    mockMvc.perform(get("/_/tenant"))
      .andExpect(status().is(400))
      .andExpect(content().string(startsWith("x-okapi-tenant header must be provided")));
  }

  @Test
  void canCreateTenant() throws Exception {
    mockMvc.perform(post("/_/tenant")
      .contentType(APPLICATION_JSON)
      .header(TENANT, "can_create_tenant")
      .content(toJsonString(new TenantAttributes().moduleTo("mod-example-1.0.0"))))
      .andExpect(status().isOk())
      .andExpect(content().string("true"));
  }

  @Test
  void canGetTenantWhenExists() throws Exception {
    final String tenant = "can_get_tenant";

    mockMvc.perform(post("/_/tenant")
      .contentType(APPLICATION_JSON)
      .header(TENANT, tenant)
      .content(toJsonString(new TenantAttributes().moduleTo("mod-example-1.0.0"))))
      .andExpect(status().isOk())
      .andExpect(content().string("true"));

    mockMvc.perform(get("/_/tenant")
      .header(TENANT, tenant))
      .andExpect(status().isOk())
      .andExpect(content().string("true"));
  }

  @Test
  void cannotGetNonexistentTenant() throws Exception {
    mockMvc.perform(get("/_/tenant")
      .header(TENANT, "not_existent_tenant"))
      .andExpect(status().isOk())
      .andExpect(content().string("false"));
  }

  @Test
  void canDeleteExistingTenant() throws Exception {
    final var tenant = "can_delete_existing_tenant";

    mockMvc.perform(post("/_/tenant")
      .contentType(APPLICATION_JSON)
      .header(TENANT, tenant)
      .content(toJsonString(new TenantAttributes().moduleTo("mod-example-1.0.0"))))
      .andExpect(status().isOk())
      .andExpect(content().string("true"));

    mockMvc.perform(delete("/_/tenant")
      .header(TENANT, tenant))
      .andExpect(status().isNoContent());
  }

  @Test
  void cannotDeleteNonexistentTenant() throws Exception {
    final String tenant = "cannot_delete_nonexistent_tenant";

    mockMvc.perform(delete("/_/tenant")
      .header(TENANT, tenant))
      .andExpect(status().isNotFound())
      .andExpect(content().string("Tenant does not exist: " + tenant));
  }

  @SneakyThrows
  private String toJsonString(Object obj) {
    return OBJECT_MAPPER.writeValueAsString(obj);
  }
}
