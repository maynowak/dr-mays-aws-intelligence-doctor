package io.github.maynowak.drmays.core.rule.builtin;

import io.github.maynowak.drmays.core.evidence.Evidence;
import io.github.maynowak.drmays.core.finding.DiagnosticFinding;
import io.github.maynowak.drmays.core.finding.Severity;
import io.github.maynowak.drmays.core.resource.InfrastructureResource;
import io.github.maynowak.drmays.core.rule.DiagnosticContext;
import io.github.maynowak.drmays.core.rule.DiagnosticOutcome;
import io.github.maynowak.drmays.core.rule.DiagnosticRule;
import io.github.maynowak.drmays.core.rule.RuleId;
import java.util.Map;
import java.util.Objects;

/** Flags resources without a non-blank {@code owner} tag. */
public final class MissingOwnerTagRule implements DiagnosticRule {

  public static final RuleId RULE_ID = new RuleId("TAG-OWNER-001");

  @Override
  public RuleId id() {
    return RULE_ID;
  }

  @Override
  public String description() {
    return "Checks that every resource declares an owner tag";
  }

  @Override
  public DiagnosticOutcome evaluate(InfrastructureResource resource, DiagnosticContext context) {
    Objects.requireNonNull(resource, "resource");
    Objects.requireNonNull(context, "context");

    var owner = resource.tag("owner");
    if (owner.filter(value -> !value.isBlank()).isPresent()) {
      return new DiagnosticOutcome.NoFinding(RULE_ID, resource.identity(), "owner tag is present");
    }

    var observed = owner.map(value -> value.isBlank() ? "blank" : value).orElse("absent");
    var evidence =
        new Evidence(
            resource.identity(),
            "resource-tag",
            observed,
            "non-blank owner tag",
            Map.of("tag", "owner"));
    var finding =
        new DiagnosticFinding(
            RULE_ID,
            version(),
            Severity.MEDIUM,
            resource.identity(),
            "Resource has no owner tag",
            "The resource does not declare a non-blank owner tag.",
            evidence,
            "Add an owner tag identifying the responsible team.");
    return new DiagnosticOutcome.FindingDetected(finding);
  }
}
