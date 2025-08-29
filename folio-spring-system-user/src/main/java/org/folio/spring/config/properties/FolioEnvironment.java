package org.folio.spring.config.properties;

import static java.lang.System.getProperty;
import static java.lang.System.getenv;
import static org.apache.commons.lang3.StringUtils.firstNonBlank;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.validation.annotation.Validated;

@Data
@Deprecated(since = "10.0.0", forRemoval = true)
@Validated
@Configuration
@NoArgsConstructor
@AllArgsConstructor(staticName = "of")
@ConfigurationProperties(prefix = "folio")
public class FolioEnvironment {

  @NotEmpty
  @Pattern(regexp = "[a-zA-Z0-9\\-_]+", message = "Value must follow the pattern: '[\\w0-9\\-_]+'")
  private String environment;

  private String okapiUrl;

  /**
   * Return folio env name from environment or system properties as {@link String} object.
   *
   * @return folio env name.
   */
  public static String getFolioEnvName() {
    return firstNonBlank(getenv("ENV"), getProperty("env"), getProperty("environment"), "folio");
  }
}
