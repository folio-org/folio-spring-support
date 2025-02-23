package org.folio.spring.cql;

import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

/**
 * Use case sensitive matching when comparing strings.
 *
 * @see IgnoreCase
 * @see RespectAccents
 * @see IgnoreAccents
 */
@Documented
@Target({TYPE})
@Retention(RUNTIME)
public @interface RespectCase {}
