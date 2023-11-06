package org.folio.spring.i18n;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;

import java.util.List;
import java.util.Locale;
import java.util.Map;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.io.support.ResourcePatternResolver;

@SpringBootTest
public class TranslationFileGetFilesTest {

  @Autowired
  ResourcePatternResolver resourceResolver;

  @Test
  void testSingleFileGetMap() {
    TranslationConfiguration translationConfiguration = new TranslationConfiguration(
      "/test-translations/test-normal/",
      Locale.ENGLISH
    );

    List<TranslationFile> files = TranslationFile.getAvailableTranslationFiles(
      resourceResolver,
      translationConfiguration
    );

    assertThat(
      "test-normal has exactly one TranslationFile",
      files,
      hasSize(1)
    );

    Map<String, String> map = files.get(0).getMap();

    assertThat(map.getOrDefault("foo", null), is("bar"));
  }
  // @Test
  // void testEmptyFileList() {
  //   translationConfiguration.setTranslationDirectory(
  //     "/test-translations/test-empty/"
  //   );

  //   assertThrows(
  //     IllegalStateException.class,
  //     () ->
  //       TranslationFile.getAvailableTranslationFiles(
  //         translationConfiguration,
  //         resourceResolver
  //       ),
  //     "No available files should result in an IllegalStateException"
  //   );
  // }

  // @Test
  // void testLanguageCountryMap() {
  //   translationConfiguration.setTranslationDirectory(
  //     "/test-translations/test-multiple/"
  //   );

  //   Map<String, Map<String, TranslationFile>> map = TranslationFile.buildLanguageCountryPatternMap(
  //     translationConfiguration,
  //     resourceResolver
  //   );

  //   assertThat(map.containsKey("en"), is(true));
  //   assertThat(map.get("en").containsKey("*"), is(true));
  //   assertThat(map.get("en").containsKey("ca"), is(true));
  //   assertThat(map.get("en").containsKey("us"), is(false));
  //   assertThat(
  //     map.get("en").get("*"),
  //     is(not(equalTo(map.get("en").get("ca"))))
  //   );
  //   assertThat(map.containsKey("es"), is(true));
  //   assertThat(map.get("es").containsKey("*"), is(true));
  //   assertThat(map.get("es").containsKey("mx"), is(false));
  //   assertThat(map.containsKey("fr"), is(true));
  //   assertThat(map.get("fr").containsKey("*"), is(true));
  //   assertThat(map.get("fr").containsKey("fr"), is(true));
  //   assertThat(map.get("fr").containsKey("zz"), is(false));
  //   assertThat(map.get("fr").get("*"), is(equalTo(map.get("fr").get("fr"))));
  //   assertThat(map.containsKey("zz"), is(false));
  // }
}
