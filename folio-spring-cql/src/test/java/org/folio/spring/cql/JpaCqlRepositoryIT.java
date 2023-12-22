package org.folio.spring.cql;

import static org.assertj.core.api.Assertions.assertThat;

import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.stream.Stream;
import org.folio.spring.cql.domain.City;
import org.folio.spring.cql.domain.Person;
import org.folio.spring.cql.domain.Str;
import org.folio.spring.cql.repo.CityRepository;
import org.folio.spring.cql.repo.PersonRepository;
import org.folio.spring.cql.repo.StrRepository;
import org.folio.spring.testing.extension.EnablePostgres;
import org.folio.spring.testing.type.IntegrationTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.jdbc.Sql;

@IntegrationTest
@SpringBootTest
@EnablePostgres
@ContextConfiguration(classes = JpaCqlConfiguration.class)
@EnableAutoConfiguration
@Sql({"/sql/jpa-cql-general-it-schema.sql", "/sql/jpa-cql-general-test-data.sql"})
class JpaCqlRepositoryIT {

  @Autowired
  private PersonRepository personRepository;

  @Autowired
  private CityRepository cityRepository;

  @Autowired
  private StrRepository strRepository;

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
    "/sql/jpa-cql-general-it-schema.sql",
    "/sql/jpa-cql-general-test-data.sql",
    "/sql/jpa-cql-person-test-data.sql"
  })
  void testSelectsWithAndWithoutDeletedCriteria() {
    var page1 = personRepository.findByCqlAndDeletedFalse("name=Jane", PageRequest.of(0, 10));
    var page2 = personRepository.findByCql("name=Jane", PageRequest.of(0, 10));
    assertThat(page1)
      .hasSize(2)
      .extracting(Person::getName)
      .contains("Jane");
    assertThat(page2)
      .hasSize(3)
      .extracting(Person::getName)
      .contains("Jane");

    page1 = personRepository.findByCqlAndDeletedFalse("name=John and age>20", PageRequest.of(0, 10));
    page2 = personRepository.findByCql("name=John and age>20", PageRequest.of(0, 10));
    assertThat(page1)
      .hasSize(3)
      .extracting(Person::getAge)
      .startsWith(22)
      .endsWith(30);
    assertThat(page2)
      .hasSize(4)
      .extracting(Person::getAge)
      .startsWith(22)
      .endsWith(33);

    assertThat(personRepository.countDeletedFalse("(cql.allRecords=1)sortby age/sort.ascending"))
      .isEqualTo(5);
    assertThat(personRepository.count("(cql.allRecords=1)sortby age/sort.ascending"))
      .isEqualTo(7);
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

  @Test
  void testSelectAllRecordsWithSortAndPagination() {
    var page1 = personRepository.findByCql("(cql.allRecords=1)sortby age/sort.descending", PageRequest.of(0, 1));
    var page2 = personRepository.findByCql("(cql.allRecords=1)sortby age/sort.descending", PageRequest.of(1, 1));
    var page3 = personRepository.findByCql("(cql.allRecords=1)sortby age/sort.descending", PageRequest.of(2, 1));
    var page4 = personRepository.findByCql("(cql.allRecords=1)sortby age/sort.descending", PageRequest.of(3, 1));
    assertThat(page1)
      .hasSize(1)
      .extracting(Person::getAge)
      .contains(40);
    assertThat(page2)
      .hasSize(1)
      .extracting(Person::getAge)
      .contains(22);
    assertThat(page3)
      .hasSize(1)
      .extracting(Person::getAge)
      .contains(20);
    assertThat(page4).isEmpty();
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
