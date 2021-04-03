package org.folio.junit.extension;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.PARAMETER)
public @interface RandomInt {

  int min() default Short.MIN_VALUE; // inclusive
      
  int max() default Short.MAX_VALUE; // exclusive
}