package org.folio.spring.service;

import static com.github.tomakehurst.wiremock.client.WireMock.getRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.postRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.folio.spring.repository.SystemUserRepository;
import org.folio.spring.support.TestBase;
import org.folio.tenant.domain.dto.TenantAttributes;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.TestPropertySource;

@TestPropertySource({"classpath:enable-sys-user.properties", "classpath:application.properties"})
class SystemUserServiceIT extends TestBase {
  @Autowired
  private SystemUserRepository userRepository;

  @Test
  void shouldCreateSystemUserDuringTenantInit() throws Exception {
    var tenantId = "shouldCreateSystemUserDuringTenantInit";

    mockMvc.perform(post("/_/tenant")
      .headers(defaultHeaders(tenantId))
      .content(toJsonString(new TenantAttributes()
        .moduleTo("folio-spring-base-1.0.0"))))
      .andExpect(status().isOk());

    wireMockServer.verify(getRequestedFor(urlEqualTo("/users?query=username%3D%3Dfolio-spring-base")));
    wireMockServer.verify(postRequestedFor(urlEqualTo("/users")));
    wireMockServer.verify(postRequestedFor(urlEqualTo("/authn/credentials")));
    wireMockServer.verify(postRequestedFor(urlEqualTo("/perms/users")));
    wireMockServer.verify(postRequestedFor(urlEqualTo("/authn/login")));

    var systemUserOptional = userRepository.getByTenantId(tenantId);
    assertThat(systemUserOptional.isPresent(), is(true));
    assertThat(systemUserOptional.get().getTenantId(), is(tenantId));
    assertThat(systemUserOptional.get().getOkapiToken(), is("aa.bb.cc"));
    assertThat(systemUserOptional.get().getUsername(), is("folio-spring-base"));
    assertThat(systemUserOptional.get().getPassword(), is("folio-spring-base-pwd"));
  }
}
