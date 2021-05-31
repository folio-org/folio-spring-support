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
import org.springframework.test.context.jdbc.Sql;

import org.folio.spring.cql.domain.City;
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

  @Configuration
  static class TestConfiguration {
  }

  @Test
  public void testSelectAllRecordsWithSort() {
    var page = personRepository.findByCQL("(cql.allRecords=1)sortby age/sort.ascending", OffsetRequest.of(0, 10));
    assertThat(page)
      .hasSize(3)
      .extracting(Person::getAge)
      .startsWith(20)
      .endsWith(40);
  }

  @Test
  public void testSelectAllRecordsWithSortAndPagination() {
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
  public void testSelectAllRecordsByNameEquals() {
    var page = personRepository.findByCQL("name=Jane", OffsetRequest.of(0, 10));
    assertThat(page)
      .hasSize(1)
      .extracting(Person::getName)
      .contains("Jane");
  }

  @Test
  public void testSelectAllRecordsByNameAndAge() {
    var page = personRepository.findByCQL("name=John and age>20", OffsetRequest.of(0, 10));
    assertThat(page)
      .hasSize(2)
      .extracting(Person::getName)
      .contains("John");
  }

  @Test
  public void testSelectAllRecordsByCityNameEquals() {
    var page = personRepository.findByCQL("city.name=Kyiv", OffsetRequest.of(0, 10));
    assertThat(page)
      .hasSize(1)
      .extracting(Person::getCity)
      .extracting(City::getName)
      .contains("Kyiv");
  }

  @Test
  public void testInvalidQuery() {
    Assertions.assertThrows(CqlQueryValidationException.class,
      () -> personRepository.findByCQL("!!sortby name", OffsetRequest.of(0, 10))
    );
  }

  @Test
  public void testUnsupportedFeatureQuery() {
    Assertions.assertThrows(CqlQueryValidationException.class,
      () -> personRepository.findByCQL("name prox Jon", OffsetRequest.of(0, 10))
    );
  }
}