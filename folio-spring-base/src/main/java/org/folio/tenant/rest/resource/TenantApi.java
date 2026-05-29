package org.folio.tenant.rest.resource;

import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;
import static org.springframework.http.MediaType.TEXT_PLAIN_VALUE;

import io.swagger.v3.oas.annotations.Hidden;
import jakarta.validation.Valid;
import org.folio.tenant.domain.dto.TenantAttributes;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

/**
 * REST API interface for FOLIO tenant lifecycle management.
 *
 * <p>Exposes endpoints under the {@code /_/tenant} path to create, upgrade, purge, and query
 * tenants in accordance with the FOLIO module descriptor contract.</p>
 */
@Hidden
@Validated
public interface TenantApi {

  String PATH_DELETE_TENANT = "/_/tenant/{operationId}";
  String PATH_GET_TENANT = "/_/tenant/{operationId}";
  String PATH_POST_TENANT = "/_/tenant";

  /**
   * Submits a tenant job (create, upgrade, or delete) with the supplied attributes.
   *
   * <p>Corresponds to {@code POST /_/tenant}.</p>
   *
   * @param tenantAttributes attributes describing the tenant job to perform; must not be null
   * @return {@code 204 No Content} when the job completes synchronously,
   *   {@code 400 Bad Request} if the request body is malformed,
   *   {@code 422 Unprocessable Entity} on validation errors,
   *   {@code 500 Internal Server Error} on unexpected failure
   */
  @PostMapping(value = TenantApi.PATH_POST_TENANT,
               produces = {APPLICATION_JSON_VALUE, TEXT_PLAIN_VALUE},
               consumes = APPLICATION_JSON_VALUE)
  ResponseEntity<Void> postTenant(@Valid @RequestBody TenantAttributes tenantAttributes);

  /**
   * Checks whether the tenant job identified by the given ID has completed.
   *
   * <p>Corresponds to {@code GET /_/tenant/{operationId}}.</p>
   *
   * @param operationId the ID of the tenant job to query
   * @return {@code 200 OK} with {@code "true"} if the operation exists and is complete,
   *   {@code "false"} otherwise;
   *   {@code 500 Internal Server Error} on unexpected failure
   */
  @GetMapping(value = TenantApi.PATH_GET_TENANT, produces = TEXT_PLAIN_VALUE)
  ResponseEntity<String> getTenant(@PathVariable String operationId);

  /**
   * Deletes the tenant job identified by the given operation ID.
   *
   * <p>Corresponds to {@code DELETE /_/tenant/{operationId}}.</p>
   *
   * @param operationId the ID of the tenant job to delete
   * @return {@code 204 No Content} on success,
   *   {@code 400 Bad Request} if the operation ID is invalid,
   *   {@code 500 Internal Server Error} on unexpected failure
   */
  @DeleteMapping(value = TenantApi.PATH_DELETE_TENANT, produces = TEXT_PLAIN_VALUE)
  ResponseEntity<Void> deleteTenant(@PathVariable String operationId);
}
