package org.folio.spring.domain;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.Serializable;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;

import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import org.folio.junit.extension.Random;
import org.folio.junit.extension.RandomParametersExtension;

@ExtendWith({
    RandomParametersExtension.class,
    MockitoExtension.class
})
class PostgreEnumTypeSqlTest {

  private final PostgreEnumTypeSql type = new PostgreEnumTypeSql();

  @Test
  void shouldReturnStringAsSQLRepresentationOfObject(@Random Object obj) {
    assertEquals(obj.toString(), type.toString(obj));
  }

  @Test
  void shouldReturnNullAsSQLRepresentationOfNull() {
    assertNull(type.toString(null));
  }

  @Test
  void shouldReturnOtherAsSqlType() {
    assertEquals(Types.OTHER, type.getSqlType());
  }

  @Test
  void shouldReturnStringClass() {
    assertEquals(String.class, type.returnedClass());
  }

  @Test
  void shouldReturnTrueIfObjectsAreEqual(@Random Object obj) {
    assertTrue(type.equals(obj, obj));
  }

  @Test
  void shouldReturnFalseIfObjectsAreNotEqual(@Random Object obj1, @Random Object obj2) {
    assertFalse(type.equals(obj1, obj2));
  }

  @Test
  void shouldReturnFalseIfObjectIsNull(@Random Object obj) {
    assertFalse(type.equals(null, obj));
  }

  @Test
  void shouldReturnObjectHashCode(@Random Object obj) {
    assertEquals(obj.hashCode(), type.hashCode(obj));
  }

  @Test
  void shouldReturnZeroAsHashCodeOfNull() {
    assertEquals(0, type.hashCode(null));
  }

  @Test
  void shouldReturnStringFromResultSet(@Mock ResultSet rs,
      @Mock SharedSessionContractImplementor session, @Random Object owner) throws SQLException {
    String result = "RESULT";
    when(rs.getString(0)).thenReturn(result);

    assertEquals(result, type.nullSafeGet(rs, 0, session, owner));
  }

  @Test
  void shouldSetStatementParameterToStringValueWithTypeOther(@Mock PreparedStatement st, @Random Object value,
      @Random int index, @Mock SharedSessionContractImplementor session) throws SQLException {
    type.nullSafeSet(st, value, index, session);

    verify(st).setObject(index, value.toString(), Types.OTHER);
  }

  @Test
  void shouldSetStatementParameterToNullWithTypeOther(@Mock PreparedStatement st,
      @Random int index, @Mock SharedSessionContractImplementor session) throws SQLException {
    type.nullSafeSet(st, null, index, session);

    verify(st).setNull(index, Types.OTHER);
  }

  @Test
  void shouldReturnFalseAsMutable() {
    assertFalse(type.isMutable());
  }

  @Test
  void shouldReturnSameObjectAsDeepCopy(@Random Object obj) {
    assertSame(obj, type.deepCopy(obj));
  }

  @Test
  void shouldReturnSameCachedAsAssembleResult(@Random SerializableObj cached, @Random Object owner) {
    assertSame(cached, type.assemble(cached, owner));
  }

  @Test
  void shouldReturnGivenStringAsDisassembleResult(@Random String str) {
    assertSame(str, type.disassemble(str));
  }

  @Test
  void shouldReturnNullAsDisassembleResultIfGivenIsNotString(@Random Object obj) {
    assertNull(type.disassemble(obj));
  }

  @Test
  void shouldReturnOriginalAsReplaceResult(@Random Object original, @Random Object target, @Random Object owner) {
    assertSame(original, type.replace(original, target, owner));
  }

  public static class SerializableObj implements Serializable {
  }

}
