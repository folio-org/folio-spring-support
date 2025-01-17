package org.folio.spring.utils;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import org.folio.spring.testing.type.UnitTest;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

@UnitTest
class CqlUtilsTest {

  static List<Arguments> cqlEncodingCases() {
    return List.of(
      Arguments.of("", "\"\""),
      Arguments.of("value", "\"value\""),
      Arguments.of("$value%|'", "\"$value%|'\""),
      Arguments.of("\"value\"", "\"\\\"value\\\"\""),
      Arguments.of("value^", "\"value\\^\""),
      Arguments.of("value?", "\"value\\?\""),
      Arguments.of("value*", "\"value\\*\""),
      Arguments.of("value\\", "\"value\\\\\""),
      Arguments.of("^?*\"\\", "\"\\^\\?\\*\\\"\\\\\"")
    );
  }

  @ParameterizedTest
  @MethodSource("cqlEncodingCases")
  void testCqlEncoding(String input, String expected) {
    assertThat(CqlUtils.encodeCql(input)).isEqualTo(expected);
  }
}
