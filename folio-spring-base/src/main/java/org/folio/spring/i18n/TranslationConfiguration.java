package org.folio.spring.i18n;

import java.util.Locale;
import lombok.Getter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

/**
 * An {@link org.springframework.context.annotation.Configuration Configuration} bean to hold the
 * location of the current translation directory.  By default, this is set from the application
 * property {@code folio.translation=directory} in your {@code application.properties} or
 * {@code application.yaml} file.  Note that the proceeding and trailing slashes are required.
 */
@Getter
@Configuration
public class TranslationConfiguration {

  private String translationDirectory;

  private Locale defaultLocale;

  @Autowired
  public TranslationConfiguration(
    @Value(
      "${folio.translation-directory:/translations/}"
    ) String translationDirectory,
    @Value("#{T(java.util.Locale).getDefault()}") Locale defaultLocale
  ) {
    this.translationDirectory = translationDirectory;
    this.defaultLocale = defaultLocale;
  }
}
