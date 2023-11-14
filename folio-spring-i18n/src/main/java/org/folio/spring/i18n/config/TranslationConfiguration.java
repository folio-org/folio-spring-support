package org.folio.spring.i18n.config;

import java.util.Locale;
import lombok.Getter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

/**
 * An {@link org.springframework.context.annotation.Configuration Configuration} bean to hold the
 * location of the current translation directory and default fallback locale.
 */
@Getter
@Configuration
public class TranslationConfiguration {

  private String translationDirectory;

  private Locale fallbackLocale;

  @Autowired
  public TranslationConfiguration(
    @Value("/translations/") String translationDirectory,
    @Value("#{T(java.util.Locale).ENGLISH}") Locale fallbackLocale
  ) {
    this.translationDirectory = translationDirectory;
    this.fallbackLocale = fallbackLocale;
  }
}
