package io.github.maynowak.drmays.ai;

import static io.github.maynowak.drmays.ai.AiTestFixtures.RESOURCE;
import static io.github.maynowak.drmays.ai.AiTestFixtures.RULE;
import static io.github.maynowak.drmays.ai.AiTestFixtures.request;
import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import org.junit.jupiter.api.Test;

class IntelligenceResultValidatorTest {

  private final IntelligenceResultValidator validator = new IntelligenceResultValidator();
  private final ModelMetadata metadata = new ModelMetadata("mock", "m", "1");

  @Test
  void acceptsValidResult() {
    var result = new MockIntelligenceAnalyzer().analyze(request());

    assertThat(validator.validate(request(), result)).isSameAs(result);
  }

  @Test
  void rejectsExplanationForUnknownFinding() {
    var reference = new EvidenceReference(RULE, RESOURCE, "resource-attribute");
    var unknown =
        new IntelligenceResult(
            ModelExecutionStatus.SUCCESS,
            metadata,
            "summary",
            List.of(
                new FindingExplanation(
                    new io.github.maynowak.drmays.core.rule.RuleId("UNKNOWN-001"),
                    RESOURCE,
                    "unknown",
                    List.of(reference))),
            List.of(),
            List.of(reference),
            List.of(),
            List.of());

    assertThat(validator.validate(request(), unknown).status())
        .isEqualTo(ModelExecutionStatus.INVALID_RESPONSE);
  }

  @Test
  void rejectsUnknownEvidenceReference() {
    var result = new MockIntelligenceAnalyzer().analyze(request());
    var invalid =
        new IntelligenceResult(
            result.status(),
            result.metadata(),
            result.summary(),
            result.explanations(),
            result.recommendations(),
            List.of(new EvidenceReference(RULE, RESOURCE, "invented-source")),
            result.verifiedRelationships(),
            result.uncertainties());

    assertThat(validator.validate(request(), invalid).status())
        .isEqualTo(ModelExecutionStatus.INVALID_RESPONSE);
  }

  @Test
  void rejectsUnverifiedRelationship() {
    var result = new MockIntelligenceAnalyzer().analyze(request());
    var otherResource =
        new io.github.maynowak.drmays.core.resource.ResourceIdentity(
            "aws", "000000000000", "eu-west-1", "network", "other");
    var invalid =
        new IntelligenceResult(
            result.status(),
            result.metadata(),
            result.summary(),
            result.explanations(),
            result.recommendations(),
            result.evidenceReferences(),
            List.of(
                new io.github.maynowak.drmays.core.dna.ResourceEdge(
                    otherResource,
                    otherResource,
                    io.github.maynowak.drmays.core.dna.RelationshipType.SECURITY_GROUP_IN_VPC,
                    new io.github.maynowak.drmays.core.dna.RelationshipEvidence(
                        new io.github.maynowak.drmays.core.evidence.Evidence(
                            otherResource, "test", "a", "b", java.util.Map.of())))),
            result.uncertainties());

    assertThat(validator.validate(request(), invalid).status())
        .isEqualTo(ModelExecutionStatus.INVALID_RESPONSE);
  }

  @Test
  void rejectsDuplicateExplanation() {
    var result = new MockIntelligenceAnalyzer().analyze(request());
    var invalid =
        new IntelligenceResult(
            result.status(),
            result.metadata(),
            result.summary(),
            List.of(result.explanations().getFirst(), result.explanations().getFirst()),
            result.recommendations(),
            result.evidenceReferences(),
            result.verifiedRelationships(),
            result.uncertainties());

    assertThat(validator.validate(request(), invalid).status())
        .isEqualTo(ModelExecutionStatus.INVALID_RESPONSE);
  }

  @Test
  void rejectsBlankSummary() {
    var result = new MockIntelligenceAnalyzer().analyze(request());
    var invalid =
        new IntelligenceResult(
            result.status(),
            result.metadata(),
            "",
            result.explanations(),
            result.recommendations(),
            result.evidenceReferences(),
            result.verifiedRelationships(),
            result.uncertainties());

    assertThat(validator.validate(request(), invalid).status())
        .isEqualTo(ModelExecutionStatus.INVALID_RESPONSE);
  }

  @Test
  void passesThroughNonSuccessStatus() {
    var failed = IntelligenceResult.failed(metadata);

    assertThat(validator.validate(request(), failed)).isSameAs(failed);
  }
}
