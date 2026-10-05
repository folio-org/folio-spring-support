package org.folio.spring.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.core.env.StandardEnvironment.SYSTEM_ENVIRONMENT_PROPERTY_SOURCE_NAME;

import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.Map;
import org.folio.spring.config.properties.ExchangeHttpClientProperties;
import org.folio.spring.testing.type.UnitTest;
import org.junit.jupiter.api.Test;
import org.springframework.boot.SpringApplication;
import org.springframework.core.env.MapPropertySource;
import org.springframework.core.env.StandardEnvironment;
import org.springframework.core.env.SystemEnvironmentPropertySource;

@UnitTest
class ExchangeHttpClientEnvironmentPostProcessorTest {

  private final ExchangeHttpClientEnvironmentPostProcessor postProcessor =
    new ExchangeHttpClientEnvironmentPostProcessor();

  @Test
  void postProcessEnvironment_positive_mapsEnvVariablesWithPrecedenceOverConfigFiles() {
    var environment = environmentWith(Map.of(
      "FOLIO_EXCHANGE_HTTP_MAX_CONNECTIONS_PER_ROUTE", "200",
      "FOLIO_EXCHANGE_HTTP_RESPONSE_TIMEOUT", "30s"));
    environment.getPropertySources().addLast(new MapPropertySource("applicationConfig",
      Map.of("folio.exchange.http-client.max-connections-per-route", "10")));

    postProcessor.postProcessEnvironment(environment, new SpringApplication());

    assertThat(environment.getProperty("folio.exchange.http-client.max-connections-per-route")).isEqualTo("200");
    assertThat(environment.getProperty("folio.exchange.http-client.response-timeout")).isEqualTo("30s");
    assertThat(environment.getProperty("folio.exchange.http-client.connect-timeout")).isNull();
  }

  @Test
  void postProcessEnvironment_positive_noPropertySourceWhenNoEnvVariables() {
    var environment = environmentWith(Map.of());

    postProcessor.postProcessEnvironment(environment, new SpringApplication());

    assertThat(environment.getPropertySources()
      .contains(ExchangeHttpClientEnvironmentPostProcessor.PROPERTY_SOURCE_NAME)).isFalse();
  }

  @Test
  void toEnvVariableName_positive() {
    assertThat(ExchangeHttpClientEnvironmentPostProcessor.toEnvVariableName("connection-time-to-live"))
      .isEqualTo("FOLIO_EXCHANGE_HTTP_CONNECTION_TIME_TO_LIVE");
  }

  @Test
  void properties_positive_coverAllExchangeHttpClientProperties() {
    var fieldNames = Arrays.stream(ExchangeHttpClientProperties.class.getDeclaredFields())
      .filter(field -> !Modifier.isStatic(field.getModifiers()))
      .map(field -> field.getName().replaceAll("([A-Z])", "-$1").toLowerCase())
      .toList();

    assertThat(fieldNames).containsExactlyInAnyOrderElementsOf(ExchangeHttpClientEnvironmentPostProcessor.PROPERTIES);
  }

  private static StandardEnvironment environmentWith(Map<String, Object> envVariables) {
    var environment = new StandardEnvironment();
    environment.getPropertySources().replace(SYSTEM_ENVIRONMENT_PROPERTY_SOURCE_NAME,
      new SystemEnvironmentPropertySource(SYSTEM_ENVIRONMENT_PROPERTY_SOURCE_NAME, envVariables));
    return environment;
  }
}
