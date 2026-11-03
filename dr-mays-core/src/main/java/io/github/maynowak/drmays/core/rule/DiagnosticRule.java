package io.github.maynowak.drmays.core.rule;

import io.github.maynowak.drmays.core.resource.InfrastructureResource;

/**
 * Deterministic, side-effect-free contract for one diagnostic check.
 */
public interface DiagnosticRule {

    RuleId id();

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
     * <p>Implementations must not mutate the resource, the context or any
     * external infrastructure.</p>
     *
     * @param resource the resource under evaluation
     * @param context the full, immutable run context
     * @return a typed outcome attributed to this rule and resource
     */
    DiagnosticOutcome evaluate(InfrastructureResource resource, DiagnosticContext context);
}
