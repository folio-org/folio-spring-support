package org.folio.spring.controller;

import static org.springframework.http.ResponseEntity.noContent;

import java.util.List;
import java.util.Optional;

import javax.validation.Valid;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.lang3.StringUtils;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

import org.folio.spring.service.TenantService;
import org.folio.tenant.domain.dto.Parameter;
import org.folio.tenant.domain.dto.TenantAttributes;
import org.folio.tenant.rest.resource.TenantApi;

@Log4j2
@RestController("defaultTenantController")
@ConditionalOnMissingBean(name = "folioTenantController")
@RequiredArgsConstructor
public class TenantController implements TenantApi {

  private static final String LOAD_REFERENCE_PARAM = "loadReference";
  private static final String LOAD_SAMPLE_PARAM = "loadSample";

  private final TenantService tenantService;

  @Override
  public ResponseEntity<Void> postTenant(@Valid TenantAttributes tenantAttributes) {
    if (isDisableJob(tenantAttributes)) {
      log.info("Disabling tenant...");
      disableTenant();
    } else {
      log.info("Upgrading tenant...");
      upgradeTenant();
      var parameters = tenantAttributes.getParameters();
      var loadReferenceParam = getParameterValue(LOAD_REFERENCE_PARAM, parameters);
      if (loadReferenceParam.isPresent() && loadReferenceParam.get().equals("true")) {
        loadReferenceData();
      }

      var loadSampleParam = getParameterValue(LOAD_SAMPLE_PARAM, parameters);
      if (loadSampleParam.isPresent() && loadSampleParam.get().equals("true")) {
        loadSampleData();
      }
    }
    return noContent().build();
  }

  protected void upgradeTenant() {
    tenantService.createTenantIfNotExist();
  }

  protected void disableTenant() {
    tenantService.deleteTenant();
  }

  protected void loadReferenceData() {

  }

  protected void loadSampleData() {

  }

  private Optional<String> getParameterValue(String paramName, List<Parameter> parameters) {
    if (parameters == null || parameters.isEmpty()) {
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
