package org.folio.junit.extension;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.PARAMETER)
public @interface RandomLong {

  long min() default Long.MIN_VALUE; // inclusive
      
  long max() default Long.MAX_VALUE; // exclusive
}