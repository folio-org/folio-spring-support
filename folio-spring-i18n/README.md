# Documentation for folio-spring-i18n features

- [Introduction](#introduction)
- [Adding this module to your project](#adding-this-module-to-your-project)
- [Using translations in your code](#using-translations-in-your-code)
  - [`String format(String key, Object... args)`](#string-formatstring-key-object-args)
    - [Example](#example)
  - [`String formatList(Collection<?> list)`](#string-formatlistcollection-list)
    - [Example](#example-1)

## Introduction

Translations may be performed in backend modules using this library, per [this TC decision](https://wiki.folio.org/x/SqTc). These translations work the same as UI modules:

- Translations are stored in JSON files like `/translations/mod-foo/en_ca.json` in the code repository. The path is required for the automated exchange of translation files to and from FOLIO's translation tool [Lokalise](https://lokalise.org/).
- This `/translations/` directory must be loaded as a resource and packaged with the application. The path is required by folio-spring-i18n.
- Each JSON file is named for a locale and region, e.g. `en_ca.json`.
- The `en.json` file is the source of truth and fallback; other translation files will be generated and tracked separately by Lokalise.
- Translation keys are prepended with the module name, e.g. `mod-foo.title`.

This library provides a number of features, including:

- Support for LocalDate and LocalTime formatting (use of time zones/offsets are not supported, as there is no way for clients to provide this information)
- Support for list formatting
- Automatic fallbacks based on languages and server locale (`zh_CN` -> `zh` -> `en` -> server locale)
- Full ICU token support

See https://wiki.folio.org/display/I18N/How+to+Translate+FOLIO for more information on how translations are contributed and tracked.

## Adding this module to your project

To add this module to your project, add the following to your `pom.xml` in `<dependencies>`:

```xml
<dependency>
  <groupId>org.folio</groupId>
  <artifactId>folio-spring-i18n</artifactId>
  <version>${folio-spring-base.version}</version>
</dependency>
```

Then, add the following to your `pom.xml` in `<build>`:

```xml
  <resources>
    <resource>
      <directory>src/main/resources</directory>
    </resource>
    <resource>
      <directory>${project.basedir}/translations</directory>
      <targetPath>translations</targetPath>
    </resource>
  </resources>
```

Now, create a folder `/translations/mod-name-here` and you're good to go!

## Using translations in your code

To use this library, inject an instance of `TranslationService` into your class (e.g. by autowiring). This class provides the main interface to the library. Then, the following two methods can be invoked:

### `String format(String key, Object... args)`

This method will format a given translation key with optional arguments. Arguments should be provided in pairs (key, value).

#### Example

With `/translations/mod-foo/en.json` as follows:

```json
{
  "simple": "Hello, world!",
  "complex": "Hello, {name}! You have {count, plural, one {# item} other {# items}}."
}
```

The `TranslationService` will produce:

```java
translationService.format("mod-foo.simple"); // "Hello, world!"
translationService.format("mod-foo.complex", "name", "Bob", "count", 1); // "Hello, Bob! You have 1 item."
translationService.format("mod-foo.complex", "name", "Bob", "count", 2); // "Hello, Bob! You have 2 items."
```

### `String formatList(Collection<?> list)`

This method will format a list of items in the current locale.

#### Example

```java
translationService.formatList("A"); // A"
translationService.formatList("A", "B"); // "A and B"
translationService.formatList("A", "B", "C"); // "A, B, and C"
```
