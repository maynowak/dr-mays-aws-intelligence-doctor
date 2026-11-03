package io.github.maynowak.drmays.core.rule;

import io.github.maynowak.drmays.core.finding.DiagnosticFinding;
import io.github.maynowak.drmays.core.resource.ResourceIdentity;
import io.github.maynowak.drmays.core.validation.DomainChecks;
import java.util.Objects;

/** Typed result of evaluating one rule against one resource. */
public sealed interface DiagnosticOutcome
    permits DiagnosticOutcome.FindingDetected,
        DiagnosticOutcome.NoFinding,
        DiagnosticOutcome.NotApplicable {

  RuleId ruleId();

  ResourceIdentity resource();

  /**
   * Returns a stable one-line summary for logs and assertions.
   *
   * @return the outcome summary
   */
  default String summary() {
    return switch (this) {
      case FindingDetected detected ->
          "FINDING " + detected.ruleId().value() + " @ " + detected.resource().canonicalId();
      case NoFinding noFinding ->
          "NO_FINDING " + noFinding.ruleId().value() + " @ " + noFinding.resource().canonicalId();
      case NotApplicable notApplicable ->
          "NOT_APPLICABLE "
              + notApplicable.ruleId().value()
              + " @ "
              + notApplicable.resource().canonicalId();
    };
  }

  /** A rule produced a finding. */
  record FindingDetected(DiagnosticFinding finding) implements DiagnosticOutcome {

    public FindingDetected {
      Objects.requireNonNull(finding, "finding");
    }

    @Override
    public RuleId ruleId() {
      return finding.ruleId();
    }

    @Override
    public ResourceIdentity resource() {
      return finding.resource();
    }
  }

  /** A rule ran and found no issue. */
  record NoFinding(RuleId ruleId, ResourceIdentity resource, String reason)
      implements DiagnosticOutcome {

    public NoFinding {
      ruleId = Objects.requireNonNull(ruleId, "ruleId");
      resource = Objects.requireNonNull(resource, "resource");
      reason = DomainChecks.requireText(reason, "reason");
    }
  }

  /** A rule does not apply to the resource. */
  record NotApplicable(RuleId ruleId, ResourceIdentity resource, String reason)
      implements DiagnosticOutcome {

    public NotApplicable {
      ruleId = Objects.requireNonNull(ruleId, "ruleId");
      resource = Objects.requireNonNull(resource, "resource");
      reason = DomainChecks.requireText(reason, "reason");
    }
  }
}
