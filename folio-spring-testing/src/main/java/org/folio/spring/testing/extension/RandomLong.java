package org.folio.spring.testing.extension;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * An annotation used to mark a parameter as requiring a random long value within a specified range.
 * This annotation can be applied to method parameters.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.PARAMETER)
public @interface RandomLong {

  /**
   * Specifies the minimum value (inclusive) for the random long.
   *
   * @return The inclusive minimum value for the random long (defaults to {@code Long.MIN_VALUE}).
   */
  long min() default Long.MIN_VALUE;

  /**
   * Specifies the maximum value (exclusive) for the random long.
   *
   * @return The exclusive maximum value for the random long (defaults to {@code Long.MAX_VALUE}).
   */
  long max() default Long.MAX_VALUE;
}
