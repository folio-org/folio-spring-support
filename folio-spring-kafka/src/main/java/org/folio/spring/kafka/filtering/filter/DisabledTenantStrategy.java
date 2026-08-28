package org.folio.spring.kafka.filtering.filter;

/**
 * Strategy applied when a tenant is not entitled to receive messages for the current module.
 */
public enum DisabledTenantStrategy {

  /** Accept the record and let the listener process it. */
  ACCEPT,

  /** Silently discard the record. */
  SKIP,

  /** Throw a typed exception to signal the unexpected state. */
  FAIL
}
