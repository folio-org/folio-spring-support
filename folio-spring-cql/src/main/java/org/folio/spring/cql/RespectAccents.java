package org.folio.spring.cql;

import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

/**
 * Respect accents when comparing strings.
 * String are considered equal if they are the same after removing accents.
 *
 * @see IgnoreAccents
 * @see RespectCase
 * @see IgnoreCase
 */
@Documented
@Target({TYPE})
@Retention(RUNTIME)
public @interface RespectAccents {}
