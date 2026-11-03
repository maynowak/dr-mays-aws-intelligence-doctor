package io.github.maynowak.drmays.ai;

import io.github.maynowak.drmays.core.dna.GraphCompleteness;
import io.github.maynowak.drmays.core.finding.DiagnosticFinding;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/** Deterministic, offline analyzer used by default and in CI. */
public final class MockIntelligenceAnalyzer implements IntelligenceAnalyzer {

  private static final ModelMetadata METADATA =
      new ModelMetadata("mock", "deterministic-v1", "1.0");

  @Override
  public IntelligenceResult analyze(IntelligenceRequest request) {
    var ordered =
        request.findings().stream()
            .sorted(
                Comparator.comparing(
                        (DiagnosticFinding finding) -> finding.resource().canonicalId())
                    .thenComparing(finding -> finding.ruleId().value()))
            .toList();

    var explanations = new ArrayList<FindingExplanation>();
    var recommendations = new ArrayList<Recommendation>();
    var evidence = new ArrayList<EvidenceReference>();

    for (var finding : ordered) {
      var reference =
          new EvidenceReference(finding.ruleId(), finding.resource(), finding.evidence().source());
      evidence.add(reference);
      explanations.add(
          new FindingExplanation(
              finding.ruleId(),
              finding.resource(),
              "Rule "
                  + finding.ruleId().value()
                  + " observed: "
                  + finding.title()
                  + ". "
                  + finding.description(),
              List.of(reference)));
      recommendations.add(
          new Recommendation(
              "Address " + finding.title(), finding.recommendation(), finding.severity()));
    }

    var uncertainties = new ArrayList<String>();
    if (request.completeness() == GraphCompleteness.PARTIAL) {
      uncertainties.add("Discovery data is partial; unknown resources are not covered.");
    }
    if (ordered.isEmpty()) {
      uncertainties.add("No deterministic findings were produced.");
    }

    var summary =
        ordered.isEmpty()
            ? "No deterministic findings were produced."
            : ordered.size()
                + " deterministic finding(s) were explained; evidence remains authoritative.";
    return new IntelligenceResult(
        ModelExecutionStatus.SUCCESS,
        METADATA,
        summary,
        explanations,
        recommendations,
        evidence,
        request.relationships(),
        uncertainties);
  }
}
