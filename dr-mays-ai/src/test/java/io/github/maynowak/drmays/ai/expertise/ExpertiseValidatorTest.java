package io.github.maynowak.drmays.ai.expertise;

import static io.github.maynowak.drmays.ai.expertise.ExpertiseTestFixtures.RULE;
import static io.github.maynowak.drmays.ai.expertise.ExpertiseTestFixtures.SG;
import static io.github.maynowak.drmays.ai.expertise.ExpertiseTestFixtures.request;
import static org.assertj.core.api.Assertions.assertThat;

import io.github.maynowak.drmays.ai.EvidenceReference;
import io.github.maynowak.drmays.ai.FindingExplanation;
import io.github.maynowak.drmays.ai.ModelMetadata;
import io.github.maynowak.drmays.ai.Recommendation;
import io.github.maynowak.drmays.ai.provider.ProviderExecutionStatus;
import io.github.maynowak.drmays.core.dna.RelationshipEvidence;
import io.github.maynowak.drmays.core.dna.RelationshipType;
import io.github.maynowak.drmays.core.dna.ResourceEdge;
import io.github.maynowak.drmays.core.evidence.Evidence;
import io.github.maynowak.drmays.core.finding.Severity;
import io.github.maynowak.drmays.core.resource.ResourceIdentity;
import io.github.maynowak.drmays.core.rule.RuleId;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class ExpertiseValidatorTest {

  private final ExpertiseValidator validator = new ExpertiseValidator();
  private final ExpertiseProfile profile = ExpertiseProfile.standard();

  @Test
  void acceptsValidReport() {
    var report = ExpertiseTestFixtures.report();

    assertThat(validator.validate(request(), report, profile)).isSameAs(report);
  }

  @Test
  void rejectsUnknownFinding() {
    var unknown = new RuleId("UNKNOWN-001");
    var reference = new EvidenceReference(unknown, SG, "resource-attribute");
    var report =
        withExplanation(new FindingExplanation(unknown, SG, "unknown finding", List.of(reference)));

    assertThat(validator.validate(request(), report, profile).status())
        .isEqualTo(ProviderExecutionStatus.INVALID_RESPONSE);
  }

  @Test
  void rejectsUnknownEvidenceSource() {
    var reference = new EvidenceReference(RULE, SG, "invented-source");
    var report =
        withExplanation(new FindingExplanation(RULE, SG, "invented evidence", List.of(reference)));

    assertThat(validator.validate(request(), report, profile).status())
        .isEqualTo(ProviderExecutionStatus.INVALID_RESPONSE);
  }

  @Test
  void rejectsInventedRelationship() {
    var other = new ResourceIdentity("aws", "123456789012", "eu-west-1", "network", "other");
    var edge =
        new ResourceEdge(
            other,
            other,
            RelationshipType.SECURITY_GROUP_IN_VPC,
            new RelationshipEvidence(new Evidence(other, "test", "a", "b", Map.of())));
    var report =
        new ExpertiseReport(
            ProviderExecutionStatus.SUCCESS,
            new ModelMetadata("mock", "m", "1.0"),
            "summary",
            List.of("fact"),
            ExpertiseTestFixtures.report().explanations(),
            List.of(),
            List.of(),
            List.of(),
            ExpertiseTestFixtures.report().evidenceReferences(),
            List.of(edge),
            List.of(),
            ExpertiseTestFixtures.report().privacyReceipt());

    assertThat(validator.validate(request(), report, profile).status())
        .isEqualTo(ProviderExecutionStatus.INVALID_RESPONSE);
  }

  @Test
  void rejectsDuplicateExplanation() {
    var explanation = ExpertiseTestFixtures.report().explanations().getFirst();
    var report = withExplanations(List.of(explanation, explanation));

    assertThat(validator.validate(request(), report, profile).status())
        .isEqualTo(ProviderExecutionStatus.INVALID_RESPONSE);
  }

  @Test
  void rejectsUnsafeFreeText() {
    var reference = new EvidenceReference(RULE, SG, "resource-attribute");
    var report =
        withExplanation(
            new FindingExplanation(RULE, SG, "account 123456789012 is public", List.of(reference)));

    assertThat(validator.validate(request(), report, profile).status())
        .isEqualTo(ProviderExecutionStatus.INVALID_RESPONSE);
  }

  @Test
  void rejectsTooManyExplanationsForProfile() {
    var tiny = new ExpertiseProfile("tiny", 1, 1, 4_000);
    var explanation = ExpertiseTestFixtures.report().explanations().getFirst();
    var report = withExplanations(List.of(explanation, explanation));

    assertThat(validator.validate(request(), report, tiny).status())
        .isEqualTo(ProviderExecutionStatus.INVALID_RESPONSE);
  }

  @Test
  void rejectsTooLongRecommendation() {
    var recommendation = new Recommendation("title", "x".repeat(30), Severity.LOW);
    var tiny = new ExpertiseProfile("tiny", 10, 10, 10);
    var report =
        new ExpertiseReport(
            ProviderExecutionStatus.SUCCESS,
            new ModelMetadata("mock", "m", "1.0"),
            "summary",
            List.of("fact"),
            List.of(),
            List.of(recommendation),
            List.of(),
            List.of(),
            List.of(),
            List.of(),
            List.of(),
            ExpertiseTestFixtures.report().privacyReceipt());

    assertThat(validator.validate(request(), report, tiny).status())
        .isEqualTo(ProviderExecutionStatus.INVALID_RESPONSE);
  }

  private ExpertiseReport withExplanation(FindingExplanation explanation) {
    return withExplanations(List.of(explanation));
  }

  private ExpertiseReport withExplanations(List<FindingExplanation> explanations) {
    var base = ExpertiseTestFixtures.report();
    return new ExpertiseReport(
        base.status(),
        base.metadata(),
        base.summary(),
        base.facts(),
        explanations,
        base.recommendations(),
        base.assumptions(),
        base.missingInformation(),
        base.evidenceReferences(),
        base.verifiedRelationships(),
        base.uncertainties(),
        base.privacyReceipt());
  }
}
