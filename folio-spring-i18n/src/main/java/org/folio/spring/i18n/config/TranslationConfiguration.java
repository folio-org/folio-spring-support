package org.folio.spring.i18n.config;

import java.util.List;
import java.util.Locale;
import lombok.Getter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.context.annotation.ComponentScan;

/**
 * An {@link org.springframework.context.annotation.Configuration Configuration} bean to hold the
 * location of the current translation directory and default fallback locale.
 */
@Getter
@AutoConfiguration
@ComponentScan(basePackages = "org.folio.spring.i18n.service")
public class TranslationConfiguration {

  private final List<String> translationDirectories;

  private final Locale fallbackLocale;

  @Autowired
  public TranslationConfiguration(
    @Value("#{'/translations/,/custom-translations/'.split(',')}") List<String> translationDirectories,
    @Value("#{T(java.util.Locale).ENGLISH}") Locale fallbackLocale
  ) {
    this.translationDirectories = translationDirectories;
    this.fallbackLocale = fallbackLocale;
  }

  // compatibility with versions prior to 10.0.0
  public TranslationConfiguration(String translationDirectory, Locale fallbackLocale) {
    this(List.of(translationDirectory), fallbackLocale);
  }
}
