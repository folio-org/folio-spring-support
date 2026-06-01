package org.folio.tenant.domain.dto;

import static org.assertj.core.api.Assertions.assertThat;

import org.folio.spring.testing.type.UnitTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.ObjectMapper;

@UnitTest
class ParameterTest {

  private ObjectMapper mapper;

  @BeforeEach
  void setUp() {
    mapper = new ObjectMapper();
  }

  @Test
  void defaultValuesOnNoArgConstruction() {
    var param = new Parameter();

    assertThat(param.getKey()).isNull();
    assertThat(param.getValue()).isNull();
  }

  @Test
  void constructorWithKeySetsKey() {
    var param = new Parameter("myKey");

    assertThat(param.getKey()).isEqualTo("myKey");
    assertThat(param.getValue()).isNull();
  }

  @Test
  void fluentSettersReturnSameInstance() {
    var param = new Parameter();

    assertThat(param.key("k")).isSameAs(param);
    assertThat(param.value("v")).isSameAs(param);
  }

  @Test
  void fluentSettersStoreValues() {
    var param = new Parameter().key("myKey").value("myValue");

    assertThat(param.getKey()).isEqualTo("myKey");
    assertThat(param.getValue()).isEqualTo("myValue");
  }

  @Test
  void nullValueIsAccepted() {
    var param = new Parameter().key("k").value(null);

    assertThat(param.getValue()).isNull();
  }

  @Test
  void equalObjectsHaveSameHashCode() {
    var a = new Parameter().key("k").value("v");
    var b = new Parameter().key("k").value("v");

    assertThat(a).isEqualTo(b).hasSameHashCodeAs(b);
  }

  @Test
  void toStringContainsFieldValues() {
    var param = new Parameter().key("k").value("v");

    assertThat(param.toString()).contains("k", "v");
  }

  @Test
  void serializesFieldNamesCorrectly() {
    var param = new Parameter().key("myKey").value("myValue");

    var json = mapper.writeValueAsString(param);

    assertThat(json).isEqualTo("""
      {"key":"myKey","value":"myValue"}""");
  }

  @Test
  void serializesNullValueAsNullNode() {
    var param = new Parameter().key("k").value(null);

    var json = mapper.writeValueAsString(param);

    assertThat(json).contains("""
      {"key":"k","value":null}""");
  }

  @Test
  void deserializesFromJson() {
    var json = """
      {
        "key": "myKey",
        "value": "myValue"
      }
      """;

    var param = mapper.readValue(json, Parameter.class);

    assertThat(param.getKey()).isEqualTo("myKey");
    assertThat(param.getValue()).isEqualTo("myValue");
  }

  @Test
  void deserializesNullValueField() {
    var json = """
      {
        "key": "k",
        "value": null
      }
      """;

    var param = mapper.readValue(json, Parameter.class);

    assertThat(param.getKey()).isEqualTo("k");
    assertThat(param.getValue()).isNull();
  }

  @Test
  void roundTripPreservesValues() {
    var original = new Parameter().key("k").value("v");

    var param = mapper.readValue(mapper.writeValueAsString(original), Parameter.class);

    assertThat(param).isEqualTo(original);
  }
}
