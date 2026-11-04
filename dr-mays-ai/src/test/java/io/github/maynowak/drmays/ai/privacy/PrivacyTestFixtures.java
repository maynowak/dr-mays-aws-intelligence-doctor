package io.github.maynowak.drmays.ai.privacy;

import io.github.maynowak.drmays.ai.IntelligenceRequest;
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
import tools.jackson.databind.ObjectMapper;

/** Deterministic privacy test data with raw identifiers that must never reach a payload. */
final class PrivacyTestFixtures {

  static final String ACCOUNT = "123456789012";
  static final String REGION = "eu-west-1";
  static final ResourceIdentity SG =
      new ResourceIdentity("aws", ACCOUNT, REGION, "security-group", "sg-0abc1234");
  static final ResourceIdentity VPC =
      new ResourceIdentity("aws", ACCOUNT, REGION, "vpc", "vpc-0def5678");
  static final RuleId RULE = new RuleId("NET-PUBLIC-001");
  static final byte[] KEY = new byte[32];

  private PrivacyTestFixtures() {}

  static DiagnosticFinding finding() {
    return finding(
        "vpc.id=vpc-0def5678 account=123456789012 owner=alice@corp.example "
            + "host=db.internal.example.com ip=203.0.113.7");
  }

  static DiagnosticFinding finding(String observed) {
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
            observed,
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

  static IntelligenceRequest requestWithObserved(String observed) {
    return new IntelligenceRequest(
        List.of(finding(observed)), List.of(relationship()), GraphCompleteness.COMPLETE);
  }

  static IntelligenceRequest partialRequest() {
    return new IntelligenceRequest(
        List.of(finding()), List.of(relationship()), GraphCompleteness.PARTIAL);
  }

  static PrivacyProcessingResult processed() {
    return processor(PrivacyPolicy.defaultPolicy()).process(request());
  }

  static PrivacyProcessingResult partialProcessed() {
    return processor(PrivacyPolicy.defaultPolicy()).process(partialRequest());
  }

  static PrivacyProcessor processor(PrivacyPolicy policy) {
    return new PrivacyProcessor(policy, new ObjectMapper(), () -> AliasMapping.withKey(KEY));
  }
}
