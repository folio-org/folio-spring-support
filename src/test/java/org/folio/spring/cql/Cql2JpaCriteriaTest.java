package org.folio.spring.cql;

import static org.junit.Assert.assertTrue;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.Persistence;

import lombok.Getter;
import org.hibernate.query.sqm.internal.QuerySqmImpl;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import org.folio.spring.cql.domain.Person;

class Cql2JpaCriteriaTest {

  private static EntityManagerFactory managerFactory;

  @BeforeAll
  static void beforeAll() {
    managerFactory = Persistence.createEntityManagerFactory("templatePU");
  }

  @ParameterizedTest
  @EnumSource(value = ValidCqlToJpaQuery.class)
  void testValidQueries(ValidCqlToJpaQuery testData) {
    String actualJpaQuery = getJpaQuery(testData.getCqlQuery());
    assertEquals(testData.getJpaQuery(), actualJpaQuery);
  }

  @ParameterizedTest
  @EnumSource(value = InvalidCqlToJpaQuery.class)
  void testInvalidQueries(InvalidCqlToJpaQuery testData) {
    var message =
      assertThrows(CqlQueryValidationException.class, () -> getJpaQuery(testData.getCqlQuery())).getMessage();
    assertTrue("Error should  contain 'Not implemented' or 'Unsupported modifier': " + message,
        message.contains("Not implemented") || message.contains("Unsupported modifier"));
  }

  private String getJpaQuery(String cqlQuery) {
    var em = managerFactory.createEntityManager();
    var cql2JpaCriteria = new Cql2JpaCriteria<>(Person.class, em);
    var criteria = cql2JpaCriteria.toCollectCriteria(cqlQuery);
    var hql = em.createQuery(criteria).unwrap(QuerySqmImpl.class).getSqmStatement().toHqlString();
    return hql.replaceAll("_[0-9]+|org.folio.spring.cql.domain.", "");
  }

  @Getter
  private enum InvalidCqlToJpaQuery {

    INVALID("invalid"),
    NOT_EXISTENT_TERM("age % 10"),
    NON_EXISTENT_MODIFIER("age =/modifier 10"),
    PREFIX_NODE("> n = name n=Ka"),
    PROX("name=Lea prox/unit=word/distance>3 name=Long");

    private final String cqlQuery;

    InvalidCqlToJpaQuery(String cqlQuery) {
      this.cqlQuery = cqlQuery;
    }
  }

  @Getter
  private enum ValidCqlToJpaQuery {

    EQ_FOR_STR("name=John", "select alias from Person alias where alias.name like John"),
    EQ_FOR_NUM("age=10", "select alias from Person alias where alias.age = 10"),
    EQ_FOR_UUID("identifier=11111111-1111-1111-1111-111111111111",
      "select alias from Person alias where alias.identifier = 11111111-1111-1111-1111-111111111111"),
    EQ_FOR_BOOL("isAlive=true",
      "select alias from Person alias where alias.isAlive = true"),
    EQ_EQ("name==\"*John*\"",
      "select alias from Person alias where alias.name like %John%"),
    ALL("name all \"Potter Harry\"",
      "select alias from Person alias where alias.name like Potter Harry"),
    ANY("name any \"Potter Harry\"",
      "select alias from Person alias where alias.name like Potter Harry"),
    ADJ("name adj \"Potter Harry\"",
      "select alias from Person alias where alias.name like Potter Harry"),
    LESS("age < 10", "select alias from Person alias where alias.age < 10"),
    LESS_EQ("age <= 10", "select alias from Person alias where alias.age <= 10"),
    GREATER("age > 10", "select alias from Person alias where alias.age > 10"),
    GREATER_EQ("age >= 10", "select alias from Person alias where alias.age >= 10"),
    NOT_EQ("age <> 10", "select alias from Person alias where alias.age != 10"),
    ALL_RECORDS("cql.allRecords=1", "select alias from Person alias where 1 = 1"),
    ALL_RECORDS_NOT("cql.allRecords=1 NOT age = 10",
      "select alias from Person alias where not (1 = 1 and alias.age = 10)"),
    ALL_RECORDS_WITH_1_SORT("cql.allRecords=1 sortby name",
      "select alias from Person alias where 1 = 1 order by alias.name"),
    ALL_RECORDS_WITH_2_SORT("cql.allRecords=1 sortby name age",
      "select alias from Person alias where 1 = 1 order by alias.name, alias.age"),
    ALL_RECORDS_WITH_SORT_ASC("cql.allRecords=1 sortby name/sort.ascending",
      "select alias from Person alias where 1 = 1 order by alias.name"),
    ALL_RECORDS_WITH_SORT_DESC("cql.allRecords=1 sortby name/sort.descending",
      "select alias from Person alias where 1 = 1 order by alias.name desc"),
    OR("name = John or age = 10",
      "select alias from Person alias where alias.name like John or alias.age = 10"),
    OR_WITH_ASTERISKS("name = John or age =*",
      "select alias from Person alias where alias.name like John"),
    AND("name = John and age = 10",
      "select alias from Person alias where alias.name like John and alias.age = 10"),
    JOIN("city.name = Kyiv",
      "select alias from Person alias left join alias.city alias join alias.city alias where alias.name like Kyiv");

    private final String cqlQuery;
    private final String jpaQuery;

    ValidCqlToJpaQuery(String cqlQuery, String jpaQuery) {
      this.cqlQuery = cqlQuery;
      this.jpaQuery = jpaQuery;
    }
  }
}