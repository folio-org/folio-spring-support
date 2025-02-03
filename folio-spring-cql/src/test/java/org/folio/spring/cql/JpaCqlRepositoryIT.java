package org.folio.spring.cql;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.context.jdbc.SqlMergeMode.MergeMode.MERGE;

import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;
import org.folio.spring.cql.domain.City;
import org.folio.spring.cql.domain.Language;
import org.folio.spring.cql.domain.Person;
import org.folio.spring.cql.domain.Str;
import org.folio.spring.cql.repo.CityRepository;
import org.folio.spring.cql.repo.LanguageRepository;
import org.folio.spring.cql.repo.PersonRepository;
import org.folio.spring.cql.repo.StrRepository;
import org.folio.spring.testing.extension.EnablePostgres;
import org.folio.spring.testing.type.IntegrationTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.context.jdbc.SqlMergeMode;

@IntegrationTest
@SpringBootTest
@EnablePostgres
@ContextConfiguration(classes = JpaCqlConfiguration.class)
@EnableAutoConfiguration
@SqlMergeMode(MERGE)
@Sql({"/sql/jpa-cql-general-it-schema.sql", "/sql/jpa-cql-general-test-data.sql"})
class JpaCqlRepositoryIT {

  @Autowired
  private PersonRepository personRepository;

  @Autowired
  private CityRepository cityRepository;

  @Autowired
  private StrRepository strRepository;

  @Autowired
  private LanguageRepository languageRepository;

  @Test
  void testTypesOfRepositories() {
    assertThat(personRepository).isInstanceOf(JpaCqlRepository.class);
    assertThat(cityRepository).isInstanceOf(JpaCqlRepository.class);
    assertThat(strRepository).isInstanceOf(JpaCqlRepository.class);
  }

  @Test
  void testSelectAllRecordsWithSort() {
    var page = personRepository.findByCql("(cql.allRecords=1)sortby age/sort.ascending", PageRequest.of(0, 10));
    assertThat(page)
      .hasSize(3)
      .extracting(Person::getAge)
      .startsWith(20)
      .endsWith(40);
  }

  @Test
  @Sql({
    "/sql/jpa-cql-lang-ignore-case-test-data.sql"
  })
  void testFindByCqlIgnoreCase() {
    var page = languageRepository.findByCqlIgnoreCase("name=Java",
      PageRequest.of(0, 10), true);

    assertThat(page)
      .extracting(Language::getName)
      .contains("Java", "jAva", "JaVa", "javA")
      .hasSize(4);

    assertThat(languageRepository.countIgnoreCase("(cql.allRecords=1)sortby name/sort.ascending",
      true))
      .isEqualTo(4);
  }

  @Test
  @Sql({
    "/sql/jpa-cql-lang-ignore-case-test-data.sql"
  })
  void testFindByCql() {
    var page = languageRepository.findByCql("name=Java",
      PageRequest.of(0, 10));

    assertThat(page)
      .extracting(Language::getName)
      .contains("Java")
      .hasSize(1);

    assertThat(languageRepository.count("(cql.allRecords=1)sortby name/sort.ascending"))
      .isEqualTo(4);
  }

  @Test
  @Sql({
    "/sql/jpa-cql-person-test-data.sql"
  })
  void testSelectsWithAndWithoutDeletedCriteria() {
    var page = personRepository.findByCqlAndDeletedFalse("name=Jane", PageRequest.of(0, 10));
    assertThat(page)
      .hasSize(2)
      .extracting(Person::getName)
      .contains("Jane");

    page = personRepository.findByCql("name=Jane", PageRequest.of(0, 10));
    assertThat(page)
      .hasSize(3)
      .extracting(Person::getName)
      .contains("Jane");

    page = personRepository.findByCqlAndDeletedFalse("name=John and age>20", PageRequest.of(0, 10));
    assertThat(page)
      .hasSize(3)
      .extracting(Person::getAge)
      .startsWith(22)
      .endsWith(30);

    page = personRepository.findByCql("name=John and age>20", PageRequest.of(0, 10));
    assertThat(page)
      .hasSize(4)
      .extracting(Person::getAge)
      .startsWith(22)
      .endsWith(33);

    assertThat(personRepository.countDeletedFalse("(cql.allRecords=1)sortby age/sort.ascending"))
      .isEqualTo(5);
    assertThat(personRepository.count("(cql.allRecords=1)sortby age/sort.ascending"))
      .isEqualTo(9);
  }

  @Sql({
    "/sql/jpa-cql-person-test-data.sql"
  })
  @ParameterizedTest
  @CsvSource({
    "city=\"\", 8, John2, Jane;John",
    "cql.allRecords=1 NOT city=\"\", 1, Jane;John, John2",
    "age>30 NOT city=\"\", 1, Jane;John, John2",
    "name=John NOT city=\"\", 0, John2,",
    "city.id=\"\", 8, John2, Jane;John",
    "cql.allRecords=1 NOT city.id=\"\", 1, Jane;John, John2",
    "age=40 NOT city.id=\"\", 1, Jane;John, John2",
    "city.name=\"\", 8, John2, Jane;John",
    "identifier=\"\" NOT city.id=\"\", 1, Jane;John, John2",
    "name=\"\", 9, , Jane;John;John2",
    "city.name==\"\", 1, Jane;John;John2, Jane2",
    "age<45 NOT city.name==\"\", 8, Jane2, Jane;John;John2",
    "city.name=\"\" NOT city.name==\"\", 7, Jane2;John2, Jane;John",
    "name=\"\", 9, , Jane;John;John2",
    "name<>\"peter\", 9, , Jane;John;John2"
  })
  void testSelectAllRecordsByNonSpecifiedField(String query, int expectedSize, String excludedNames,
                                               String includedNames) {
    var expectedNames = Optional.ofNullable(includedNames).map(names -> names.split(";")).orElse(new String[0]);
    var notExpectedNames = Optional.ofNullable(excludedNames).map(names -> names.split(";")).orElse(new String[]{""});
    var page = personRepository.findByCql(query, PageRequest.of(0, 10));
    assertThat(page)
      .hasSize(expectedSize)
      .extracting(Person::getName)
      .contains(expectedNames)
      .doesNotContain(notExpectedNames);
  }

  @Test
  void testSelectWithFilterByCreatedDate() {
    var page = personRepository.findByCqlAndDeletedFalse("createdDate<=2021-12-26T12:00:00.0", PageRequest.of(0, 4));
    assertThat(page)
      .hasSize(2)
      .extracting(Person::getName)
      .contains("Jane", "John");

    page = personRepository.findByCqlAndDeletedFalse(
      "createdDate>2021-12-24T12:00:00.0 and createdDate<=2021-12-31T12:00:00.0", PageRequest.of(0, 4));
    assertThat(page)
      .hasSize(2)
      .extracting(Person::getName)
      .contains("John");
  }

  @Test
  void testSelectAllRecordsWithAsterisks() {
    var page = personRepository.findByCql("name=* sortby age/sort.ascending", PageRequest.of(0, 10));
    assertThat(page)
      .hasSize(3)
      .extracting(Person::getAge)
      .startsWith(20)
      .endsWith(40);
  }

  @ParameterizedTest
  @CsvSource({
    "0, 1, 40",
    "1, 1, 22",
    "2, 1, 20",
    "3, 0, -1"
  })
  void testSelectAllRecordsWithSortAndPagination(int pageNumber, int size, int age) {
    var page = personRepository.findByCql("(cql.allRecords=1)sortby age/sort.descending",
      PageRequest.of(pageNumber, 1));

    if (size == 0) {
      assertThat(page)
        .isEmpty();
      return;
    }

    assertThat(page)
      .hasSize(size)
      .extracting(Person::getAge)
      .contains(age);
  }

  @Test
  void testSelectAllRecordsByNameEquals() {
    var page = personRepository.findByCql("name=Jane", PageRequest.of(0, 10));
    assertThat(page)
      .hasSize(1)
      .extracting(Person::getName)
      .contains("Jane");
  }

  @Test
  void testSelectAllRecordsByNameAndAge() {
    var page = personRepository.findByCql("name=John and age>20", PageRequest.of(0, 10));
    assertThat(page)
      .hasSize(2)
      .extracting(Person::getName)
      .contains("John");
  }

  @Test
  void testSelectAllRecordsByNameOrAge() {
    var page = personRepository.findByCql("name=John or age<21", PageRequest.of(0, 10));
    assertThat(page)
      .hasSize(3)
      .extracting(Person::getAge)
      .startsWith(20)
      .endsWith(40);
  }

  @Test
  void testSelectAllRecordsByNameNot() {
    var page = personRepository.findByCql("cql.allRecords=1 NOT age>=22", PageRequest.of(0, 10));
    assertThat(page)
      .hasSize(1)
      .extracting(Person::getAge)
      .contains(20);
  }

  @Test
  void testSelectAllRecordsByCityIdWithLimit() {
    var page = personRepository.findByCql("city.id==2", PageRequest.of(0, 1));
    assertThat(page)
      .hasSize(1)
      .extracting(Person::getCity)
      .extracting(City::getId)
      .contains(2);
  }

  @Test
  void testSelectAllRecordsByCityIdLessThanOrEquals() {
    var page = personRepository.findByCql("(city.id<=2)sortby city/sort.descending", PageRequest.of(0, 2));
    assertThat(page)
      .hasSize(2)
      .extracting(Person::getCity)
      .extracting(City::getId)
      .startsWith(2)
      .endsWith(1);
  }

  @Test
  void testSelectAllRecordsByCityIdNotEquals() {
    var page = personRepository.findByCql("city.id<>2", PageRequest.of(0, 10));
    assertThat(page)
      .hasSize(2)
      .extracting(Person::getCity)
      .extracting(City::getId)
      .containsOnly(1);
  }

  @Test
  void testSelectRecordsByNameAndAsterisk() {
    var page = personRepository.findByCql("name=Jo*", PageRequest.of(0, 1));
    assertThat(page)
      .hasSize(1)
      .extracting(Person::getName)
      .contains("John");
  }

  @Test
  void testSelectAllRecordsByCityNameEquals() {
    var page = personRepository.findByCql("city.name==Kyiv", PageRequest.of(0, 10));
    assertThat(page)
      .hasSize(1)
      .extracting(Person::getCity)
      .extracting(City::getName)
      .contains("Kyiv");
  }

  @Test
  void testInvalidQuery() {
    var offsetRequest = PageRequest.of(0, 10);
    org.junit.jupiter.api.Assertions.assertThrows(CqlQueryValidationException.class,
      () -> personRepository.findByCql("!!sortby name", offsetRequest)
    );
  }

  @Test
  void testUnsupportedFeatureQuery() {
    org.junit.jupiter.api.Assertions.assertThrows(CqlQueryValidationException.class,
      () -> personRepository.count("name prox Jon")
    );
  }

  @Test
  void testWithUnsupportedQueryOperator() {
    var offsetRequest = PageRequest.of(0, 10);
    var thrown = org.junit.jupiter.api.Assertions.assertThrows(CqlQueryValidationException.class,
      () -> personRepository.findByCql("city.name%Kyiv", offsetRequest)
    );

    org.junit.jupiter.api.Assertions.assertTrue(thrown.getMessage().contains("Not implemented yet"));
  }

  @Test
  void testFilterByDates() {
    var page = personRepository.findByCql("dateBorn=2001-01-01:2001-01-03", PageRequest.of(0, 10));
    assertThat(page)
      .hasSize(2)
      .extracting(Person::getDateBorn)
      .contains(Timestamp.from(LocalDate.parse("2001-01-01").atStartOfDay().atZone(ZoneId.systemDefault()).toInstant()),
        Timestamp.from(LocalDate.parse("2001-01-02").atStartOfDay().atZone(ZoneId.systemDefault()).toInstant()));
  }

  @Test
  void testFilterByLocalDates() {
    var page = personRepository.findByCql("localDate=2001-01-01:2001-01-03", PageRequest.of(0, 10));
    assertThat(page)
      .hasSize(2)
      .extracting(Person::getLocalDate)
      .contains(LocalDate.parse("2001-01-01").atStartOfDay(),
        LocalDate.parse("2001-01-02").atStartOfDay());
  }


  static Stream<Arguments> testLikeMasking() {
    return Stream.of(
        Arguments.arguments("a", List.of("a")),
        Arguments.arguments("a?", List.of("ab")),
        Arguments.arguments("a*", List.of("a", "ab", "abc")),
        Arguments.arguments("\\*", List.of("*")),
        Arguments.arguments("\\?", List.of("?")),
        Arguments.arguments("%", List.of("%")),
        Arguments.arguments("_", List.of("_")),
        Arguments.arguments("\\\\", List.of("\\")),
        Arguments.arguments("'", List.of("'")),
        Arguments.arguments("\\\"", List.of("\""))
        );
  }

  @ParameterizedTest
  @MethodSource
  void testLikeMasking(String cql, List<String> expected) {
    var page = strRepository.findByCql("str==\"" + cql + "\" sortBy str", PageRequest.of(0, 100));
    assertThat(page)
      .extracting(Str::getStr)
      .containsExactlyElementsOf(expected);
  }

  static Stream<Arguments> testNotLikeMasking() {
    return Stream.of(
        Arguments.arguments("?", List.of("ab", "abc")),
        Arguments.arguments("_", List.of("a", "ab", "abc", "*", "?", "%", "\"", "'", "\\")),
        Arguments.arguments("'", List.of("a", "ab", "abc", "*", "?", "%", "_", "\"", "\\"))
        );
  }

  @ParameterizedTest
  @MethodSource
  void testNotLikeMasking(String cql, List<String> expected) {
    var page = strRepository.findByCql("str<>\"" + cql + "\"", PageRequest.of(0, 100));
    assertThat(page)
      .extracting(Str::getStr)
      .containsExactlyInAnyOrderElementsOf(expected);
  }

}
