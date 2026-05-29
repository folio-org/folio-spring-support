package org.folio.tenant.domain.dto;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import org.folio.spring.testing.type.UnitTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.ObjectMapper;

@UnitTest
class ErrorsTest {

  private ObjectMapper mapper;

  @BeforeEach
  void setUp() {
    mapper = new ObjectMapper();
  }

  @Test
  void defaultValuesOnNoArgConstruction() {
    var errors = new Errors();

    assertThat(errors.getErrors()).isEmpty();
    assertThat(errors.getTotalRecords()).isNull();
  }

  @Test
  void fluentSettersReturnSameInstance() {
    var errors = new Errors();

    assertThat(errors.errors(List.of())).isSameAs(errors);
    assertThat(errors.totalRecords(0)).isSameAs(errors);
  }

  @Test
  void fluentSettersStoreValues() {
    var error = new Error("msg");
    var errors = new Errors()
      .errors(List.of(error))
      .totalRecords(1);

    assertThat(errors.getErrors()).containsExactly(error);
    assertThat(errors.getTotalRecords()).isEqualTo(1);
  }

  @Test
  void addErrorsItemAppendsToList() {
    var first = new Error("first");
    var second = new Error("second");
    var errors = new Errors()
      .addErrorsItem(first)
      .addErrorsItem(second);

    assertThat(errors.getErrors()).containsExactly(first, second);
  }

  @Test
  void addErrorsItemInitializesListWhenNull() {
    var errors = new Errors();
    errors.setErrors(null);

    var error = new Error("msg");
    errors.addErrorsItem(error);

    assertThat(errors.getErrors()).containsExactly(error);
  }

  @Test
  void nullTotalRecordsIsAccepted() {
    var errors = new Errors().totalRecords(null);

    assertThat(errors.getTotalRecords()).isNull();
  }

  @Test
  void equalObjectsHaveSameHashCode() {
    var a = new Errors().addErrorsItem(new Error("msg")).totalRecords(1);
    var b = new Errors().addErrorsItem(new Error("msg")).totalRecords(1);

    assertThat(a).isEqualTo(b).hasSameHashCodeAs(b);
  }

  @Test
  void toStringContainsFieldValues() {
    var errors = new Errors().totalRecords(5);

    assertThat(errors.toString()).contains("5");
  }

  @Test
  void serializesSnakeCaseTotalRecords() {
    var errors = new Errors().totalRecords(3);

    var json = mapper.writeValueAsString(errors);

    assertThat(json).contains("\"total_records\":3");
  }

  @Test
  void serializesErrorsList() {
    var errors = new Errors().addErrorsItem(new Error("msg").type("t").code("c"));

    var json = mapper.writeValueAsString(errors);

    assertThat(json)
      .isEqualTo("""
        {"errors":[{"message":"msg","code":"c","parameters":[],"type":"t"}],"total_records":null}""");
  }

  @Test
  void serializesNullTotalRecordsAsNullNode() {
    var errors = new Errors().totalRecords(null);

    var json = mapper.writeValueAsString(errors);

    assertThat(json).contains("\"total_records\":null");
  }

  @Test
  void deserializesFromJson() {
    var json = """
      {
        "errors": [
          {
            "message": "msg",
            "type": "t",
            "code": "c",
            "parameters": [ ]
          }
        ],
        "total_records": 1
      }
      """;

    var errors = mapper.readValue(json, Errors.class);

    assertThat(errors.getTotalRecords()).isEqualTo(1);
    assertThat(errors.getErrors()).hasSize(1);
    assertThat(errors.getErrors().getFirst())
      .extracting(Error::getMessage, Error::getType, Error::getCode)
      .containsExactly("msg", "t", "c");
  }

  @Test
  void deserializesNullTotalRecords() {
    var json = """
      {
        "errors": [ ],
        "total_records": null
      }
      """;

    var errors = mapper.readValue(json, Errors.class);

    assertThat(errors.getTotalRecords()).isNull();
  }

  @Test
  void roundTripPreservesValues() {
    var original = new Errors()
      .addErrorsItem(new Error("msg").type("t").code("c"))
      .totalRecords(1);

    var errors = mapper.readValue(mapper.writeValueAsString(original), Errors.class);

    assertThat(errors).isEqualTo(original);
  }
}
