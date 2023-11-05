package org.folio.spring.service;

import javax.validation.constraints.NotEmpty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Data
@Validated
@NoArgsConstructor
@AllArgsConstructor
@ConfigurationProperties("folio.system-user")
public final class SystemUserProperties {

  /**
   * Defines if system-user functionality is enabled or not.
   */
  private boolean enabled = true;

  /**
   * System user username.
   */
  private String username;

  /**
   * System user password.
   */
  @NotEmpty(message = "system user password must be configured to be non-empty")
  private String password;

  /**
   * System user lastname.
   */
  private String lastname;

  /**
   * Path to the system user permissions CSV file.
   */
  private String permissionsFilePath;

  /**
   * Constructor for 4 arguments: username, password, lastname and permissionsFilePath.
   *
   * @param username - system user username
   * @param password - system user password
   * @param lastname - system user lastname
   * @param permissionsFilePath - path to the system user permissions CSV file
   */
  public SystemUserProperties(String username, String password, String lastname, String permissionsFilePath) {
    if (StringUtils.isEmpty(password)) {
      throw new IllegalArgumentException("system user password must be configured to be non-empty");
    }

    this.username = username;
    this.password = password;
    this.lastname = lastname;
    this.permissionsFilePath = permissionsFilePath;
  }

  /**
   * Returns system user username.
   *
   * @return system user username as {@link String}
   */
  public String username() {
    return username;
  }

  /**
   * Returns system user password.
   *
   * @return system user password as {@link String}
   */
  public String password() {
    return password;
  }

  /**
   * Returns system user lastname.
   *
   * @return system user lastname as {@link String}
   */
  public String lastname() {
    return lastname;
  }

  /**
   * Returns path to the system user permissions CSV file.
   *
   * @return path to the system user permissions CSV file as {@link String}.
   */
  public String permissionsFilePath() {
    return permissionsFilePath;
  }
}
