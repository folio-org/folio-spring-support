package org.folio.spring.controller;

import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.folio.spring.support.TestBase;
import org.folio.tenant.domain.dto.TenantAttributes;
import org.junit.jupiter.api.Test;

class TenantControllerIT extends TestBase {
  @Test
  void canCreateTenant() throws Exception {
    mockMvc.perform(post("/_/tenant")
      .contentType(APPLICATION_JSON)
      .headers(defaultHeaders("can_create_tenant"))
      .content(toJsonString(new TenantAttributes().moduleTo("mod-example-1.0.0"))))
      .andExpect(status().isOk())
      .andExpect(content().string("true"));
  }

  @Test
  void canGetTenantWhenExists() throws Exception {
    final String tenant = "can_get_tenant";

    mockMvc.perform(post("/_/tenant")
      .contentType(APPLICATION_JSON)
      .headers(defaultHeaders(tenant))
      .content(toJsonString(new TenantAttributes().moduleTo("mod-example-1.0.0"))))
      .andExpect(status().isOk())
      .andExpect(content().string("true"));

    mockMvc.perform(get("/_/tenant")
      .headers(defaultHeaders(tenant)))
      .andExpect(status().isOk())
      .andExpect(content().string("true"));
  }

  @Test
  void cannotGetNonexistentTenant() throws Exception {
    mockMvc.perform(get("/_/tenant")
      .headers(defaultHeaders("not_existent_tenant")))
      .andExpect(status().isOk())
      .andExpect(content().string("false"));
  }

  @Test
  void canDeleteExistingTenant() throws Exception {
    final var tenant = "can_delete_existing_tenant";

    mockMvc.perform(post("/_/tenant")
      .contentType(APPLICATION_JSON)
      .headers(defaultHeaders(tenant))
      .content(toJsonString(new TenantAttributes().moduleTo("mod-example-1.0.0"))))
      .andExpect(status().isOk())
      .andExpect(content().string("true"));

    mockMvc.perform(delete("/_/tenant")
      .headers(defaultHeaders(tenant)))
      .andExpect(status().isNoContent());
  }

  @Test
  void cannotDeleteNonexistentTenant() throws Exception {
    final String tenant = "cannot_delete_nonexistent_tenant";

    mockMvc.perform(delete("/_/tenant")
      .headers(defaultHeaders(tenant)))
      .andExpect(status().isNotFound())
      .andExpect(content().string("Tenant does not exist: " + tenant));
  }
}
