package org.folio.tenant.domain.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeName;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.jspecify.annotations.Nullable;

@Data
@NoArgsConstructor
@JsonTypeName("parameter")
public final class Parameter {

  @JsonProperty("key")
  private String key;

  @JsonProperty("value")
  private @Nullable String value;

  public Parameter(String key) {
    this.key = key;
  }

  public Parameter key(String key) {
    this.key = key;
    return this;
  }

  public Parameter value(@Nullable String value) {
    this.value = value;
    return this;
  }
}

