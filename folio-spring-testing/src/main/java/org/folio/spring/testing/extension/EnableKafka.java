package org.folio.spring.testing.extension;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import org.folio.spring.testing.extension.impl.KafkaContainerExtension;
import org.junit.jupiter.api.extension.ExtendWith;

/**
 * An annotation used to enable a Kafka container for integration tests at the class level.
 * This annotation is used in conjunction with the {@link KafkaContainerExtension} extension.
 * It should be applied to test classes to enable the use of a Kafka container during testing.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
@ExtendWith(KafkaContainerExtension.class)
public @interface EnableKafka { }
