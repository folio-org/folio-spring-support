package org.folio.spring.testing.extension;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import org.folio.spring.testing.extension.impl.PostgresContainerExtension;
import org.junit.jupiter.api.extension.ExtendWith;

/**
 * An annotation used to enable the Postgres container for integration tests at the class level.
 * This annotation is used in conjunction with the {@link PostgresContainerExtension} extension.
 * It can be applied to test classes to enable the use of a Postgres container during testing.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
@ExtendWith(PostgresContainerExtension.class)
public @interface EnablePostgres { }
