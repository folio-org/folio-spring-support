package org.folio.spring.cql;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import javax.persistence.EntityManagerFactory;
import javax.persistence.Persistence;

import lombok.Getter;
import org.hibernate.query.Query;
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
    var exception =
      assertThrows(CqlQueryValidationException.class, () -> getJpaQuery(testData.getCqlQuery()));
    System.out.println(exception.getMessage());
  }

  private String getJpaQuery(String cqlQuery) {
    var em = managerFactory.createEntityManager();
    var cql2JpaCriteria = new Cql2JpaCriteria<>(Person.class, em);
    var criteria = cql2JpaCriteria.toCollectCriteria(cqlQuery);
    return em.createQuery(criteria).unwrap(Query.class).getQueryString();
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

    EQ_FOR_STR("name=John", "select generatedAlias0 from Person as generatedAlias0 where generatedAlias0.name like :param0"),
    EQ_FOR_NUM("age=10", "select generatedAlias0 from Person as generatedAlias0 where generatedAlias0.age=10"),
    EQ_FOR_UUID("identifier=11111111-1111-1111-1111-111111111111",
      "select generatedAlias0 from Person as generatedAlias0 where generatedAlias0.identifier=:param0"),
    EQ_FOR_BOOL("isAlive=true",
      "select generatedAlias0 from Person as generatedAlias0 where generatedAlias0.isAlive=:param0"),
    EQ_FOR_DATE("dateBorn=2020-12-12T00:00:00.000",
      "select generatedAlias0 from Person as generatedAlias0 where generatedAlias0.dateBorn=:param0"),
    EQ_EQ("name==\"*John*\"",
      "select generatedAlias0 from Person as generatedAlias0 where generatedAlias0.name like :param0"),
    ALL("name all \"Potter Harry\"",
      "select generatedAlias0 from Person as generatedAlias0 where generatedAlias0.name like :param0"),
    ANY("name any \"Potter Harry\"",
      "select generatedAlias0 from Person as generatedAlias0 where generatedAlias0.name like :param0"),
    ADJ("name adj \"Potter Harry\"",
      "select generatedAlias0 from Person as generatedAlias0 where generatedAlias0.name like :param0"),
    LESS("age < 10", "select generatedAlias0 from Person as generatedAlias0 where generatedAlias0.age<10"),
    LESS_EQ("age <= 10", "select generatedAlias0 from Person as generatedAlias0 where generatedAlias0.age<=10"),
    GREATER("age > 10", "select generatedAlias0 from Person as generatedAlias0 where generatedAlias0.age>10"),
    GREATER_EQ("age >= 10", "select generatedAlias0 from Person as generatedAlias0 where generatedAlias0.age>=10"),
    NOT_EQ("age <> 10", "select generatedAlias0 from Person as generatedAlias0 where generatedAlias0.age<>10"),
    ALL_RECORDS("cql.allRecords=1", "select generatedAlias0 from Person as generatedAlias0 where 1=1"),
    ALL_RECORDS_NOT("cql.allRecords=1 NOT age=10",
      "select generatedAlias0 from Person as generatedAlias0 where ( 0=1 ) or ( generatedAlias0.age<>10 )"),
    ALL_RECORDS_WITH_1_SORT("cql.allRecords=1 sortby name",
      "select generatedAlias0 from Person as generatedAlias0 where 1=1 order by generatedAlias0.name asc"),
    ALL_RECORDS_WITH_2_SORT("cql.allRecords=1 sortby name age",
      "select generatedAlias0 from Person as generatedAlias0 where 1=1 order by generatedAlias0.name asc, generatedAlias0.age asc"),
    ALL_RECORDS_WITH_SORT_ASC("cql.allRecords=1 sortby name/sort.ascending",
      "select generatedAlias0 from Person as generatedAlias0 where 1=1 order by generatedAlias0.name asc"),
    ALL_RECORDS_WITH_SORT_DESC("cql.allRecords=1 sortby name/sort.descending",
      "select generatedAlias0 from Person as generatedAlias0 where 1=1 order by generatedAlias0.name desc"),
    OR("name = John or age = 10",
      "select generatedAlias0 from Person as generatedAlias0 where ( generatedAlias0.name like :param0 ) or ( generatedAlias0.age=10 )"),
    OR_WITH_ASTERISKS("name = John or age =*",
      "select generatedAlias0 from Person as generatedAlias0 where generatedAlias0.name like :param0"),
    AND("name = John and age = 10",
      "select generatedAlias0 from Person as generatedAlias0 where ( generatedAlias0.name like :param0 ) and ( generatedAlias0.age=10 )"),
    JOIN("city.name = Kyiv",
      "select generatedAlias0 from Person as generatedAlias0 left join generatedAlias0.city as generatedAlias1 inner join fetch generatedAlias0.city as generatedAlias2 where generatedAlias1.name like :param0");

    private final String cqlQuery;
    private final String jpaQuery;

    ValidCqlToJpaQuery(String cqlQuery, String jpaQuery) {
      this.cqlQuery = cqlQuery;
      this.jpaQuery = jpaQuery;
    }
  }
}