package org.folio.spring.testing.extension;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * An annotation used to mark a parameter as requiring a random integer value within a specified range.
 * This annotation can be applied to method parameters.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.PARAMETER)
public @interface RandomInt {

  /**
   * Specifies the minimum value (inclusive) for the random integer.
   *
   * @return The inclusive minimum value for the random integer (defaults to {@code Integer.MIN_VALUE}).
   */
  int min() default Short.MIN_VALUE; // inclusive

  /**
   * Specifies the maximum value (exclusive) for the random integer.
   *
   * @return The exclusive maximum value for the random integer (defaults to {@code Integer.MAX_VALUE}).
   */
  int max() default Short.MAX_VALUE; // exclusive
}
