package org.folio.spring.cql;

import static java.lang.String.format;
import static org.apache.commons.lang3.StringUtils.equalsAny;
import static org.folio.cql2pgjson.model.CqlTermFormat.NUMBER;
import static org.springframework.util.CollectionUtils.isEmpty;

import jakarta.persistence.EntityManager;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Expression;
import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.Order;
import jakarta.persistence.criteria.Path;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import java.io.IOException;
import java.lang.reflect.Field;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.UUID;
import java.util.function.UnaryOperator;
import java.util.regex.Pattern;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.lang3.StringUtils;
import org.folio.cql2pgjson.exception.CQLFeatureUnsupportedException;
import org.folio.cql2pgjson.exception.QueryValidationException;
import org.folio.cql2pgjson.model.CqlModifiers;
import org.folio.cql2pgjson.model.CqlSort;
import org.folio.cql2pgjson.util.Cql2SqlUtil;
import org.springframework.data.jpa.domain.Specification;
import org.z3950.zing.cql.CQLAndNode;
import org.z3950.zing.cql.CQLBooleanNode;
import org.z3950.zing.cql.CQLNode;
import org.z3950.zing.cql.CQLNotNode;
import org.z3950.zing.cql.CQLOrNode;
import org.z3950.zing.cql.CQLParseException;
import org.z3950.zing.cql.CQLParser;
import org.z3950.zing.cql.CQLSortNode;
import org.z3950.zing.cql.CQLTermNode;
import org.z3950.zing.cql.Modifier;
import org.z3950.zing.cql.ModifierSet;

@Log4j2
public class Cql2JpaCriteria<E> {

  private static final String NOT_EQUALS_OPERATOR = "<>";
  private static final String ASTERISKS_SIGN = "*";
  private static final Pattern DATES_RANGE_PATTERN = Pattern.compile("\\d{4}(-\\d{2}){2}:\\d{4}(-\\d{2}){2}");

  private final Class<E> domainClass;
  private final EntityManager em;

  public Cql2JpaCriteria(Class<E> domainClass, EntityManager entityManager) {
    this.domainClass = domainClass;
    this.em = entityManager;
  }

  /**
   * Convert the CQL query into WHERE and the ORDER BY SQL clauses and return {@link CriteriaQuery} for selection.
   *
   * @param cql the query to convert
   * @return {@link CriteriaQuery} for selection
   */
  public CriteriaQuery<E> toCollectCriteria(String cql) {
    try {
      var node = new CQLParser().parse(cql);

      var cb = em.getCriteriaBuilder();
      var query = cb.createQuery(domainClass);
      var root = query.from(domainClass);
      var predicate = createPredicate(node, root, cb, query);

      query.where(predicate);
      return query;
    } catch (IOException | QueryValidationException | CQLParseException e) {
      throw new CqlQueryValidationException(e);
    }
  }

  /**
   * Create {@link CriteriaQuery} for collect/select query utilizing the given {@link Specification} into WHERE.
   *
   * @param specification specification describing the criteria for collect query
   * @return {@link CriteriaQuery} for collect
   */
  public CriteriaQuery<E> toCollectCriteria(Specification<E> specification) {
    var cb = em.getCriteriaBuilder();
    var query = cb.createQuery(domainClass);
    var root = query.from(domainClass);

    query.where(specification.toPredicate(root, query, cb));

    return query;
  }

  /**
   * Convert the CQL query into WHERE and the ORDER BY SQL clauses and return {@link CriteriaQuery} for count.
   *
   * @param cql the query to convert
   * @return {@link CriteriaQuery} for count
   */
  public CriteriaQuery<Long> toCountCriteria(String cql) {
    try {
      var node = new CQLParser().parse(cql);

      var cb = em.getCriteriaBuilder();
      var query = cb.createQuery(Long.class);
      var root = query.from(domainClass);
      query.select(cb.count(root));
      var predicate = createPredicate(node, root, cb, query);
      query.orderBy(Collections.emptyList());
      root.getFetches().clear();
      query.where(predicate);
      return query;
    } catch (IOException | CQLParseException | QueryValidationException e) {
      throw new CqlQueryValidationException(e);
    }
  }

  /**
   * Create {@link CriteriaQuery} for count utilizing the given {@link Specification} into WHERE.
   *
   * @param specification specification describing the criteria for count query
   * @return {@link CriteriaQuery} for count
   */
  public CriteriaQuery<Long> toCountCriteria(Specification<E> specification) {
    var cb = em.getCriteriaBuilder();
    var query = cb.createQuery(Long.class);
    var root = query.from(domainClass);
    query.select(cb.count(root));
    query.orderBy(Collections.emptyList());
    root.getFetches().clear();
    query.where(specification.toPredicate(root, query, cb));
    return query;
  }

  /**
   * Create collect criteria specification which can be used with other specifications in order to create
   * more complex ones.
   *
   * @param cql query string
   * @return {@link Specification} describing the criteria
   */
  public Specification<E> createCollectSpecification(String cql) {
    return (root, query, criteriaBuilder) -> {
      try {
        var node = new CQLParser().parse(cql);
        return createPredicate(node, root, criteriaBuilder, query);
      } catch (IOException | CQLParseException | QueryValidationException e) {
        throw new CqlQueryValidationException(e);
      }
    };
  }

  /**
   * Create count criteria specification which can be used with other specifications in order to create
   * more complex ones.
   *
   * @param cql query string
   * @return {@link Specification} describing the criteria
   */
  public Specification<E> createCountSpecification(String cql) {
    return (root, query, criteriaBuilder) -> {
      try {
        var node = new CQLParser().parse(cql);
        return createPredicate(node, root, criteriaBuilder, criteriaBuilder.createQuery(Long.class));
      } catch (IOException | CQLParseException | QueryValidationException e) {
        throw new CqlQueryValidationException(e);
      }
    };
  }

  private <T> Predicate createPredicate(CQLNode node, Root<E> root, CriteriaBuilder cb, CriteriaQuery<T> query)
    throws QueryValidationException {
    Predicate predicates;
    if (node instanceof CQLSortNode sortNode) {
      var orders = toOrders(sortNode, root, cb);
      query.orderBy(orders);
      predicates = process(sortNode.getSubtree(), cb, root, query);
    } else {
      predicates = process(node, cb, root, query);
    }
    return predicates;
  }

  private List<Order> toOrders(CQLSortNode node, Root<E> root, CriteriaBuilder cb)
    throws CQLFeatureUnsupportedException {
    List<Order> orders = new ArrayList<>();

    for (ModifierSet sortIndex : node.getSortIndexes()) {
      var modifiers = new CqlModifiers(sortIndex);
      orders.add(
        CqlSort.DESCENDING.equals(modifiers.getCqlSort())
          ? cb.desc(root.get(sortIndex.getBase()))
          : cb.asc(root.get(sortIndex.getBase())));
    }
    return orders;
  }

  private Predicate process(CQLNode node, CriteriaBuilder cb, Root<E> root, CriteriaQuery<?> query)
    throws QueryValidationException {
    switch (node) {
      case CQLTermNode cqlTermNode -> {
        return processTerm(cqlTermNode, cb, root, query);
      }
      case CQLBooleanNode cqlBooleanNode -> {
        return processBoolean(cqlBooleanNode, cb, root, query);
      }
      default -> throw createUnsupportedException(node);
    }
  }

  private Predicate processBoolean(CQLBooleanNode node, CriteriaBuilder cb, Root<E> root, CriteriaQuery<?> query)
    throws QueryValidationException {
    switch (node) {
      case CQLAndNode cqlAndNode -> {
        return cb.and(process(cqlAndNode.getLeftOperand(), cb, root, query),
          process(cqlAndNode.getRightOperand(), cb, root, query));
      }
      case CQLOrNode cqlOrNode -> {
        if (cqlOrNode.getRightOperand().getClass() == CQLTermNode.class) {
          // special case for the query the UI uses most often, before the user has
          // typed in anything: title=* OR contributors*= OR identifier=*
          var rightOperand = (CQLTermNode) (cqlOrNode.getRightOperand());
          if (ASTERISKS_SIGN.equals(rightOperand.getTerm()) && "=".equals(rightOperand.getRelation().getBase())
              && isEmpty(rightOperand.getRelation().getModifiers())) {
            log.debug("pgFT(): Simplifying =* OR =* ");
            return process(cqlOrNode.getLeftOperand(), cb, root, query);
          }
        }
        return cb.or(process(cqlOrNode.getLeftOperand(), cb, root, query),
          process(cqlOrNode.getRightOperand(), cb, root, query));
      }
      case CQLNotNode cqlNotNode -> {
        return
          cb.and(process(cqlNotNode.getLeftOperand(), cb, root, query),
            cb.not(process(cqlNotNode.getRightOperand(), cb, root, query)));
      }
      default -> throw createUnsupportedException(node);
    }
  }

  private Predicate processTerm(CQLTermNode node, CriteriaBuilder cb, Root<E> root, CriteriaQuery<?> query)
    throws QueryValidationException {
    var fieldName = node.getIndex();
    if (StringUtils.startsWithIgnoreCase(fieldName, "cql")) {
      if ("cql.allRecords".equalsIgnoreCase(fieldName)) {
        return cb.and();
      } else {
        throw createUnsupportedException(node);
      }
    }

    var field = getPath(node, root);
    var cqlModifiers = new CqlModifiers(node);
    if (!isEmpty(cqlModifiers.getRelationModifiers())) {
      query.distinct(true);
    }
    return indexNode(field, node, cqlModifiers, cb);
  }

  private Path<?> getPath(CQLTermNode node, Root<E> root) {
    var fieldName = node.getIndex();
    if (fieldName.contains(".")) {
      final var dotIdx = fieldName.indexOf(".");
      final var attributeName = fieldName.substring(0, dotIdx);
      var children = root.join(attributeName, JoinType.LEFT);
      return children.get(fieldName.substring(dotIdx + 1));
    } else if (!isEmpty(node.getRelation().getModifiers())) {
      return root.join(fieldName, JoinType.INNER);
    } else {
      return root.get(fieldName);
    }
  }

  private static <G extends Comparable<? super G>> Predicate toPredicate(Expression<G> field, G value,
      String comparator, CriteriaBuilder cb) throws QueryValidationException {

    return switch (comparator) {
      case ">" -> cb.greaterThan(field, value);
      case "<" -> cb.lessThan(field, value);
      case ">=" -> cb.greaterThanOrEqualTo(field, value);
      case "<=" -> cb.lessThanOrEqualTo(field, value);
      case "==", "=" -> cb.equal(field, value);
      case NOT_EQUALS_OPERATOR -> cb.notEqual(field, value);
      default -> throw unsupportedOperatorException(comparator);
    };
  }

  private static <G extends Comparable<? super G>> Predicate toPredicate(Expression<G> field, Expression<G> value,
      String comparator, CriteriaBuilder cb) throws QueryValidationException {

    return switch (comparator) {
      case ">" -> cb.greaterThan(field, value);
      case "<" -> cb.lessThan(field, value);
      case ">=" -> cb.greaterThanOrEqualTo(field, value);
      case "<=" -> cb.lessThanOrEqualTo(field, value);
      case "==", "=" -> cb.equal(field, value);
      case NOT_EQUALS_OPERATOR -> cb.notEqual(field, value);
      default -> throw unsupportedOperatorException(comparator);
    };
  }

  private static QueryValidationException unsupportedOperatorException(String operator) {
    return new QueryValidationException(
        "CQL: Unsupported operator '"
            + operator
            + "', "
            + " only supports '=', '==', and '<>' (possibly with right truncation)");
  }

  private Predicate indexNode(Path<?> field, CQLTermNode node, CqlModifiers modifiers,
                              CriteriaBuilder cb) throws QueryValidationException {

    if (!isEmpty(modifiers.getRelationModifiers())) {
      return buildModifiersQuery(field, modifiers, cb);
    }

    var comparator = node.getRelation().getBase().toLowerCase();
    var term = node.getTerm();

    if (equalsAny(comparator, "=", "==") && StringUtils.isEmpty(term)) {
      return definedOrDefinedAndEmptyQuery(field, comparator, cb);
    }

    return switch (comparator) {
      case "=" -> modifiers.getCqlTermFormat() == NUMBER
        ? queryBySql(field, term, comparator, cb)
        : buildQuery(field, term, comparator, cb);
      case "adj", "all", "any", "==", NOT_EQUALS_OPERATOR -> buildQuery(field, term, comparator, cb);
      case "<", ">", "<=", ">=" -> queryBySql(field, node.getTerm(), comparator, cb);
      default -> throw new CQLFeatureUnsupportedException("Relation " + comparator + " not implemented yet: " + node);
    };
  }

  private Predicate definedOrDefinedAndEmptyQuery(Path<?> field, String comparator, CriteriaBuilder cb) {
    boolean isString = String.class.equals(field.getJavaType());
    if ("==".equals(comparator) && isString) {
      return cb.and(cb.isNotNull(field), cb.equal(field, ""));
    }

    return cb.isNotNull(field);
  }

  @SuppressWarnings({"rawtypes", "unchecked"})
  private Predicate buildQuery(Path<?> field, Comparable term, String comparator,
                               CriteriaBuilder cb) throws QueryValidationException {
    boolean isString = String.class.equals(field.getJavaType());
    if (isString) {
      return queryByLike((Path<String>) field, (String) term, comparator, cb);
    } else {
      return queryBySql(field, term, comparator, cb);
    }
  }

  private Predicate buildModifiersQuery(Path<?> field, CqlModifiers modifiers, CriteriaBuilder cb)
    throws QueryValidationException {
    var result = cb.conjunction();

    for (Modifier modifier : modifiers.getRelationModifiers()) {
      var fieldName = getFieldNameByModifier(field, modifier);
      var fieldValue = modifier.getValue();

      result = cb.and(result, queryBySql(field.get(fieldName), fieldValue, modifier.getComparison(), cb));
    }

    return result;
  }

  private String getFieldNameByModifier(Path<?> entity, Modifier modifier) {
    var typeName = modifier.getType().substring(1);
    var modelFields = entity.getModel().getBindableJavaType().getDeclaredFields();
    var fieldNamesForAttribute =
      Arrays.stream(modelFields).map(Field::getName)
        .filter(name -> name.equalsIgnoreCase(typeName)).toList();
    if (isEmpty(fieldNamesForAttribute) || fieldNamesForAttribute.size() > 1) {
      throw new CqlQueryValidationException(format("Query contains nonExisting field [%s]", typeName));
    }
    return fieldNamesForAttribute.getFirst();
  }

  /**
   * Create an SQL expression using LIKE query syntax.
   */
  private Predicate queryByLike(Path<String> field0, String term0, String comparator,
                                CriteriaBuilder cb) {

    var wrapper = wrapper(cb);
    var field = wrapper.apply(field0);
    var term = wrapper.apply(cb.literal(cql2like(term0)));
    if (NOT_EQUALS_OPERATOR.equals(comparator)) {
      return cb.notLike(field, term, '\\');
    } else {
      return cb.like(field, term, '\\');
    }
  }

  /**
   * Convert an CQL string into an SQL LIKE string.
   */
  private static String cql2like(String cqlString) {
    return Cql2SqlUtil.cql2like(cqlString).replace("''", "'");
  }

  /**
   * A wrapper with functions lower and/or f_unaccent as needed for {@link #domainClass}.
   */
  private UnaryOperator<Expression<String>> wrapper(CriteriaBuilder cb) {
    var respectAccents = domainClass.getAnnotation(RespectAccents.class) != null;
    var respectCase = domainClass.getAnnotation(RespectCase.class) != null;
    if (respectAccents) {
      if (respectCase) {
        return UnaryOperator.identity();
      } else {
        return cb::lower;
      }
    } else {
      if (respectCase) {
        return expression -> cb.function("f_unaccent", String.class, expression);
      } else {
        return expression -> cb.lower(cb.function("f_unaccent", String.class, expression));
      }
    }
  }

  /**
   * Create an SQL expression using SQL as is syntax.
   */
  @SuppressWarnings({"rawtypes", "unchecked"})
  Predicate queryBySql(Expression field, Comparable term, String comparator,
      CriteriaBuilder cb) throws QueryValidationException {

    var value = (String) term;
    var javaType = field.getJavaType();

    if (String.class.equals(javaType)) {
      UnaryOperator<Expression<String>> wrapper = wrapper(cb);
      field = wrapper.apply(field);
      var termExpression = wrapper.apply(cb.literal(cql2like(term.toString())));
      return toPredicate(field, termExpression, comparator, cb);
    } else if (Number.class.equals(javaType)) {
      term = Integer.parseInt(value);
    } else if (UUID.class.equals(javaType)) {
      term = UUID.fromString(value);
    } else if (Boolean.class.equals(javaType)) {
      term = Boolean.valueOf(value);
    } else if (Date.class.equals(javaType) || Timestamp.class.equals(javaType)) {
      if (isDatesRange(value)) {
        return toFilterByDatesPredicate(field, value, cb);
      } else {
        var dateTime = LocalDateTime.parse(value);
        term = Date.from(dateTime.atZone(ZoneId.systemDefault()).toInstant());
      }
    } else if (LocalDateTime.class.equals(javaType)) {
      if (isDatesRange(value)) {
        return toFilterByLocalDatesPredicate(field, value, cb);
      } else {
        term = LocalDateTime.parse(value);
      }
    } else if (javaType.isEnum()) {
      field = field.as(String.class);
    }

    return toPredicate(field, term, comparator, cb);
  }

  private static CQLFeatureUnsupportedException createUnsupportedException(CQLNode node) {
    return new CQLFeatureUnsupportedException(
      String.format("Not implemented yet node type: %s, CQL: %s", node.getClass().getSimpleName(), node.toCQL()));
  }

  private static boolean isDatesRange(String value) {
    return DATES_RANGE_PATTERN.matcher(value).matches();
  }

  private static Predicate toFilterByDatesPredicate(Expression<Date> field, String value, CriteriaBuilder cb) {
    var dates = value.split(":");
    var dateTimeFrom = LocalDate.parse(dates[0]).atStartOfDay();
    var dateFrom = Date.from(dateTimeFrom.atZone(ZoneId.systemDefault()).toInstant());
    var dateTimeTo = LocalDate.parse(dates[1]).atStartOfDay();
    var dateTo = Date.from(dateTimeTo.atZone(ZoneId.systemDefault()).toInstant());
    return cb.and(cb.greaterThanOrEqualTo(field, dateFrom), cb.lessThan(field, dateTo));
  }

  private static Predicate toFilterByLocalDatesPredicate(Expression<LocalDateTime> field, String value,
    CriteriaBuilder cb) {
    var dates = value.split(":");
    var dateTimeFrom = LocalDate.parse(dates[0]).atStartOfDay();
    var dateTimeTo = LocalDate.parse(dates[1]).atStartOfDay();
    return cb.and(cb.greaterThanOrEqualTo(field, dateTimeFrom), cb.lessThan(field, dateTimeTo));
  }
}
