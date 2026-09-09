package org.folio.spring.cql.nativex;

import static org.springframework.aot.hint.MemberCategory.INVOKE_PUBLIC_METHODS;

import org.folio.spring.cql.IgnoreAccents;
import org.folio.spring.cql.IgnoreCase;
import org.folio.spring.cql.JpaCqlRepositoryImpl;
import org.folio.spring.cql.RespectAccents;
import org.folio.spring.cql.RespectCase;
import org.springframework.aot.hint.RuntimeHints;
import org.springframework.aot.hint.RuntimeHintsRegistrar;

/**
 * GraalVM native-image reachability hints for {@code folio-spring-cql}.
 *
 * <p>Wired into the context via {@code @ImportRuntimeHints} on
 * {@link org.folio.spring.cql.JpaCqlConfiguration} (an auto-configuration entry point).</p>
 */
public class JpaCqlRuntimeHints implements RuntimeHintsRegistrar {

  @Override
  public void registerHints(RuntimeHints hints, ClassLoader classLoader) {
    // Cql2JpaCriteria#wrapper reads class-level CQL annotations off the consuming application's
    // @Entity classes via Class#getAnnotation, so the annotation TYPES must stay reflectively
    // reachable. (The entity classes themselves are the consumer's responsibility — their reflection
    // metadata is contributed by Spring Data JPA's AOT processing in the consuming app.)
    //
    // Only @RespectAccents/@RespectCase are read today (Cql2JpaCriteria:511-512); @IgnoreAccents and
    // @IgnoreCase are part of the same public CQL annotation contract and are registered for
    // completeness so consumer entities annotated with them remain introspectable.
    hints.reflection().registerType(RespectAccents.class);
    hints.reflection().registerType(RespectCase.class);
    hints.reflection().registerType(IgnoreAccents.class);
    hints.reflection().registerType(IgnoreCase.class);

    // The CQL query methods (findByCql/countByCql) are contributed to every JpaCqlRepository by the
    // JpaCqlRepositoryImpl base fragment. Spring Data's AOT repository processor cannot derive them
    // (they are not name-based queries — "cql" is not an entity property), so it logs a
    // PropertyReferenceException at build time and does NOT generate an optimized invocation. At runtime
    // Spring Data therefore falls back to invoking the fragment method reflectively via
    // RepositoryFragmentMethodInvoker, which fails natively with MissingReflectionRegistrationError unless
    // the impl's methods are registered for reflective invocation. Register them here so every consuming
    // repository's CQL search works in the native image.
    hints.reflection().registerType(JpaCqlRepositoryImpl.class,
        builder -> builder.withMembers(INVOKE_PUBLIC_METHODS));

    // Note: the org.z3950.zing.cql.* parser types (CQLParser, CQLNode, ...) are statically imported
    // and used by Cql2JpaCriteria, so native-image static analysis already makes them reachable — no
    // reflection hint is required for them.
  }
}
