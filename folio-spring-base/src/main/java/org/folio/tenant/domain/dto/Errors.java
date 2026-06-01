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
@JsonTypeName("errors")
public final class Errors {

  @Valid
  @JsonProperty("errors")
  private List<@Valid Error> errors = new ArrayList<>();

  @JsonProperty("total_records")
  private @Nullable Integer totalRecords;

  public Errors errors(List<@Valid Error> errors) {
    this.errors = errors;
    return this;
  }

  public Errors addErrorsItem(Error errorsItem) {
    if (this.errors == null) {
      this.errors = new ArrayList<>();
    }
    this.errors.add(errorsItem);
    return this;
  }

  public Errors totalRecords(@Nullable Integer totalRecords) {
    this.totalRecords = totalRecords;
    return this;
  }
}

