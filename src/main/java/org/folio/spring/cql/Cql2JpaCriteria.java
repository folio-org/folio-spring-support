package org.folio.spring.cql;

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
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.UUID;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.lang3.StringUtils;
import org.folio.cql2pgjson.exception.CQLFeatureUnsupportedException;
import org.folio.cql2pgjson.exception.QueryValidationException;
import org.folio.cql2pgjson.model.CqlModifiers;
import org.folio.cql2pgjson.model.CqlSort;
import org.folio.cql2pgjson.model.CqlTermFormat;
import org.folio.cql2pgjson.util.Cql2SqlUtil;
import org.z3950.zing.cql.CQLAndNode;
import org.z3950.zing.cql.CQLBooleanNode;
import org.z3950.zing.cql.CQLNode;
import org.z3950.zing.cql.CQLNotNode;
import org.z3950.zing.cql.CQLOrNode;
import org.z3950.zing.cql.CQLParseException;
import org.z3950.zing.cql.CQLParser;
import org.z3950.zing.cql.CQLSortNode;
import org.z3950.zing.cql.CQLTermNode;
import org.z3950.zing.cql.ModifierSet;

@Log4j2
public class Cql2JpaCriteria<E> {

  private static final String NOT_EQUALS_OPERATOR = "<>";
  private static final String ASTERISKS_SIGN = "*";

  private final Class<E> domainClass;
  private final EntityManager em;
  private final CQLParser cqlParser;

  public Cql2JpaCriteria(Class<E> domainClass, EntityManager entityManager) {
    this.domainClass = domainClass;
    this.em = entityManager;
    this.cqlParser = new CQLParser();
  }

  /**
   * Convert the CQL query into WHERE and the ORDER BY SQL clauses and return {@link CriteriaQuery} for selection.
   *
   * @param cql the query to convert
   * @return {@link CriteriaQuery} for selection
   */
  public CriteriaQuery<E> toCollectCriteria(String cql) {
    try {
      var node = cqlParser.parse(cql);

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
   * Convert the CQL query into WHERE and the ORDER BY SQL clauses and return {@link CriteriaQuery} for count.
   *
   * @param cql the query to convert
   * @return {@link CriteriaQuery} for count
   */
  public CriteriaQuery<Long> toCountCriteria(String cql) {
    try {
      var node = cqlParser.parse(cql);

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

  private <T> Predicate createPredicate(CQLNode node, Root<E> root, CriteriaBuilder cb, CriteriaQuery<T> query)
    throws QueryValidationException {
    Predicate predicates;
    if (node instanceof CQLSortNode sortNode) {
      var orders = toOrders(sortNode, root, cb);
      query.orderBy(orders);
      predicates = process(sortNode.getSubtree(), cb, root);
    } else {
      predicates = process(node, cb, root);
    }
    return predicates;
  }

  private List<Order> toOrders(CQLSortNode node, Root<E> root, CriteriaBuilder cb) throws CQLFeatureUnsupportedException {
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

  private Predicate process(CQLNode node, CriteriaBuilder cb, Root<E> root) throws QueryValidationException {
    if (node instanceof CQLTermNode cqlTermNode) {
      return processTerm(cqlTermNode, cb, root);
    } else if (node instanceof CQLBooleanNode cqlBooleanNode) {
      return processBoolean(cqlBooleanNode, cb, root);
    } else {
      throw createUnsupportedException(node);
    }
  }

  private Predicate processBoolean(CQLBooleanNode node, CriteriaBuilder cb, Root<E> root) throws QueryValidationException {
    if (node instanceof CQLAndNode) {
      return cb.and(process(node.getLeftOperand(), cb, root), process(node.getRightOperand(), cb, root));
    } else if (node instanceof CQLOrNode) {
      if (node.getRightOperand().getClass() == CQLTermNode.class) {
        // special case for the query the UI uses most often, before the user has
        // typed in anything: title=* OR contributors*= OR identifier=*
        CQLTermNode r = (CQLTermNode) (node.getRightOperand());
        if (ASTERISKS_SIGN.equals(r.getTerm()) && "=".equals(r.getRelation().getBase())) {
          log.debug("pgFT(): Simplifying =* OR =* ");
          return process(node.getLeftOperand(), cb, root);
        }
      }
      return cb.or(process(node.getLeftOperand(), cb, root), process(node.getRightOperand(), cb, root));
    } else if (node instanceof CQLNotNode) {
      return cb.not(
        cb.and(process(node.getLeftOperand(), cb, root), process(node.getRightOperand(), cb, root)));
    } else {
      throw createUnsupportedException(node);
    }
  }

  private Predicate processTerm(CQLTermNode node, CriteriaBuilder cb, Root<E> root) throws QueryValidationException {
    String fieldName = node.getIndex();
    if (StringUtils.startsWithIgnoreCase(fieldName, "cql")) {
      if ("cql.allRecords".equalsIgnoreCase(fieldName)) {
        return cb.and();
      } else {
        throw createUnsupportedException(node);
      }
    }

    var field = getPath(fieldName, root);
    var cqlModifiers = new CqlModifiers(node);
    return indexNode(field, node, cqlModifiers, cb);
  }

  private Path<?> getPath(String fieldName, Root<E> root) {
    if (fieldName.contains(".")) {
      final int dotIdx = fieldName.indexOf(".");
      final var attributeName = fieldName.substring(0, dotIdx);
      Join<E, Object> children = root.join(attributeName, JoinType.LEFT);
      return children.get(fieldName.substring(dotIdx + 1));
    } else {
      return root.get(fieldName);
    }
  }

  private static <G extends Comparable<? super G>> Predicate toPredicate(Expression<G> field, G value, String comparator,
                                                                  CriteriaBuilder cb) throws QueryValidationException {

    return switch (comparator) {
      case ">" -> cb.greaterThan(field, value);
      case "<" -> cb.lessThan(field, value);
      case ">=" -> cb.greaterThanOrEqualTo(field, value);
      case "<=" -> cb.lessThanOrEqualTo(field, value);
      case "==", "=" -> cb.equal(field, value);
      case NOT_EQUALS_OPERATOR -> cb.notEqual(field, value);
      default -> throw new QueryValidationException(
        "CQL: Unsupported operator '"
          + comparator
          + "', "
          + " only supports '=', '==', and '<>' (possibly with right truncation)");
    };
  }

  private Predicate indexNode(Path<?> field, CQLTermNode node, CqlModifiers modifiers,
                              CriteriaBuilder cb)
    throws QueryValidationException {

    boolean isString = String.class.equals(field.getJavaType());

    String comparator = node.getRelation().getBase().toLowerCase();

    switch (comparator) {
      case "=":
        if (CqlTermFormat.NUMBER.equals(modifiers.getCqlTermFormat())) {
          return queryBySql(field, node, comparator, cb);
        }
      case "adj", "all", "any":
        return buildQuery(field, node, isString, comparator, cb);
      case "==", NOT_EQUALS_OPERATOR:
        return buildQuery(field, node, isString, comparator, cb);
      case "<", ">", "<=", ">=":
        return queryBySql(field, node, comparator, cb);
      default:
        throw new CQLFeatureUnsupportedException("Relation " + comparator + " not implemented yet: " + node);
    }
  }

  @SuppressWarnings("unchecked")
  private Predicate buildQuery(Path<?> field, CQLTermNode node, boolean isString, String comparator,
                               CriteriaBuilder cb)
    throws QueryValidationException {
    if (isString) {
      return queryByLike((Path<String>) field, node, comparator, cb);
    } else {
      return queryBySql(field, node, comparator, cb);
    }
  }

  /**
   * Create an SQL expression using LIKE query syntax.
   */
  private static Predicate queryByLike(Path<String> field, CQLTermNode node, String comparator,
                                CriteriaBuilder cb) {
    if (NOT_EQUALS_OPERATOR.equals(comparator)) {
      return cb.notLike(field, cql2like(node.getTerm()), '\\');
    } else {
      return cb.like(field, cql2like(node.getTerm()), '\\');
    }
  }

  /**
   * Convert an CQL string into an SQL LIKE string.
   */
  private static String cql2like(String cqlString) {
    return Cql2SqlUtil.cql2like(cqlString).replace("''", "'");
  }

  /**
   * Create an SQL expression using SQL as is syntax.
   */
  @SuppressWarnings({"rawtypes", "unchecked"})
  private static Predicate queryBySql(Expression field, CQLTermNode node, String comparator,
                               CriteriaBuilder cb) throws QueryValidationException {
    Comparable val = node.getTerm();

    Class<?> javaType = field.getJavaType();
    if (Number.class.equals(javaType)) {
      val = Integer.parseInt((String) val);
    } else if (UUID.class.equals(javaType)) {
      val = UUID.fromString((String) val);
    } else if (Boolean.class.equals(javaType)) {
      val = Boolean.valueOf((String) val);
    } else if (Date.class.equals(javaType)) {
      var dateTime = LocalDateTime.parse((String) val);
      val = Date.from(dateTime.atZone(ZoneId.systemDefault()).toInstant());
    } else if (javaType.isEnum()) {
      field = field.as(String.class);
    }

    return toPredicate(field, val, comparator, cb);
  }

  private static CQLFeatureUnsupportedException createUnsupportedException(CQLNode node) {
    return new CQLFeatureUnsupportedException(
      String.format("Not implemented yet node type: %s, CQL: %s", node.getClass().getSimpleName(), node.toCQL()));
  }
}
