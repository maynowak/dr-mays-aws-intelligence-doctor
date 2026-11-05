package io.github.maynowak.drmays.ai.expertise;

import io.github.maynowak.drmays.ai.EvidenceReference;
import io.github.maynowak.drmays.ai.FindingExplanation;
import io.github.maynowak.drmays.ai.IntelligenceRequest;
import io.github.maynowak.drmays.ai.ModelMetadata;
import io.github.maynowak.drmays.ai.Recommendation;
import io.github.maynowak.drmays.ai.privacy.ApprovalStatus;
import io.github.maynowak.drmays.core.dna.GraphCompleteness;
import io.github.maynowak.drmays.core.dna.RelationshipEvidence;
import io.github.maynowak.drmays.core.dna.RelationshipType;
import io.github.maynowak.drmays.core.dna.ResourceEdge;
import io.github.maynowak.drmays.core.evidence.Evidence;
import io.github.maynowak.drmays.core.finding.DiagnosticFinding;
import io.github.maynowak.drmays.core.finding.Severity;
import io.github.maynowak.drmays.core.resource.ResourceIdentity;
import io.github.maynowak.drmays.core.rule.RuleId;
import java.util.List;
import java.util.Map;

/** Shared deterministic expertise test data. */
final class ExpertiseTestFixtures {

  static final String ACCOUNT = "123456789012";
  static final ResourceIdentity SG =
      new ResourceIdentity("aws", ACCOUNT, "eu-west-1", "security-group", "sg-0abc1234");
  static final ResourceIdentity VPC =
      new ResourceIdentity("aws", ACCOUNT, "eu-west-1", "vpc", "vpc-0def5678");
  static final RuleId RULE = new RuleId("NET-PUBLIC-001");

  private ExpertiseTestFixtures() {}

  static DiagnosticFinding finding() {
    return new DiagnosticFinding(
        RULE,
        "1.0",
        Severity.HIGH,
        SG,
        "Public ingress",
        "The security group declares public ingress.",
        new Evidence(
            SG,
            "resource-attribute",
            "vpc.id=vpc-0def5678",
            "ingress.public=false",
            Map.of("attribute", "ingress.public")),
        "Restrict ingress.");
  }

  static ResourceEdge relationship() {
    return new ResourceEdge(
        SG,
        VPC,
        RelationshipType.SECURITY_GROUP_IN_VPC,
        new RelationshipEvidence(
            new Evidence(
                SG,
                "resource-attribute",
                "vpc.id=vpc-0def5678",
                "vpc.id",
                Map.of("attribute", "vpc.id"))));
  }

  static IntelligenceRequest request() {
    return new IntelligenceRequest(
        List.of(finding()), List.of(relationship()), GraphCompleteness.COMPLETE);
  }

  static IntelligenceRequest partialRequest() {
    return new IntelligenceRequest(
        List.of(finding()), List.of(relationship()), GraphCompleteness.PARTIAL);
  }

  static ExpertiseReport report() {
    var reference = new EvidenceReference(RULE, SG, "resource-attribute");
    return new ExpertiseReport(
        io.github.maynowak.drmays.ai.provider.ProviderExecutionStatus.SUCCESS,
        new ModelMetadata("mock", "deterministic-v1", "1.0"),
        "One deterministic finding was explained.",
        List.of("fact"),
        List.of(new FindingExplanation(RULE, SG, "The finding is HIGH.", List.of(reference))),
        List.of(new Recommendation("Review", "Apply the remediation.", Severity.HIGH)),
        List.of(),
        List.of(),
        List.of(reference),
        List.of(relationship()),
        List.of(),
        new PrivacyReceipt(
            "privacy-v2",
            "hash",
            ApprovalStatus.NOT_REQUIRED,
            "mock",
            "deterministic-v1",
            "standard"));
  }
}
