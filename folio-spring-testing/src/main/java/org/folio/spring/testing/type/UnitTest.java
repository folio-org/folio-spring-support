package org.folio.spring.testing.type;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import org.junit.jupiter.api.Tag;

/**
 * Marks a test class as a unit test.
 * Unit tests focus on testing individual units or components of code in isolation.
 */
@Tag("unit")
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface UnitTest {
}
