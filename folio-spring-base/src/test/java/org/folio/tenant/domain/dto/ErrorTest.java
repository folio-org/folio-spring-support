package org.folio.tenant.domain.dto;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import org.folio.spring.testing.type.UnitTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.ObjectMapper;

@UnitTest
class ErrorTest {

  private ObjectMapper mapper;

  @BeforeEach
  void setUp() {
    mapper = new ObjectMapper();
  }

  @Test
  void defaultValuesOnNoArgConstruction() {
    var error = new Error();

    assertThat(error.getMessage()).isNull();
    assertThat(error.getType()).isNull();
    assertThat(error.getCode()).isNull();
    assertThat(error.getParameters()).isEmpty();
  }

  @Test
  void constructorWithMessageSetsMessage() {
    var error = new Error("something went wrong");

    assertThat(error.getMessage()).isEqualTo("something went wrong");
    assertThat(error.getType()).isNull();
    assertThat(error.getCode()).isNull();
    assertThat(error.getParameters()).isEmpty();
  }

  @Test
  void fluentSettersReturnSameInstance() {
    var error = new Error();

    assertThat(error.message("msg")).isSameAs(error);
    assertThat(error.type("t")).isSameAs(error);
    assertThat(error.code("c")).isSameAs(error);
    assertThat(error.parameters(List.of())).isSameAs(error);
  }

  @Test
  void fluentSettersStoreValues() {
    var param = new Parameter().key("k").value("v");
    var error = new Error()
      .message("msg")
      .type("ERROR")
      .code("ERR-001")
      .parameters(List.of(param));

    assertThat(error.getMessage()).isEqualTo("msg");
    assertThat(error.getType()).isEqualTo("ERROR");
    assertThat(error.getCode()).isEqualTo("ERR-001");
    assertThat(error.getParameters()).containsExactly(param);
  }

  @Test
  void addParametersItemAppendsToList() {
    var first = new Parameter().key("a");
    var second = new Parameter().key("b");
    var error = new Error()
      .addParametersItem(first)
      .addParametersItem(second);

    assertThat(error.getParameters()).containsExactly(first, second);
  }

  @Test
  void addParametersItemInitializesListWhenNull() {
    var error = new Error();
    error.setParameters(null);

    var param = new Parameter().key("x");
    error.addParametersItem(param);

    assertThat(error.getParameters()).containsExactly(param);
  }

  @Test
  void nullTypeAndCodeAreAccepted() {
    var error = new Error().type(null).code(null);

    assertThat(error.getType()).isNull();
    assertThat(error.getCode()).isNull();
  }

  @Test
  void equalObjectsHaveSameHashCode() {
    var a = new Error().message("msg").type("t").code("c");
    var b = new Error().message("msg").type("t").code("c");

    assertThat(a).isEqualTo(b).hasSameHashCodeAs(b);
  }

  @Test
  void toStringContainsFieldValues() {
    var error = new Error().message("msg").type("t").code("c");

    assertThat(error.toString()).contains("msg", "t", "c");
  }

  @Test
  void serializesFieldNamesCorrectly() {
    var error = new Error().message("msg").type("ERROR").code("ERR-001");

    var json = mapper.writeValueAsString(error);

    assertThat(json)
      .isEqualTo("""
        {"message":"msg","code":"ERR-001","parameters":[],"type":"ERROR"}""");
  }

  @Test
  void serializesNullTypeAndCodeAsNullNodes() {
    var error = new Error().message("msg").type(null).code(null);

    var json = mapper.writeValueAsString(error);

    assertThat(json).contains("\"type\":null", "\"code\":null");
  }

  @Test
  void serializesParametersList() {
    var error = new Error().message("msg")
      .addParametersItem(new Parameter().key("k").value("v"));

    var json = mapper.writeValueAsString(error);

    assertThat(json)
      .isEqualTo("""
        {"message":"msg","code":null,"parameters":[{"key":"k","value":"v"}],"type":null}""");
  }

  @Test
  void deserializesFromJson() {
    var json = """
      {
        "message": "msg",
        "type": "ERROR",
        "code": "ERR-001",
        "parameters": [
          {
            "key": "k",
            "value": "v"
          }
        ]
      }
      """;

    var error = mapper.readValue(json, Error.class);

    assertThat(error.getMessage()).isEqualTo("msg");
    assertThat(error.getType()).isEqualTo("ERROR");
    assertThat(error.getCode()).isEqualTo("ERR-001");
    assertThat(error.getParameters()).hasSize(1);
    assertThat(error.getParameters().getFirst())
      .extracting(Parameter::getKey, Parameter::getValue)
      .containsExactly("k", "v");
  }

  @Test
  void deserializesNullTypeAndCode() {
    var json = """
      {
        "message": "msg",
        "type": null,
        "code": null
      }
      """;

    var error = mapper.readValue(json, Error.class);

    assertThat(error.getType()).isNull();
    assertThat(error.getCode()).isNull();
  }

  @Test
  void roundTripPreservesValues() {
    var original = new Error()
      .message("msg")
      .type("ERROR")
      .code("ERR-001")
      .addParametersItem(new Parameter().key("k").value("v"));

    var error = mapper.readValue(mapper.writeValueAsString(original), Error.class);

    assertThat(error).isEqualTo(original);
  }
}
