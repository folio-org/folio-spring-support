package org.folio.spring.domain;

import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.usertype.EnhancedUserType;

import java.io.Serializable;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;

/**
 * Simple type to support PostgreSQL ENUM types
 */
public class PostgreEnumTypeSql implements EnhancedUserType {
  @Override
  public String objectToSQLString(Object value) {
    return value != null ? value.toString() : null;
  }

  @Override
  public String toXMLString(Object value) {
    return objectToSQLString(value);
  }

  @Override
  public Object fromXMLString(String xmlValue) {
    return xmlValue;
  }

  @Override
  public int[] sqlTypes() {
    return new int[]{Types.OTHER};
  }

  @Override
  public Class returnedClass() {
    return String.class;
  }

  @Override
  public boolean equals(Object x, Object y) {
    return x != null && x.equals(y);
  }

  @Override
  public int hashCode(Object x) {
    return x != null ? x.hashCode() : 0;
  }

  @Override
  public Object nullSafeGet(ResultSet rs, String[] names, SharedSessionContractImplementor session, Object owner) throws SQLException {
    return rs.getString(names[0]);
  }

  @Override
  public void nullSafeSet(PreparedStatement st, Object value, int index, SharedSessionContractImplementor session) throws SQLException {
    if (value == null) {
      st.setNull(index, Types.OTHER);
    } else {
      st.setObject(index, value.toString(), Types.OTHER);
    }
  }

  @Override
  public Object deepCopy(Object value) {
    return value;
  }

  @Override
  public boolean isMutable() {
    return false;
  }

  @Override
  public Serializable disassemble(Object value) {
    return value instanceof String ? (Serializable) value : null;
  }

  @Override
  public Object assemble(Serializable cached, Object owner) {
    return cached;
  }

  @Override
  public Object replace(Object original, Object target, Object owner) {
    return original;
  }

}
