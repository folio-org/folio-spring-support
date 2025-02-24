package org.folio.spring.cql;

import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

/**
 * Use case insensitive matching when comparing strings.
 * This is the default but might be used to make it explicit.
 *
 * @see RespectCase
 * @see RespectAccents
 * @see IgnoreAccents
 */
@Documented
@Target({TYPE})
@Retention(RUNTIME)
public @interface IgnoreCase {}
