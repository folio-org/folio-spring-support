package org.folio.spring.cql;

import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

/**
 * Ignore accents when comparing strings.
 * This is the default but might be used to make it explicit.
 * String are considered equal if they are the same after removing accents.
 *
 * @see RespectAccents
 * @see IgnoreCase
 * @see RespectCase
 */
@Documented
@Target({TYPE})
@Retention(RUNTIME)
public @interface IgnoreAccents {}
