package io.github.maynowak.drmays.ai;

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

/** Shared deterministic AI test data. */
final class AiTestFixtures {

  static final String ACCOUNT = "000000000000";
  static final String REGION = "eu-west-1";
  static final ResourceIdentity RESOURCE =
      new ResourceIdentity("aws", ACCOUNT, REGION, "network", "net-1");
  static final ResourceIdentity VPC = new ResourceIdentity("aws", ACCOUNT, REGION, "vpc", "vpc-1");
  static final RuleId RULE = new RuleId("NET-PUBLIC-001");

  private AiTestFixtures() {}

  static DiagnosticFinding finding() {
    return finding(RULE, RESOURCE, Severity.HIGH);
  }

  static DiagnosticFinding finding(RuleId rule, ResourceIdentity resource, Severity severity) {
    return new DiagnosticFinding(
        rule,
        "1.0",
        severity,
        resource,
        "Public ingress",
        "The resource declares public ingress.",
        new Evidence(
            resource,
            "resource-attribute",
            "ingress.public=true",
            "ingress.public=false",
            Map.of("attribute", "ingress.public")),
        "Restrict ingress.");
  }

  static ResourceEdge relationship() {
    return new ResourceEdge(
        RESOURCE,
        VPC,
        RelationshipType.SECURITY_GROUP_IN_VPC,
        new RelationshipEvidence(
            new Evidence(
                RESOURCE,
                "resource-attribute",
                "vpc.id=vpc-1",
                "vpc.id",
                Map.of("attribute", "vpc.id"))));
  }

  static IntelligenceRequest request() {
    return new IntelligenceRequest(
        List.of(finding()), List.of(relationship()), GraphCompleteness.COMPLETE);
  }
}
