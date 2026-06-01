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
@JsonTypeName("error")
public final class Error {

  @JsonProperty("message")
  private String message;

  @JsonProperty("type")
  private @Nullable String type;

  @JsonProperty("code")
  private @Nullable String code;

  @Valid
  @JsonProperty("parameters")
  private List<@Valid Parameter> parameters = new ArrayList<>();

  public Error(String message) {
    this.message = message;
  }

  public Error message(String message) {
    this.message = message;
    return this;
  }

  public Error type(@Nullable String type) {
    this.type = type;
    return this;
  }

  public Error code(@Nullable String code) {
    this.code = code;
    return this;
  }

  public Error parameters(List<@Valid Parameter> parameters) {
    this.parameters = parameters;
    return this;
  }

  public Error addParametersItem(Parameter parametersItem) {
    if (this.parameters == null) {
      this.parameters = new ArrayList<>();
    }
    this.parameters.add(parametersItem);
    return this;
  }
}

