package org.folio.spring.kafka.filtering.entitlement;

import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;

import java.util.Set;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.service.annotation.GetExchange;
import org.springframework.web.service.annotation.HttpExchange;

/**
 * HTTP client for resolving tenants entitled to a module.
 */
@HttpExchange(url = "entitlements", contentType = APPLICATION_JSON_VALUE)
public interface TenantEntitlementClient {

  /**
   * Returns tenants entitled to the provided module id.
   *
   * @param moduleId module id, for example {@code mod-foo-1.0.0}
   * @return entitled tenant ids
   */
  @GetExchange("/modules/{id}")
  Set<String> lookupTenantsByModuleId(@PathVariable("id") String moduleId);
}
