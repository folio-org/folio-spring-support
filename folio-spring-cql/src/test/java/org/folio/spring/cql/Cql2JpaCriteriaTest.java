package org.folio.spring.cql;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import jakarta.persistence.EntityManager;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Expression;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.Order;
import jakarta.persistence.criteria.Path;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import jakarta.persistence.metamodel.EntityType;
import jakarta.persistence.metamodel.PluralAttribute;
import jakarta.persistence.metamodel.Type;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Date;
import java.util.List;
import java.util.UUID;
import java.util.stream.Stream;
import org.folio.cql2pgjson.exception.QueryValidationException;
import org.folio.spring.testing.type.UnitTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.jpa.domain.Specification;

@UnitTest
@ExtendWith(MockitoExtension.class)
class Cql2JpaCriteriaTest {

  // "cql.unsupported" is valid CQL syntax but triggers CQLFeatureUnsupportedException during processing
  private static final String UNSUPPORTED_CQL = "cql.unsupported = *";
  private static final String UUID_VALUE = "aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa";
  private static final String DATE_RANGE = "2023-01-01:2023-12-31";
  private static final String DATE_TIME = "2023-06-15T10:30:00";

  @Mock
  private EntityManager entityManager;

  @Mock
  private CriteriaBuilder criteriaBuilder;

  @Mock
  private CriteriaQuery<Object> criteriaQuery;

  @Mock
  @SuppressWarnings("rawtypes")
  private CriteriaQuery countCriteriaQuery;

  @Mock
  private Root<Object> root;

  @Mock
  private Predicate predicate;

  // --- toCollectCriteria(String) ---

  @Test
  void toCollectCriteria_negative_unsupportedCqlField_throwsCqlQueryValidationException() {
    // Arrange: mock enough of the JPA chain for CQL parsing to succeed and processing to fail
    var criteria = new Cql2JpaCriteria<>(Object.class, entityManager);
    when(entityManager.getCriteriaBuilder()).thenReturn(criteriaBuilder);
    when(criteriaBuilder.createQuery(Object.class)).thenReturn(criteriaQuery);
    when(criteriaQuery.from(Object.class)).thenReturn(root);

    // Act + Assert
    assertThatThrownBy(() -> criteria.toCollectCriteria(UNSUPPORTED_CQL))
      .isInstanceOf(CqlQueryValidationException.class)
      .hasMessageContaining("cql.unsupported");
  }

  // --- toCountCriteria(String) ---

  @Test
  @SuppressWarnings("unchecked")
  void toCountCriteria_negative_unsupportedCqlField_throwsCqlQueryValidationException() {
    var criteria = new Cql2JpaCriteria<>(Object.class, entityManager);
    when(entityManager.getCriteriaBuilder()).thenReturn(criteriaBuilder);
    when(criteriaBuilder.createQuery(Long.class)).thenReturn(countCriteriaQuery);
    when(countCriteriaQuery.from(Object.class)).thenReturn(root);

    assertThatThrownBy(() -> criteria.toCountCriteria(UNSUPPORTED_CQL))
      .isInstanceOf(CqlQueryValidationException.class)
      .hasMessageContaining("cql.unsupported");
  }

  // --- createCollectSpecification ---

  @Test
  void createCollectSpecification_negative_unsupportedCqlField_throwsCqlQueryValidationException() {
    var criteria = new Cql2JpaCriteria<>(Object.class, entityManager);
    var spec = criteria.createCollectSpecification(UNSUPPORTED_CQL);

    assertThatThrownBy(() -> spec.toPredicate(root, criteriaQuery, criteriaBuilder))
      .isInstanceOf(CqlQueryValidationException.class)
      .hasMessageContaining("cql.unsupported");
  }

  // --- createCountSpecification ---

  @Test
  @SuppressWarnings("unchecked")
  void createCountSpecification_negative_unsupportedCqlField_throwsCqlQueryValidationException() {
    var criteria = new Cql2JpaCriteria<>(Object.class, entityManager);
    var spec = criteria.createCountSpecification(UNSUPPORTED_CQL);
    when(criteriaBuilder.createQuery(Long.class)).thenReturn(countCriteriaQuery);

    assertThatThrownBy(() -> spec.toPredicate(root, criteriaQuery, criteriaBuilder))
      .isInstanceOf(CqlQueryValidationException.class)
      .hasMessageContaining("cql.unsupported");
  }

  // --- queryBySql: type handling ---

  @Test
  @SuppressWarnings({"unchecked", "rawtypes"})
  void queryBySql_positive_numberType_equalComparator() throws QueryValidationException {
    var criteria = new Cql2JpaCriteria<>(Object.class, entityManager);
    Expression<Number> expression = mock(Expression.class);
    when(expression.getJavaType()).thenReturn((Class) Number.class);
    when(criteriaBuilder.equal(expression, 42)).thenReturn(predicate);

    var result = criteria.queryBySql(expression, "42", "=", criteriaBuilder);

    assertThat(result).isEqualTo(predicate);
  }

  @Test
  @SuppressWarnings({"unchecked", "rawtypes"})
  void queryBySql_positive_numberType_notEqualsComparator() throws QueryValidationException {
    var criteria = new Cql2JpaCriteria<>(Object.class, entityManager);
    Expression<Number> expression = mock(Expression.class);
    when(expression.getJavaType()).thenReturn((Class) Number.class);
    when(criteriaBuilder.notEqual(expression, 5)).thenReturn(predicate);

    var result = criteria.queryBySql(expression, "5", "<>", criteriaBuilder);

    assertThat(result).isEqualTo(predicate);
  }

  @Test
  @SuppressWarnings({"unchecked", "rawtypes"})
  void queryBySql_positive_uuidType_equalComparator() throws QueryValidationException {
    var criteria = new Cql2JpaCriteria<>(Object.class, entityManager);
    var uuid = UUID.fromString(UUID_VALUE);
    Expression<UUID> expression = mock(Expression.class);
    when(expression.getJavaType()).thenReturn((Class) UUID.class);
    when(criteriaBuilder.equal(expression, uuid)).thenReturn(predicate);

    var result = criteria.queryBySql(expression, UUID_VALUE, "=", criteriaBuilder);

    assertThat(result).isEqualTo(predicate);
  }

  @Test
  @SuppressWarnings({"unchecked", "rawtypes"})
  void queryBySql_positive_booleanType_equalComparator() throws QueryValidationException {
    var criteria = new Cql2JpaCriteria<>(Object.class, entityManager);
    Expression<Boolean> expression = mock(Expression.class);
    when(expression.getJavaType()).thenReturn((Class) Boolean.class);
    when(criteriaBuilder.equal(expression, Boolean.TRUE)).thenReturn(predicate);

    var result = criteria.queryBySql(expression, "true", "=", criteriaBuilder);

    assertThat(result).isEqualTo(predicate);
  }

  @Test
  @SuppressWarnings({"unchecked", "rawtypes"})
  void queryBySql_positive_dateType_dateRangeValue() throws QueryValidationException {
    var criteria = new Cql2JpaCriteria<>(Object.class, entityManager);
    Expression<Date> expression = mock(Expression.class);
    when(expression.getJavaType()).thenReturn((Class) Date.class);

    var zone = ZoneId.systemDefault();
    var expectedFrom = Date.from(LocalDate.parse("2023-01-01").atStartOfDay().atZone(zone).toInstant());
    var expectedTo = Date.from(LocalDate.parse("2023-12-31").atStartOfDay().atZone(zone).toInstant());
    var gePredicate = mock(Predicate.class);
    var ltPredicate = mock(Predicate.class);
    when(criteriaBuilder.greaterThanOrEqualTo(expression, expectedFrom)).thenReturn(gePredicate);
    when(criteriaBuilder.lessThan(expression, expectedTo)).thenReturn(ltPredicate);
    when(criteriaBuilder.and(gePredicate, ltPredicate)).thenReturn(predicate);

    var result = criteria.queryBySql(expression, DATE_RANGE, "=", criteriaBuilder);

    assertThat(result).isEqualTo(predicate);
  }

  @Test
  @SuppressWarnings({"unchecked", "rawtypes"})
  void queryBySql_positive_dateType_dateTimeValue() throws QueryValidationException {
    var criteria = new Cql2JpaCriteria<>(Object.class, entityManager);
    Expression<Date> expression = mock(Expression.class);
    when(expression.getJavaType()).thenReturn((Class) Date.class);

    var expectedDate = Date.from(LocalDateTime.parse(DATE_TIME).atZone(ZoneId.systemDefault()).toInstant());
    when(criteriaBuilder.equal(expression, expectedDate)).thenReturn(predicate);

    var result = criteria.queryBySql(expression, DATE_TIME, "=", criteriaBuilder);

    assertThat(result).isEqualTo(predicate);
  }

  @Test
  @SuppressWarnings({"unchecked", "rawtypes"})
  void queryBySql_positive_timestampType_dateRangeValue() throws QueryValidationException {
    var criteria = new Cql2JpaCriteria<>(Object.class, entityManager);
    Expression<Timestamp> expression = mock(Expression.class);
    when(expression.getJavaType()).thenReturn((Class) Timestamp.class);

    var zone = ZoneId.systemDefault();
    var expectedFrom = Date.from(LocalDate.parse("2023-01-01").atStartOfDay().atZone(zone).toInstant());
    var expectedTo = Date.from(LocalDate.parse("2023-12-31").atStartOfDay().atZone(zone).toInstant());
    var gePredicate = mock(Predicate.class);
    var ltPredicate = mock(Predicate.class);
    when(criteriaBuilder.greaterThanOrEqualTo(expression, expectedFrom)).thenReturn(gePredicate);
    when(criteriaBuilder.lessThan(expression, expectedTo)).thenReturn(ltPredicate);
    when(criteriaBuilder.and(gePredicate, ltPredicate)).thenReturn(predicate);

    var result = criteria.queryBySql(expression, DATE_RANGE, "=", criteriaBuilder);

    assertThat(result).isEqualTo(predicate);
  }

  @Test
  @SuppressWarnings({"unchecked", "rawtypes"})
  void queryBySql_positive_localDateTimeType_dateRangeValue() throws QueryValidationException {
    var criteria = new Cql2JpaCriteria<>(Object.class, entityManager);
    Expression<LocalDateTime> expression = mock(Expression.class);
    when(expression.getJavaType()).thenReturn((Class) LocalDateTime.class);

    var expectedFrom = LocalDate.parse("2023-01-01").atStartOfDay();
    var expectedTo = LocalDate.parse("2023-12-31").atStartOfDay();
    var gePredicate = mock(Predicate.class);
    var ltPredicate = mock(Predicate.class);
    when(criteriaBuilder.greaterThanOrEqualTo(expression, expectedFrom)).thenReturn(gePredicate);
    when(criteriaBuilder.lessThan(expression, expectedTo)).thenReturn(ltPredicate);
    when(criteriaBuilder.and(gePredicate, ltPredicate)).thenReturn(predicate);

    var result = criteria.queryBySql(expression, DATE_RANGE, "=", criteriaBuilder);

    assertThat(result).isEqualTo(predicate);
  }

  @Test
  @SuppressWarnings({"unchecked", "rawtypes"})
  void queryBySql_positive_localDateTimeType_dateTimeValue() throws QueryValidationException {
    var criteria = new Cql2JpaCriteria<>(Object.class, entityManager);
    Expression<LocalDateTime> expression = mock(Expression.class);
    when(expression.getJavaType()).thenReturn((Class) LocalDateTime.class);

    var expectedDateTime = LocalDateTime.parse(DATE_TIME);
    when(criteriaBuilder.equal(expression, expectedDateTime)).thenReturn(predicate);

    var result = criteria.queryBySql(expression, DATE_TIME, "=", criteriaBuilder);

    assertThat(result).isEqualTo(predicate);
  }

  @Test
  @SuppressWarnings({"unchecked", "rawtypes"})
  void queryBySql_positive_enumType_equalComparator() throws QueryValidationException {
    var criteria = new Cql2JpaCriteria<>(Object.class, entityManager);
    Expression<SampleEnum> enumExpression = mock(Expression.class);
    Expression<String> textExpression = mock(Expression.class);
    when(enumExpression.getJavaType()).thenReturn((Class) SampleEnum.class);
    when(criteriaBuilder.function("text", String.class, enumExpression)).thenReturn(textExpression);
    when(criteriaBuilder.equal(textExpression, "VALUE_A")).thenReturn(predicate);

    var result = criteria.queryBySql(enumExpression, "VALUE_A", "=", criteriaBuilder);

    assertThat(result).isEqualTo(predicate);
  }

  @Test
  @SuppressWarnings({"unchecked", "rawtypes"})
  void queryBySql_positive_stringType_equalComparator() throws QueryValidationException {
    // @RespectAccents + @RespectCase → identity wrapper (no cb.lower / f_unaccent calls)
    var criteria = new Cql2JpaCriteria<>(RespectCaseAndAccentsEntity.class, entityManager);
    Expression<String> expression = mock(Expression.class);
    Expression<String> literalExpression = mock(Expression.class);
    when(expression.getJavaType()).thenReturn((Class) String.class);
    when(criteriaBuilder.literal("hello")).thenReturn(literalExpression);
    when(criteriaBuilder.equal(expression, literalExpression)).thenReturn(predicate);

    var result = criteria.queryBySql(expression, "hello", "=", criteriaBuilder);

    assertThat(result).isEqualTo(predicate);
  }

  @Test
  @SuppressWarnings({"unchecked", "rawtypes"})
  void queryBySql_positive_stringType_notEqualsComparator() throws QueryValidationException {
    var criteria = new Cql2JpaCriteria<>(RespectCaseAndAccentsEntity.class, entityManager);
    Expression<String> expression = mock(Expression.class);
    Expression<String> literalExpression = mock(Expression.class);
    when(expression.getJavaType()).thenReturn((Class) String.class);
    when(criteriaBuilder.literal("world")).thenReturn(literalExpression);
    when(criteriaBuilder.notEqual(expression, literalExpression)).thenReturn(predicate);

    var result = criteria.queryBySql(expression, "world", "<>", criteriaBuilder);

    assertThat(result).isEqualTo(predicate);
  }

  // --- queryBySql: unsupported operator ---

  @ParameterizedTest
  @MethodSource("unsupportedOperators")
  @SuppressWarnings({"unchecked", "rawtypes"})
  void queryBySql_negative_unsupportedOperator_throwsQueryValidationException(String operator) {
    var criteria = new Cql2JpaCriteria<>(Object.class, entityManager);
    Expression<Number> expression = mock(Expression.class);
    when(expression.getJavaType()).thenReturn((Class) Number.class);

    assertThatThrownBy(() -> criteria.queryBySql(expression, "42", operator, criteriaBuilder))
      .isInstanceOf(QueryValidationException.class)
      .hasMessageContaining("Unsupported operator '" + operator + "'");
  }

  private static Stream<String> unsupportedOperators() {
    return Stream.of("~", "%", "prox", "within");
  }

  // --- queryBySql: toPredicate(Expression, Comparable) missing operators ---

  @Test
  @SuppressWarnings({"unchecked", "rawtypes"})
  void queryBySql_positive_dateType_greaterThanComparator() throws QueryValidationException {
    var criteria = new Cql2JpaCriteria<>(Object.class, entityManager);
    Expression<Date> expression = mock(Expression.class);
    when(expression.getJavaType()).thenReturn((Class) Date.class);

    var expectedDate = Date.from(LocalDateTime.parse(DATE_TIME).atZone(ZoneId.systemDefault()).toInstant());
    when(criteriaBuilder.greaterThan(expression, expectedDate)).thenReturn(predicate);

    var result = criteria.queryBySql(expression, DATE_TIME, ">", criteriaBuilder);

    assertThat(result).isEqualTo(predicate);
  }

  @Test
  @SuppressWarnings({"unchecked", "rawtypes"})
  void queryBySql_positive_dateType_lessThanOrEqualComparator() throws QueryValidationException {
    var criteria = new Cql2JpaCriteria<>(Object.class, entityManager);
    Expression<Date> expression = mock(Expression.class);
    when(expression.getJavaType()).thenReturn((Class) Date.class);

    var expectedDate = Date.from(LocalDateTime.parse(DATE_TIME).atZone(ZoneId.systemDefault()).toInstant());
    when(criteriaBuilder.lessThanOrEqualTo(expression, expectedDate)).thenReturn(predicate);

    var result = criteria.queryBySql(expression, DATE_TIME, "<=", criteriaBuilder);

    assertThat(result).isEqualTo(predicate);
  }

  // --- queryBySql: toPredicate(Expression, Expression) missing operators ---

  @Test
  @SuppressWarnings({"unchecked", "rawtypes"})
  void queryBySql_positive_stringType_greaterThanComparator() throws QueryValidationException {
    var criteria = new Cql2JpaCriteria<>(RespectCaseAndAccentsEntity.class, entityManager);
    Expression<String> expression = mock(Expression.class);
    Expression<String> literalExpression = mock(Expression.class);
    when(expression.getJavaType()).thenReturn((Class) String.class);
    when(criteriaBuilder.literal("hello")).thenReturn(literalExpression);
    when(criteriaBuilder.greaterThan(expression, literalExpression)).thenReturn(predicate);

    var result = criteria.queryBySql(expression, "hello", ">", criteriaBuilder);

    assertThat(result).isEqualTo(predicate);
  }

  @Test
  @SuppressWarnings({"unchecked", "rawtypes"})
  void queryBySql_positive_stringType_lessThanComparator() throws QueryValidationException {
    var criteria = new Cql2JpaCriteria<>(RespectCaseAndAccentsEntity.class, entityManager);
    Expression<String> expression = mock(Expression.class);
    Expression<String> literalExpression = mock(Expression.class);
    when(expression.getJavaType()).thenReturn((Class) String.class);
    when(criteriaBuilder.literal("hello")).thenReturn(literalExpression);
    when(criteriaBuilder.lessThan(expression, literalExpression)).thenReturn(predicate);

    var result = criteria.queryBySql(expression, "hello", "<", criteriaBuilder);

    assertThat(result).isEqualTo(predicate);
  }

  @Test
  @SuppressWarnings({"unchecked", "rawtypes"})
  void queryBySql_positive_stringType_greaterThanOrEqualComparator() throws QueryValidationException {
    var criteria = new Cql2JpaCriteria<>(RespectCaseAndAccentsEntity.class, entityManager);
    Expression<String> expression = mock(Expression.class);
    Expression<String> literalExpression = mock(Expression.class);
    when(expression.getJavaType()).thenReturn((Class) String.class);
    when(criteriaBuilder.literal("hello")).thenReturn(literalExpression);
    when(criteriaBuilder.greaterThanOrEqualTo(expression, literalExpression)).thenReturn(predicate);

    var result = criteria.queryBySql(expression, "hello", ">=", criteriaBuilder);

    assertThat(result).isEqualTo(predicate);
  }

  @Test
  @SuppressWarnings({"unchecked", "rawtypes"})
  void queryBySql_positive_stringType_lessThanOrEqualComparator() throws QueryValidationException {
    var criteria = new Cql2JpaCriteria<>(RespectCaseAndAccentsEntity.class, entityManager);
    Expression<String> expression = mock(Expression.class);
    Expression<String> literalExpression = mock(Expression.class);
    when(expression.getJavaType()).thenReturn((Class) String.class);
    when(criteriaBuilder.literal("hello")).thenReturn(literalExpression);
    when(criteriaBuilder.lessThanOrEqualTo(expression, literalExpression)).thenReturn(predicate);

    var result = criteria.queryBySql(expression, "hello", "<=", criteriaBuilder);

    assertThat(result).isEqualTo(predicate);
  }

  @Test
  @SuppressWarnings({"unchecked", "rawtypes"})
  void queryBySql_positive_stringType_doubleEqualComparator() throws QueryValidationException {
    var criteria = new Cql2JpaCriteria<>(RespectCaseAndAccentsEntity.class, entityManager);
    Expression<String> expression = mock(Expression.class);
    Expression<String> literalExpression = mock(Expression.class);
    when(expression.getJavaType()).thenReturn((Class) String.class);
    when(criteriaBuilder.literal("hello")).thenReturn(literalExpression);
    when(criteriaBuilder.equal(expression, literalExpression)).thenReturn(predicate);

    var result = criteria.queryBySql(expression, "hello", "==", criteriaBuilder);

    assertThat(result).isEqualTo(predicate);
  }

  // --- queryBySql: wrapper variations ---

  @Test
  @SuppressWarnings({"unchecked", "rawtypes"})
  void queryBySql_positive_stringType_respectAccentsOnlyAnnotation_usesLowerWrapper() throws QueryValidationException {
    // @RespectAccents only → wrapper = cb::lower
    var criteria = new Cql2JpaCriteria<>(RespectAccentsOnlyEntity.class, entityManager);
    Expression<String> expression = mock(Expression.class);
    Expression<String> loweredField = mock(Expression.class);
    Expression<String> literalExpression = mock(Expression.class);
    Expression<String> loweredTerm = mock(Expression.class);
    when(expression.getJavaType()).thenReturn((Class) String.class);
    when(criteriaBuilder.lower(expression)).thenReturn(loweredField);
    when(criteriaBuilder.literal("test")).thenReturn(literalExpression);
    when(criteriaBuilder.lower(literalExpression)).thenReturn(loweredTerm);
    when(criteriaBuilder.equal(loweredField, loweredTerm)).thenReturn(predicate);

    var result = criteria.queryBySql(expression, "test", "=", criteriaBuilder);

    assertThat(result).isEqualTo(predicate);
  }

  @Test
  @SuppressWarnings({"unchecked", "rawtypes"})
  void queryBySql_positive_stringType_respectCaseOnlyAnnotation_usesUnaccentWrapper() throws QueryValidationException {
    // @RespectCase only → wrapper = f_unaccent
    var criteria = new Cql2JpaCriteria<>(RespectCaseOnlyEntity.class, entityManager);
    Expression<String> expression = mock(Expression.class);
    Expression<String> unaccentedField = mock(Expression.class);
    Expression<String> literalExpression = mock(Expression.class);
    Expression<String> unaccentedTerm = mock(Expression.class);
    when(expression.getJavaType()).thenReturn((Class) String.class);
    when(criteriaBuilder.function("f_unaccent", String.class, expression)).thenReturn(unaccentedField);
    when(criteriaBuilder.literal("test")).thenReturn(literalExpression);
    when(criteriaBuilder.function("f_unaccent", String.class, literalExpression)).thenReturn(unaccentedTerm);
    when(criteriaBuilder.equal(unaccentedField, unaccentedTerm)).thenReturn(predicate);

    var result = criteria.queryBySql(expression, "test", "=", criteriaBuilder);

    assertThat(result).isEqualTo(predicate);
  }

  @Test
  @SuppressWarnings({"unchecked", "rawtypes"})
  void queryBySql_positive_stringType_noAnnotations_usesLowerUnaccentWrapper() throws QueryValidationException {
    // No annotations → wrapper = lower(f_unaccent)
    var criteria = new Cql2JpaCriteria<>(NoAnnotationEntity.class, entityManager);
    Expression<String> expression = mock(Expression.class);
    Expression<String> unaccentedField = mock(Expression.class);
    Expression<String> loweredField = mock(Expression.class);
    Expression<String> literalExpression = mock(Expression.class);
    Expression<String> unaccentedTerm = mock(Expression.class);
    Expression<String> loweredTerm = mock(Expression.class);
    when(expression.getJavaType()).thenReturn((Class) String.class);
    when(criteriaBuilder.function("f_unaccent", String.class, expression)).thenReturn(unaccentedField);
    when(criteriaBuilder.lower(unaccentedField)).thenReturn(loweredField);
    when(criteriaBuilder.literal("test")).thenReturn(literalExpression);
    when(criteriaBuilder.function("f_unaccent", String.class, literalExpression)).thenReturn(unaccentedTerm);
    when(criteriaBuilder.lower(unaccentedTerm)).thenReturn(loweredTerm);
    when(criteriaBuilder.equal(loweredField, loweredTerm)).thenReturn(predicate);

    var result = criteria.queryBySql(expression, "test", "=", criteriaBuilder);

    assertThat(result).isEqualTo(predicate);
  }

  // --- toCollectCriteria(Specification) / toCountCriteria(Specification) ---

  @Test
  void toCollectCriteria_positive_withSpecification_returnsCriteriaQuery() {
    Specification<Object> specification = (r, q, cb) -> predicate;
    when(entityManager.getCriteriaBuilder()).thenReturn(criteriaBuilder);
    when(criteriaBuilder.createQuery(Object.class)).thenReturn(criteriaQuery);
    when(criteriaQuery.from(Object.class)).thenReturn(root);

    var result = new Cql2JpaCriteria<>(Object.class, entityManager).toCollectCriteria(specification);

    assertThat(result).isEqualTo(criteriaQuery);
    verify(criteriaQuery).where(predicate);
  }

  @Test
  @SuppressWarnings("unchecked")
  void toCountCriteria_positive_withSpecification_returnsCriteriaQuery() {
    Specification<Object> specification = (r, q, cb) -> predicate;
    when(entityManager.getCriteriaBuilder()).thenReturn(criteriaBuilder);
    when(criteriaBuilder.createQuery(Long.class)).thenReturn(countCriteriaQuery);
    when(countCriteriaQuery.from(Object.class)).thenReturn(root);

    var result = new Cql2JpaCriteria<>(Object.class, entityManager).toCountCriteria(specification);

    assertThat(result).isEqualTo(countCriteriaQuery);
    verify(countCriteriaQuery).where(predicate);
  }

  // --- Boolean operations via createCollectSpecification ---

  @Test
  void createCollectSpecification_positive_cqlAndNode_returnsAndPredicate() {
    var conjPredicate = mock(Predicate.class);
    var andPredicate = mock(Predicate.class);
    when(criteriaBuilder.and()).thenReturn(conjPredicate);
    when(criteriaBuilder.and(conjPredicate, conjPredicate)).thenReturn(andPredicate);

    var spec = new Cql2JpaCriteria<>(Object.class, entityManager)
      .createCollectSpecification("cql.allRecords = 1 AND cql.allRecords = 1");
    var result = spec.toPredicate(root, criteriaQuery, criteriaBuilder);

    assertThat(result).isEqualTo(andPredicate);
  }

  @Test
  void createCollectSpecification_positive_cqlOrNode_returnsOrPredicate() {
    var conjPredicate = mock(Predicate.class);
    var orPredicate = mock(Predicate.class);
    when(criteriaBuilder.and()).thenReturn(conjPredicate);
    when(criteriaBuilder.or(conjPredicate, conjPredicate)).thenReturn(orPredicate);

    var spec = new Cql2JpaCriteria<>(Object.class, entityManager)
      .createCollectSpecification("cql.allRecords = 1 OR cql.allRecords = 1");
    var result = spec.toPredicate(root, criteriaQuery, criteriaBuilder);

    assertThat(result).isEqualTo(orPredicate);
  }

  @Test
  void createCollectSpecification_positive_cqlNotNode_returnsAndNotPredicate() {
    var conjPredicate = mock(Predicate.class);
    var notPredicate = mock(Predicate.class);
    var andPredicate = mock(Predicate.class);
    when(criteriaBuilder.and()).thenReturn(conjPredicate);
    when(criteriaBuilder.not(conjPredicate)).thenReturn(notPredicate);
    when(criteriaBuilder.and(conjPredicate, notPredicate)).thenReturn(andPredicate);

    var spec = new Cql2JpaCriteria<>(Object.class, entityManager)
      .createCollectSpecification("cql.allRecords = 1 NOT cql.allRecords = 1");
    var result = spec.toPredicate(root, criteriaQuery, criteriaBuilder);

    assertThat(result).isEqualTo(andPredicate);
  }

  @Test
  void createCollectSpecification_positive_orNodeRightIsWildcard_simplifiestoLeftPredicate() {
    // Special case: right operand is "field = *" → simplify to just left operand
    var conjPredicate = mock(Predicate.class);
    when(criteriaBuilder.and()).thenReturn(conjPredicate);

    var spec = new Cql2JpaCriteria<>(Object.class, entityManager)
      .createCollectSpecification("cql.allRecords = 1 OR foo = *");
    var result = spec.toPredicate(root, criteriaQuery, criteriaBuilder);

    assertThat(result).isEqualTo(conjPredicate);
  }

  // --- Sort via createCollectSpecification ---

  @Test
  @SuppressWarnings("rawtypes")
  void createCollectSpecification_positive_withSortAscending_appliesAscOrder() {
    var path = mock(Path.class);
    var ascOrder = mock(Order.class);
    var conjPredicate = mock(Predicate.class);
    when(root.get("name")).thenReturn(path);
    when(criteriaBuilder.asc(path)).thenReturn(ascOrder);
    when(criteriaBuilder.and()).thenReturn(conjPredicate);

    var spec = new Cql2JpaCriteria<>(Object.class, entityManager)
      .createCollectSpecification("(cql.allRecords = 1) sortby name/sort.ascending");
    var result = spec.toPredicate(root, criteriaQuery, criteriaBuilder);

    assertThat(result).isEqualTo(conjPredicate);
    verify(criteriaQuery).orderBy(List.of(ascOrder));
  }

  @Test
  @SuppressWarnings("rawtypes")
  void createCollectSpecification_positive_withSortDescending_appliesDescOrder() {
    var path = mock(Path.class);
    var descOrder = mock(Order.class);
    var conjPredicate = mock(Predicate.class);
    when(root.get("name")).thenReturn(path);
    when(criteriaBuilder.desc(path)).thenReturn(descOrder);
    when(criteriaBuilder.and()).thenReturn(conjPredicate);

    var spec = new Cql2JpaCriteria<>(Object.class, entityManager)
      .createCollectSpecification("(cql.allRecords = 1) sortby name/sort.descending");
    var result = spec.toPredicate(root, criteriaQuery, criteriaBuilder);

    assertThat(result).isEqualTo(conjPredicate);
    verify(criteriaQuery).orderBy(List.of(descOrder));
  }

  // --- Field processing via createCollectSpecification ---

  @Test
  @SuppressWarnings({"unchecked", "rawtypes"})
  void createCollectSpecification_positive_stringFieldWithEquals_usesLikeQuery() {
    EntityType entityType = mock(EntityType.class);
    Path path = mock(Path.class);
    Expression<String> literalExpr = mock(Expression.class);
    when(root.getModel()).thenReturn(entityType);
    when(entityType.getAttribute("name")).thenThrow(new IllegalArgumentException());
    when(root.get("name")).thenReturn(path);
    when(path.getJavaType()).thenReturn((Class) String.class);
    when(criteriaBuilder.literal("hello")).thenReturn(literalExpr);
    when(criteriaBuilder.like(path, literalExpr, '\\')).thenReturn(predicate);

    var spec = new Cql2JpaCriteria<>(RespectCaseAndAccentsEntity.class, entityManager)
      .createCollectSpecification("name = hello");
    var result = spec.toPredicate((Root) root, (CriteriaQuery) criteriaQuery, criteriaBuilder);

    assertThat(result).isEqualTo(predicate);
  }

  @Test
  @SuppressWarnings({"unchecked", "rawtypes"})
  void createCollectSpecification_positive_stringFieldWithNotEquals_usesNotLikeQuery() {
    EntityType entityType = mock(EntityType.class);
    Path path = mock(Path.class);
    Expression<String> literalExpr = mock(Expression.class);
    when(root.getModel()).thenReturn(entityType);
    when(entityType.getAttribute("name")).thenThrow(new IllegalArgumentException());
    when(root.get("name")).thenReturn(path);
    when(path.getJavaType()).thenReturn((Class) String.class);
    when(criteriaBuilder.literal("hello")).thenReturn(literalExpr);
    when(criteriaBuilder.notLike(path, literalExpr, '\\')).thenReturn(predicate);

    var spec = new Cql2JpaCriteria<>(RespectCaseAndAccentsEntity.class, entityManager)
      .createCollectSpecification("name <> hello");
    var result = spec.toPredicate((Root) root, (CriteriaQuery) criteriaQuery, criteriaBuilder);

    assertThat(result).isEqualTo(predicate);
  }

  @Test
  @SuppressWarnings({"unchecked", "rawtypes"})
  void createCollectSpecification_positive_fieldWithEqualsEmptyTerm_returnsIsNotNull() {
    EntityType entityType = mock(EntityType.class);
    Path path = mock(Path.class);
    when(root.getModel()).thenReturn(entityType);
    when(entityType.getAttribute("name")).thenThrow(new IllegalArgumentException());
    when(root.get("name")).thenReturn(path);
    when(path.getJavaType()).thenReturn((Class) UUID.class);
    when(criteriaBuilder.isNotNull(path)).thenReturn(predicate);

    var spec = new Cql2JpaCriteria<>(Object.class, entityManager)
      .createCollectSpecification("name = \"\"");
    var result = spec.toPredicate(root, criteriaQuery, criteriaBuilder);

    assertThat(result).isEqualTo(predicate);
  }

  @Test
  @SuppressWarnings({"unchecked", "rawtypes"})
  void createCollectSpecification_positive_stringFieldWithDoubleEqualsEmptyTerm_returnsIsNotNullAndEmpty() {
    EntityType entityType = mock(EntityType.class);
    Path path = mock(Path.class);
    var notNullPredicate = mock(Predicate.class);
    var equalEmptyPredicate = mock(Predicate.class);
    when(root.getModel()).thenReturn(entityType);
    when(entityType.getAttribute("name")).thenThrow(new IllegalArgumentException());
    when(root.get("name")).thenReturn(path);
    when(path.getJavaType()).thenReturn((Class) String.class);
    when(criteriaBuilder.isNotNull(path)).thenReturn(notNullPredicate);
    when(criteriaBuilder.equal(path, "")).thenReturn(equalEmptyPredicate);
    when(criteriaBuilder.and(notNullPredicate, equalEmptyPredicate)).thenReturn(predicate);

    var spec = new Cql2JpaCriteria<>(Object.class, entityManager)
      .createCollectSpecification("name == \"\"");
    var result = spec.toPredicate(root, criteriaQuery, criteriaBuilder);

    assertThat(result).isEqualTo(predicate);
  }

  @Test
  @SuppressWarnings({"unchecked", "rawtypes"})
  void createCollectSpecification_positive_nestedFieldWithDot_usesLeftJoin() {
    Join join = mock(Join.class);
    Path joinPath = mock(Path.class);
    Expression<String> literalExpr = mock(Expression.class);
    when(root.join("city", JoinType.LEFT)).thenReturn(join);
    when(join.get("name")).thenReturn(joinPath);
    when(joinPath.getJavaType()).thenReturn((Class) String.class);
    when(criteriaBuilder.literal("Paris")).thenReturn(literalExpr);
    when(criteriaBuilder.like(joinPath, literalExpr, '\\')).thenReturn(predicate);

    var spec = new Cql2JpaCriteria<>(RespectCaseAndAccentsEntity.class, entityManager)
      .createCollectSpecification("city.name = Paris");
    var result = spec.toPredicate((Root) root, (CriteriaQuery) criteriaQuery, criteriaBuilder);

    assertThat(result).isEqualTo(predicate);
  }

  // --- buildCollectionMemberPredicate ---

  @Test
  @SuppressWarnings({"unchecked", "rawtypes"})
  void createCollectSpecification_positive_collectionUuidField_usesMemberPredicate() {
    var uuid = UUID.fromString(UUID_VALUE);
    EntityType entityType = mock(EntityType.class);
    PluralAttribute pluralAttr = mock(PluralAttribute.class);
    Type elementType = mock(Type.class);
    Path collectionPath = mock(Path.class);
    when(root.getModel()).thenReturn(entityType);
    when(entityType.getAttribute("memberIds")).thenReturn(pluralAttr);
    when(pluralAttr.getElementType()).thenReturn(elementType);
    when(elementType.getJavaType()).thenReturn((Class) UUID.class);
    doReturn(collectionPath).when(root).get("memberIds");
    when(criteriaBuilder.isMember(uuid, collectionPath)).thenReturn(predicate);

    var spec = new Cql2JpaCriteria<>(Object.class, entityManager)
      .createCollectSpecification("memberIds = " + UUID_VALUE);
    var result = spec.toPredicate(root, criteriaQuery, criteriaBuilder);

    assertThat(result).isEqualTo(predicate);
  }

  @Test
  @SuppressWarnings({"unchecked", "rawtypes"})
  void createCollectSpecification_positive_collectionUuidFieldNotEquals_usesIsNotMemberPredicate() {
    var uuid = UUID.fromString(UUID_VALUE);
    EntityType entityType = mock(EntityType.class);
    PluralAttribute pluralAttr = mock(PluralAttribute.class);
    Type elementType = mock(Type.class);
    Path collectionPath = mock(Path.class);
    when(root.getModel()).thenReturn(entityType);
    when(entityType.getAttribute("memberIds")).thenReturn(pluralAttr);
    when(pluralAttr.getElementType()).thenReturn(elementType);
    when(elementType.getJavaType()).thenReturn((Class) UUID.class);
    doReturn(collectionPath).when(root).get("memberIds");
    when(criteriaBuilder.isNotMember(uuid, collectionPath)).thenReturn(predicate);

    var spec = new Cql2JpaCriteria<>(Object.class, entityManager)
      .createCollectSpecification("memberIds <> " + UUID_VALUE);
    var result = spec.toPredicate(root, criteriaQuery, criteriaBuilder);

    assertThat(result).isEqualTo(predicate);
  }

  // --- Helper types ---

  @RespectAccents
  @RespectCase
  private static final class RespectCaseAndAccentsEntity {}

  @RespectAccents
  private static final class RespectAccentsOnlyEntity {}

  @RespectCase
  private static final class RespectCaseOnlyEntity {}

  private static final class NoAnnotationEntity {}

  private enum SampleEnum {
    VALUE_A, VALUE_B
  }
}
