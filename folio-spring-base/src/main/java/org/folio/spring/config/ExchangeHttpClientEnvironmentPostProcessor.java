package org.folio.spring.config;

import static org.springframework.core.env.StandardEnvironment.SYSTEM_ENVIRONMENT_PROPERTY_SOURCE_NAME;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import org.jspecify.annotations.NullMarked;
import org.springframework.boot.EnvironmentPostProcessor;
import org.springframework.boot.SpringApplication;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MapPropertySource;

/**
 * Maps {@code FOLIO_EXCHANGE_HTTP_*} environment variables to {@code folio.exchange.http-client.*} properties,
 * e.g. {@code FOLIO_EXCHANGE_HTTP_MAX_CONNECTIONS_PER_ROUTE} to
 * {@code folio.exchange.http-client.max-connections-per-route}.
 *
 * <p>The mapped values are placed right after the system environment property source, so they override values
 * from module configuration files but not command line arguments or system properties.</p>
 */
@NullMarked
public class ExchangeHttpClientEnvironmentPostProcessor implements EnvironmentPostProcessor {

  static final String PROPERTY_SOURCE_NAME = "folioExchangeHttpClientEnvironment";
  static final String ENV_PREFIX = "FOLIO_EXCHANGE_HTTP_";
  static final String PROPERTY_PREFIX = "folio.exchange.http-client.";
  static final List<String> PROPERTIES = List.of(
    "max-connections-per-route",
    "max-connections-total",
    "pool-concurrency-policy",
    "pool-reuse-policy",
    "connect-timeout",
    "socket-timeout",
    "connection-time-to-live",
    "validate-after-inactivity",
    "connection-request-timeout",
    "response-timeout",
    "evict-idle-connections-after",
    "automatic-retries-enabled");

  @Override
  public void postProcessEnvironment(ConfigurableEnvironment environment, SpringApplication application) {
    Map<String, Object> properties = new LinkedHashMap<>();
    for (var property : PROPERTIES) {
      var value = environment.getProperty(toEnvVariableName(property));
      if (value != null) {
        properties.put(PROPERTY_PREFIX + property, value);
      }
    }
    if (properties.isEmpty()) {
      return;
    }

    var propertySource = new MapPropertySource(PROPERTY_SOURCE_NAME, properties);
    var propertySources = environment.getPropertySources();
    if (propertySources.contains(SYSTEM_ENVIRONMENT_PROPERTY_SOURCE_NAME)) {
      propertySources.addAfter(SYSTEM_ENVIRONMENT_PROPERTY_SOURCE_NAME, propertySource);
    } else {
      propertySources.addLast(propertySource);
    }
  }

  static String toEnvVariableName(String property) {
    return ENV_PREFIX + property.replace('-', '_').toUpperCase(Locale.ROOT);
  }
}
