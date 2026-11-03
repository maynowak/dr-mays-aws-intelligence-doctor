package io.github.maynowak.drmays.core.diagnosis;

import io.github.maynowak.drmays.core.resource.InfrastructureResource;
import io.github.maynowak.drmays.core.rule.DiagnosticContext;
import io.github.maynowak.drmays.core.rule.DiagnosticOutcome;
import io.github.maynowak.drmays.core.rule.DiagnosticRule;
import io.github.maynowak.drmays.core.validation.DomainChecks;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.TreeMap;

/** Runs registered rules over an immutable context without Spring, AWS or AI. */
public final class DiagnosisEngine {

  private final List<DiagnosticRule> rules;

  /**
   * Creates an engine with rules sorted by rule ID.
   *
   * @param rules the rules to register
   */
  public DiagnosisEngine(Collection<DiagnosticRule> rules) {
    Objects.requireNonNull(rules, "rules");
    var ordered = new TreeMap<String, DiagnosticRule>();

    for (var rule : rules) {
      var value = Objects.requireNonNull(rule, "rule");
      var id = Objects.requireNonNull(value.id(), "rule.id");
      DomainChecks.requireText(value.description(), "rule.description");
      DomainChecks.requireText(value.version(), "rule.version");

      if (ordered.putIfAbsent(id.value(), value) != null) {
        throw new IllegalArgumentException("duplicate rule id: " + id.value());
      }
    }

    this.rules = List.copyOf(ordered.values());
  }

  public List<DiagnosticRule> rules() {
    return rules;
  }

  /**
   * Evaluates every rule for every resource and returns a deterministic result.
   *
   * @param context the immutable run context
   * @return ordered outcomes and derived findings
   */
  public DiagnosisResult diagnose(DiagnosticContext context) {
    Objects.requireNonNull(context, "context");
    var outcomes = new ArrayList<DiagnosticOutcome>();

    for (InfrastructureResource resource : context.resources()) {
      for (DiagnosticRule rule : rules) {
        var outcome =
            Objects.requireNonNull(
                rule.evaluate(resource, context),
                "rule returned null outcome: " + rule.id().value());

        if (!outcome.ruleId().equals(rule.id())) {
          throw new IllegalStateException(
              "rule " + rule.id().value() + " returned outcome for " + outcome.ruleId().value());
        }
        if (!outcome.resource().equals(resource.identity())) {
          throw new IllegalStateException(
              "rule "
                  + rule.id().value()
                  + " returned outcome for "
                  + outcome.resource().canonicalId());
        }
        outcomes.add(outcome);
      }
    }

    return new DiagnosisResult(outcomes);
  }
}
