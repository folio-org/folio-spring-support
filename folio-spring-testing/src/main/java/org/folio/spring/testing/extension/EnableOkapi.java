package org.folio.spring.testing.extension;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import org.folio.spring.testing.extension.impl.OkapiExtension;
import org.junit.jupiter.api.extension.ExtendWith;

/**
 * Annotation used to start a WireMockServer on a random port and configure required Spring properties
 * for interacting with Okapi.
 * To access the OKAPI URL or WireMock, declare a static field of type
 * {@link org.folio.spring.testing.extension.impl.OkapiConfiguration}.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
@ExtendWith(OkapiExtension.class)
public @interface EnableOkapi { }
