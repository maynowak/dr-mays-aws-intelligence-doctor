package io.github.maynowak.drmays.core.rule;

import io.github.maynowak.drmays.core.resource.InfrastructureResource;

/** Deterministic, side-effect-free contract for one diagnostic check. */
public interface DiagnosticRule {

  /**
   * Returns the stable rule identifier.
   *
   * @return the rule ID
   */
  RuleId id();

  /**
   * Returns a short description of what the rule checks.
   *
   * @return the rule description
   */
  String description();

  /**
   * Rule version recorded with findings.
   *
   * @return the default version {@code 1.0}
   */
  default String version() {
    return "1.0";
  }

  /**
   * Evaluates the rule for one resource.
   *
   * <p>Implementations must not mutate the resource, the context or any external infrastructure.
   *
   * @param resource the resource under evaluation
   * @param context the full, immutable run context
   * @return a typed outcome attributed to this rule and resource
   */
  DiagnosticOutcome evaluate(InfrastructureResource resource, DiagnosticContext context);
}
