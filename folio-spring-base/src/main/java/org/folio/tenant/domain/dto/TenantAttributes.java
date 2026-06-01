package org.folio.tenant.domain.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeName;
import jakarta.validation.Valid;
import java.util.ArrayList;
import java.util.List;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.jspecify.annotations.Nullable;

@Data
@NoArgsConstructor
@JsonTypeName("tenantAttributes")
public final class TenantAttributes {

  @JsonProperty("module_from")
  private @Nullable String moduleFrom;

  @JsonProperty("module_to")
  private @Nullable String moduleTo;

  @JsonProperty("purge")
  private Boolean purge = true;

  @Valid
  @JsonProperty("parameters")
  private List<@Valid Parameter> parameters = new ArrayList<>();

  public TenantAttributes moduleFrom(@Nullable String moduleFrom) {
    this.moduleFrom = moduleFrom;
    return this;
  }

  public TenantAttributes moduleTo(@Nullable String moduleTo) {
    this.moduleTo = moduleTo;
    return this;
  }

  public TenantAttributes purge(Boolean purge) {
    this.purge = purge;
    return this;
  }

  public TenantAttributes parameters(List<@Valid Parameter> parameters) {
    this.parameters = parameters;
    return this;
  }

  public TenantAttributes addParametersItem(Parameter parametersItem) {
    if (this.parameters == null) {
      this.parameters = new ArrayList<>();
    }
    this.parameters.add(parametersItem);
    return this;
  }
}

