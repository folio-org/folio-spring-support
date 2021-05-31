package org.folio.spring.cql;

import static org.assertj.core.api.Assertions.assertThat;

import io.zonky.test.db.AutoConfigureEmbeddedDatabase;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.flyway.FlywayAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.test.context.jdbc.Sql;

import org.folio.spring.cql.domain.City;
import org.folio.spring.cql.domain.CityRepository;
import org.folio.spring.cql.domain.Person;
import org.folio.spring.cql.domain.PersonRepository;
import org.folio.spring.data.OffsetRequest;

@SpringBootTest
@AutoConfigureEmbeddedDatabase(beanName = "dataSource")
@EnableAutoConfiguration(exclude = FlywayAutoConfiguration.class)
@Sql({"/schema.sql", "/insert-data.sql"})
class JpaCQLExecutorTest {

  @Autowired
  private PersonRepository personRepository;

  @Autowired
  private CityRepository cityRepository;

  @Configuration
  static class TestConfiguration {

  }

  @Test
  void testTypesOfRepositories() {
    assertThat(personRepository).isInstanceOf(JpaCqlRepository.class);
    assertThat(cityRepository).isInstanceOf(JpaRepository.class);
  }

  @Test
  void testSelectAllRecordsWithSort() {
    var page = personRepository.findByCQL("(cql.allRecords=1)sortby age/sort.ascending", OffsetRequest.of(0, 10));
    assertThat(page)
      .hasSize(3)
      .extracting(Person::getAge)
      .startsWith(20)
      .endsWith(40);
  }

  @Test
  void testSelectAllRecordsWithAsterisks() {
    var page = personRepository.findByCQL("name=* sortby age/sort.ascending", OffsetRequest.of(0, 10));
    assertThat(page)
      .hasSize(3)
      .extracting(Person::getAge)
      .startsWith(20)
      .endsWith(40);
  }

  @Test
  void testSelectAllRecordsWithSortAndPagination() {
    var page1 = personRepository.findByCQL("(cql.allRecords=1)sortby age/sort.descending", OffsetRequest.of(0, 1));
    var page2 = personRepository.findByCQL("(cql.allRecords=1)sortby age/sort.descending", OffsetRequest.of(1, 1));
    var page3 = personRepository.findByCQL("(cql.allRecords=1)sortby age/sort.descending", OffsetRequest.of(2, 1));
    var page4 = personRepository.findByCQL("(cql.allRecords=1)sortby age/sort.descending", OffsetRequest.of(3, 1));
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
    var page = personRepository.findByCQL("name=Jane", OffsetRequest.of(0, 10));
    assertThat(page)
      .hasSize(1)
      .extracting(Person::getName)
      .contains("Jane");
  }

  @Test
  void testSelectAllRecordsByNameAndAge() {
    var page = personRepository.findByCQL("name=John and age>20", OffsetRequest.of(0, 10));
    assertThat(page)
      .hasSize(2)
      .extracting(Person::getName)
      .contains("John");
  }

  @Test
  void testSelectAllRecordsByNameOrAge() {
    var page = personRepository.findByCQL("name=John or age<21", OffsetRequest.of(0, 10));
    assertThat(page)
      .hasSize(3)
      .extracting(Person::getAge)
      .startsWith(20)
      .endsWith(40);
  }

  @Test
  void testSelectAllRecordsByNameNot() {
    var page = personRepository.findByCQL("cql.allRecords=1 NOT age>=22", OffsetRequest.of(0, 10));
    assertThat(page)
      .hasSize(1)
      .extracting(Person::getAge)
      .contains(20);
  }

  @Test
  void testSelectAllRecordsByCityNameEquals() {
    var page = personRepository.findByCQL("city.name==Kyiv", OffsetRequest.of(0, 10));
    assertThat(page)
      .hasSize(1)
      .extracting(Person::getCity)
      .extracting(City::getName)
      .contains("Kyiv");
  }

  @Test
  void testInvalidQuery() {
    var offsetRequest = OffsetRequest.of(0, 10);
    Assertions.assertThrows(CqlQueryValidationException.class,
      () -> personRepository.findByCQL("!!sortby name", offsetRequest)
    );
  }

  @Test
  void testUnsupportedFeatureQuery() {
    Assertions.assertThrows(CqlQueryValidationException.class,
      () -> personRepository.count("name prox Jon")
    );
  }
}