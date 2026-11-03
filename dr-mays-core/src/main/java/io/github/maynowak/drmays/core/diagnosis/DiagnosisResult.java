package io.github.maynowak.drmays.core.diagnosis;

import io.github.maynowak.drmays.core.finding.DiagnosticFinding;
import io.github.maynowak.drmays.core.resource.ResourceIdentity;
import io.github.maynowak.drmays.core.rule.DiagnosticOutcome;
import io.github.maynowak.drmays.core.rule.RuleId;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;

/**
 * Deterministically ordered result of one diagnosis run.
 *
 * @param outcomes the outcomes, sorted by resource canonical ID and rule ID
 */
public record DiagnosisResult(List<DiagnosticOutcome> outcomes) {

  private static final Comparator<DiagnosticOutcome> ORDER =
      Comparator.comparing((DiagnosticOutcome outcome) -> outcome.resource().canonicalId())
          .thenComparing(outcome -> outcome.ruleId().value())
          .thenComparing(DiagnosticOutcome::summary);

  public DiagnosisResult {
    Objects.requireNonNull(outcomes, "outcomes");
    var sorted = new ArrayList<DiagnosticOutcome>(outcomes.size());
    var seen = new HashSet<EvaluationKey>();

    for (var outcome : outcomes) {
      var value = Objects.requireNonNull(outcome, "outcome");
      var key = new EvaluationKey(value.resource(), value.ruleId());
      if (!seen.add(key)) {
        throw new IllegalArgumentException(
            "duplicate outcome for rule "
                + value.ruleId().value()
                + " and resource "
                + value.resource().canonicalId());
      }
      sorted.add(value);
    }

    sorted.sort(ORDER);
    outcomes = List.copyOf(sorted);
  }

  /**
   * Returns only the findings, in deterministic order.
   *
   * @return immutable list of findings
   */
  public List<DiagnosticFinding> findings() {
    return outcomes.stream()
        .<DiagnosticFinding>mapMulti((outcome, downstream) -> {
          if (outcome instanceof DiagnosticOutcome.FindingDetected detected) {
            downstream.accept(detected.finding());
          }
        })
        .toList();
  }

  public int findingCount() {
    return findings().size();
  }

  public boolean hasFindings() {
    return outcomes.stream().anyMatch(DiagnosticOutcome.FindingDetected.class::isInstance);
  }

  private record EvaluationKey(ResourceIdentity resource, RuleId ruleId) {}
}
