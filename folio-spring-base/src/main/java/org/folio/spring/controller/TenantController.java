package org.folio.spring.controller;

import static org.springframework.http.ResponseEntity.noContent;

import jakarta.validation.Valid;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.lang3.NotImplementedException;
import org.apache.commons.lang3.StringUtils;
import org.folio.spring.service.TenantService;
import org.folio.tenant.domain.dto.Parameter;
import org.folio.tenant.domain.dto.TenantAttributes;
import org.folio.tenant.rest.resource.TenantApi;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

@Log4j2
@RestController("defaultTenantController")
@ConditionalOnMissingBean(name = "folioTenantController")
@RequiredArgsConstructor
public class TenantController implements TenantApi {

  private static final String LOAD_REFERENCE_PARAM = "loadReference";
  private static final String LOAD_SAMPLE_PARAM = "loadSample";

  private final TenantService tenantService;

  @Override
  public ResponseEntity<Void> deleteTenant(String operationId) {
    /*
    do nothing.
     */
    return noContent().build();
  }

  @Override
  public ResponseEntity<String> getTenant(String operationId) {
    throw new NotImplementedException("Not implemented");
  }

  @Override
  public ResponseEntity<Void> postTenant(@Valid TenantAttributes tenantAttributes) {
    if (isDisableJob(tenantAttributes)) {
      log.info("Disabling tenant...");
      tenantService.deleteTenant(tenantAttributes);
    } else {
      log.info("Upgrading tenant...");
      tenantService.createOrUpdateTenant(tenantAttributes);
      var parameters = tenantAttributes.getParameters();
      var loadReferenceParam = getParameterValue(LOAD_REFERENCE_PARAM, parameters);
      if (loadReferenceParam.isPresent() && loadReferenceParam.get().equals("true")) {
        log.info("Loading reference data...");
        tenantService.loadReferenceData();
      }

      var loadSampleParam = getParameterValue(LOAD_SAMPLE_PARAM, parameters);
      if (loadSampleParam.isPresent() && loadSampleParam.get().equals("true")) {
        log.info("Loading sample data...");
        tenantService.loadSampleData();
      }
    }
    return noContent().build();
  }

  private Optional<String> getParameterValue(String paramName, List<Parameter> parameters) {
    if (parameters.isEmpty()) {
      return Optional.empty();
    }
    return parameters.stream()
      .filter(parameter -> parameter.getKey().equals(paramName))
      .findFirst()
      .map(Parameter::getValue);
  }

  private boolean isDisableJob(TenantAttributes tenantAttributes) {
    return StringUtils.isBlank(tenantAttributes.getModuleTo()) && tenantAttributes.getPurge();
  }
}
