package org.folio.tenant.domain.dto;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import org.folio.spring.testing.type.UnitTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.ObjectMapper;

@UnitTest
class TenantAttributesTest {

  private ObjectMapper mapper;

  @BeforeEach
  void setUp() {
    mapper = new ObjectMapper();
  }

  @Test
  void defaultValuesOnNoArgConstruction() {
    var attrs = new TenantAttributes();

    assertThat(attrs.getModuleFrom()).isNull();
    assertThat(attrs.getModuleTo()).isNull();
    assertThat(attrs.getPurge()).isTrue();
    assertThat(attrs.getParameters()).isEmpty();
  }

  @Test
  void fluentSettersReturnSameInstance() {
    var param = new Parameter().key("k").value("v");
    var attrs = new TenantAttributes();

    assertThat(attrs.moduleFrom("mod-1.0.0")).isSameAs(attrs);
    assertThat(attrs.moduleTo("mod-2.0.0")).isSameAs(attrs);
    assertThat(attrs.purge(false)).isSameAs(attrs);
    assertThat(attrs.parameters(List.of(param))).isSameAs(attrs);
  }

  @Test
  void fluentSettersStoreValues() {
    var param = new Parameter().key("k").value("v");
    var attrs = new TenantAttributes()
      .moduleFrom("mod-1.0.0")
      .moduleTo("mod-2.0.0")
      .purge(false)
      .parameters(List.of(param));

    assertThat(attrs.getModuleFrom()).isEqualTo("mod-1.0.0");
    assertThat(attrs.getModuleTo()).isEqualTo("mod-2.0.0");
    assertThat(attrs.getPurge()).isFalse();
    assertThat(attrs.getParameters()).containsExactly(param);
  }

  @Test
  void addParametersItemAppendsToList() {
    var first = new Parameter().key("a");
    var second = new Parameter().key("b");
    var attrs = new TenantAttributes()
      .addParametersItem(first)
      .addParametersItem(second);

    assertThat(attrs.getParameters()).containsExactly(first, second);
  }

  @Test
  void addParametersItemInitializesListWhenNull() {
    var attrs = new TenantAttributes();
    attrs.setParameters(null);

    var param = new Parameter().key("x");
    attrs.addParametersItem(param);

    assertThat(attrs.getParameters()).containsExactly(param);
  }

  @Test
  void equalObjectsHaveSameHashCode() {
    var a = new TenantAttributes().moduleFrom("m").moduleTo("n").purge(true);
    var b = new TenantAttributes().moduleFrom("m").moduleTo("n").purge(true);

    assertThat(a).isEqualTo(b).hasSameHashCodeAs(b);
  }

  @Test
  void toStringContainsFieldValues() {
    var attrs = new TenantAttributes().moduleFrom("mod-1.0.0").moduleTo("mod-2.0.0");

    assertThat(attrs.toString()).contains("mod-1.0.0", "mod-2.0.0");
  }

  @Test
  void nullModuleFromAndModuleToAreAccepted() {
    var attrs = new TenantAttributes().moduleFrom(null).moduleTo(null);

    assertThat(attrs.getModuleFrom()).isNull();
    assertThat(attrs.getModuleTo()).isNull();
  }

  @Test
  void serializesSnakeCaseFieldNames() {
    var attrs = new TenantAttributes().moduleFrom("mod-1.0.0").moduleTo("mod-2.0.0").purge(false);

    var json = mapper.writeValueAsString(attrs);

    assertThat(json)
      .isEqualTo("""
        {"module_from":"mod-1.0.0","module_to":"mod-2.0.0","parameters":[],"purge":false}""");
  }

  @Test
  void serializesParametersList() {
    var attrs = new TenantAttributes()
      .addParametersItem(new Parameter().key("k").value("v"));

    var json = mapper.writeValueAsString(attrs);

    assertThat(json).isEqualTo("""
      {"module_from":null,"module_to":null,"parameters":[{"key":"k","value":"v"}],"purge":true}""");
  }

  @Test
  void deserializesFromJson() {
    var json = """
      {
        "module_from": "mod-1.0.0",
        "module_to": "mod-2.0.0",
        "purge": false,
        "parameters": [
          {
            "key": "k",
            "value": "v"
          }
        ]
      }
      """;

    var attrs = mapper.readValue(json, TenantAttributes.class);

    assertThat(attrs.getModuleFrom()).isEqualTo("mod-1.0.0");
    assertThat(attrs.getModuleTo()).isEqualTo("mod-2.0.0");
    assertThat(attrs.getPurge()).isFalse();
    assertThat(attrs.getParameters()).hasSize(1);
    assertThat(attrs.getParameters().getFirst().getKey()).isEqualTo("k");
  }

  @Test
  void deserializesNullModuleFields() {
    var json = """
      {
        "module_from": null,
        "module_to": null
      }
      """;

    var attrs = mapper.readValue(json, TenantAttributes.class);

    assertThat(attrs.getModuleFrom()).isNull();
    assertThat(attrs.getModuleTo()).isNull();
  }

  @Test
  void roundTripPreservesValues() {
    var original = new TenantAttributes()
      .moduleFrom("mod-1.0.0")
      .moduleTo("mod-2.0.0")
      .purge(false)
      .addParametersItem(new Parameter().key("k").value("v"));

    var attrs = mapper.readValue(mapper.writeValueAsString(original), TenantAttributes.class);

    assertThat(attrs).isEqualTo(original);
  }
}
