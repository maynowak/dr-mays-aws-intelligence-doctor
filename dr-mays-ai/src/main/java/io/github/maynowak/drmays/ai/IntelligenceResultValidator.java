package io.github.maynowak.drmays.ai;

import io.github.maynowak.drmays.core.finding.DiagnosticFinding;
import io.github.maynowak.drmays.core.resource.ResourceIdentity;
import io.github.maynowak.drmays.core.rule.RuleId;
import java.util.HashMap;
import java.util.HashSet;

/** Validates model output against the verified request, rejecting invented content. */
public final class IntelligenceResultValidator {

  public IntelligenceResult validate(IntelligenceRequest request, IntelligenceResult result) {
    if (result.status() != ModelExecutionStatus.SUCCESS) {
      return result;
    }
    if (result.summary().isBlank()) {
      return IntelligenceResult.invalidResponse(result.metadata());
    }

    var findings = new HashMap<FindingKey, DiagnosticFinding>();
    for (var finding : request.findings()) {
      findings.put(new FindingKey(finding.ruleId(), finding.resource()), finding);
    }

    var explained = new HashSet<FindingKey>();
    for (var explanation : result.explanations()) {
      var key = new FindingKey(explanation.ruleId(), explanation.resource());
      var finding = findings.get(key);
      if (finding == null || !explained.add(key)) {
        return IntelligenceResult.invalidResponse(result.metadata());
      }
      for (var reference : explanation.evidence()) {
        if (!matchesEvidence(reference, finding)) {
          return IntelligenceResult.invalidResponse(result.metadata());
        }
      }
    }

    for (var reference : result.evidenceReferences()) {
      var finding = findings.get(new FindingKey(reference.ruleId(), reference.resource()));
      if (finding == null || !matchesEvidence(reference, finding)) {
        return IntelligenceResult.invalidResponse(result.metadata());
      }
    }

    for (var relationship : result.verifiedRelationships()) {
      if (!request.relationships().contains(relationship)) {
        return IntelligenceResult.invalidResponse(result.metadata());
      }
    }
    return result;
  }

  private static boolean matchesEvidence(EvidenceReference reference, DiagnosticFinding finding) {
    return finding.ruleId().equals(reference.ruleId())
        && finding.resource().equals(reference.resource())
        && finding.evidence().source().equals(reference.source());
  }

  private record FindingKey(RuleId ruleId, ResourceIdentity resource) {}
}
