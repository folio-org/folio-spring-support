package org.folio.spring.testing.extension;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import org.folio.spring.testing.extension.impl.DatabaseCleanupExtension;
import org.junit.jupiter.api.extension.ExtendWith;

/**
 * Annotation used to trigger the cleanup of specified database tables within specific tenants, if applicable,
 * when the Spring application context exists.
 * This annotation is associated with the {@link DatabaseCleanupExtension} extension.
 * It can be applied at the annotation, method, or type level.
 */
@Target({ElementType.ANNOTATION_TYPE, ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@ExtendWith({DatabaseCleanupExtension.class})
public @interface DatabaseCleanup {

  /**
   * Specifies the tables that should be cleaned up.
   *
   * @return An array of table names that require cleanup.
   */
  String[] tables();

  /**
   * Specifies the tenants that should be checked for cleanup.
   * If empty, tables from all tenants will be cleaned.
   *
   * @return An array of tenant names for which cleanup should be performed.
   */
  String[] tenants() default { };
}
