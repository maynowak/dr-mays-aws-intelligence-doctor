package io.github.maynowak.drmays.core.report;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.github.maynowak.drmays.core.diagnosis.DiagnosisEngine;
import io.github.maynowak.drmays.core.diagnosis.DiagnosisResult;
import io.github.maynowak.drmays.core.dna.InfrastructureGraphBuilder;
import io.github.maynowak.drmays.core.evidence.Evidence;
import io.github.maynowak.drmays.core.finding.DiagnosticFinding;
import io.github.maynowak.drmays.core.finding.Severity;
import io.github.maynowak.drmays.core.resource.InfrastructureResource;
import io.github.maynowak.drmays.core.resource.ResourceIdentity;
import io.github.maynowak.drmays.core.rule.DiagnosticContext;
import io.github.maynowak.drmays.core.rule.DiagnosticOutcome;
import io.github.maynowak.drmays.core.rule.RuleId;
import io.github.maynowak.drmays.core.rule.builtin.MissingOwnerTagRule;
import io.github.maynowak.drmays.core.rule.builtin.PublicIngressRule;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class DoctorReportAssemblerTest {

  private static final DiagnosisEngine ENGINE =
      new DiagnosisEngine(List.of(new MissingOwnerTagRule(), new PublicIngressRule()));

  @Test
  void aggregatesExistingFindingsWithoutCreatingNewOnes() {
    var report = report(List.of(network("net-1", Map.of(), Map.of("ingress.public", "true"))));

    assertThat(report.schemaVersion()).isEqualTo(DoctorReport.SCHEMA_VERSION);
    assertThat(report.summary().findingCount()).isEqualTo(2);
    assertThat(report.summary().high()).isEqualTo(1);
    assertThat(report.summary().medium()).isEqualTo(1);
    assertThat(report.summary().highestSeverity()).isEqualTo("HIGH");
    assertThat(report.findings()).hasSize(2);
    assertThat(report.findings())
        .extracting(finding -> finding.ruleId().value())
        .containsExactly("NET-PUBLIC-001", "TAG-OWNER-001");
    assertThat(report.outcomes()).hasSize(2);
    assertThat(report.metadata().inputHash()).startsWith("sha256:");
    assertThat(report.metadata().rulesFingerprint()).startsWith("sha256:");
  }

  @Test
  void emptyFindingsRemainExplicitlyInconclusive() {
    var report = report(List.of(storage("store-1", Map.of("owner", "platform"), Map.of())));

    assertThat(report.findings()).isEmpty();
    assertThat(report.summary().findingCount()).isZero();
    assertThat(report.summary().highestSeverity()).isEqualTo("NONE");
    assertThat(report.outcomes()).hasSize(2);
    assertThat(report.outcomes())
        .extracting(DoctorReportOutcome::type)
        .containsExactlyInAnyOrder("NO_FINDING", "NOT_APPLICABLE");
    assertThat(report.limitations())
        .contains("Absence of a finding does not prove that the infrastructure is secure.");
  }

  @Test
  void preservesVerifiedGraphEdgesAndPartialDiscovery() {
    var report =
        report(
            List.of(network("sg-1", Map.of("owner", "platform"), Map.of("vpc.id", "vpc-1"))), true);

    assertThat(report.metadata().partialDiscovery()).isTrue();
    assertThat(report.graph().completeness().name()).isEqualTo("PARTIAL");
    assertThat(report.graph().nodeCount()).isEqualTo(2);
    assertThat(report.graph().edgeCount()).isEqualTo(1);
    assertThat(report.graph().referencedCount()).isEqualTo(1);
    assertThat(report.graph().graph().edges().getFirst().type().name())
        .isEqualTo("SECURITY_GROUP_IN_VPC");
    assertThat(report.graph().graph().referencedNodes().getFirst().identity().resourceId())
        .isEqualTo("vpc-1");
    assertThat(report.graph().issues())
        .extracting(issue -> issue.code().name())
        .contains("PARTIAL_DISCOVERY");
    assertThat(report.limitations())
        .contains("PARTIAL DISCOVERY — RESULTS DO NOT COVER UNKNOWN RESOURCES");
  }

  @Test
  void inputAndOutcomeOrderingDoNotDependOnRequestOrder() {
    var first =
        report(
            List.of(
                network("net-b", Map.of("owner", "platform"), Map.of("ingress.public", "true")),
                network("net-a", Map.of("owner", "platform"), Map.of())));
    var second =
        report(
            List.of(
                network("net-a", Map.of("owner", "platform"), Map.of()),
                network("net-b", Map.of("owner", "platform"), Map.of("ingress.public", "true"))));

    assertThat(first.metadata().inputHash()).isEqualTo(second.metadata().inputHash());
    assertThat(first.outcomes()).isEqualTo(second.outcomes());
    assertThat(first.findings()).isEqualTo(second.findings());
  }

  @Test
  void reportsCycleExistenceWithoutInventingCycleMembers() {
    var report =
        report(
            List.of(
                vpc("sg-1", Map.of("owner", "platform"), Map.of("vpc.id", "vpc-1")),
                vpc("vpc-1", Map.of("owner", "platform"), Map.of("vpc.id", "sg-1"))));

    assertThat(report.graph().cyclic()).isTrue();
    assertThat(report.limitations())
        .contains("The graph contains a directed cycle; participating nodes are not inferred.");
  }

  @Test
  void rejectsUnknownGraphCompleteness() {
    var resources = List.of(network("net-1", Map.of("owner", "platform"), Map.of()));
    var result = ENGINE.diagnose(new DiagnosticContext(resources));
    var graph =
        new io.github.maynowak.drmays.core.dna.GraphBuildResult(
            io.github.maynowak.drmays.core.dna.InfrastructureGraph.empty(),
            List.of(),
            io.github.maynowak.drmays.core.dna.GraphCompleteness.UNKNOWN);

    assertThatThrownBy(
            () -> DoctorReportAssembler.assemble(resources, result, ENGINE.rules(), graph, false))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("UNKNOWN");
  }

  @Test
  void rejectsInconsistentPartialDiscoveryFlag() {
    var resources = List.of(network("net-1", Map.of("owner", "platform"), Map.of()));
    var result = ENGINE.diagnose(new DiagnosticContext(resources));
    var graph = new InfrastructureGraphBuilder().build(resources, false);

    assertThatThrownBy(
            () -> DoctorReportAssembler.assemble(resources, result, ENGINE.rules(), graph, true))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("partialDiscovery");
  }

  @Test
  void markdownEscapesUntrustedTextAndIsDeterministic() {
    var identity = new ResourceIdentity("aws", "000000000000", "eu-west-1", "network", "net-1");
    var evidence =
        new Evidence(
            identity,
            "source",
            "observed\n# heading",
            "expected",
            Map.of("z", "<b>", "a", "|pipe|"));
    var finding =
        new DiagnosticFinding(
            new RuleId("NET-PUBLIC-001"),
            "1.0",
            Severity.HIGH,
            identity,
            "<script>alert(1)</script>",
            "[x](javascript:alert(1))",
            evidence,
            "Fix | now");
    var result =
        new DiagnosisResult(
            List.of(
                new DiagnosticOutcome.FindingDetected(finding),
                new DiagnosticOutcome.NoFinding(
                    new RuleId("TAG-OWNER-001"), identity, "owner tag present")));
    var resource = new InfrastructureResource(identity, Map.of(), Map.of());
    var graph = new InfrastructureGraphBuilder().build(List.of(resource));
    var report =
        DoctorReportAssembler.assemble(List.of(resource), result, ENGINE.rules(), graph, false);
    var renderer = new DoctorReportMarkdownRenderer();

    var first = renderer.render(report);
    var second = renderer.render(report);

    assertThat(first).isEqualTo(second);
    assertThat(first).contains("&lt;script&gt;");
    assertThat(first).doesNotContain("<script>");
    assertThat(first).doesNotContain("](javascript:");
    assertThat(first).contains("Fix \\| now");
    assertThat(first).contains("\\# heading");
    assertThat(first).contains("## 5. Infrastructure DNA");
  }

  private static DoctorReport report(List<InfrastructureResource> resources) {
    return report(resources, false);
  }

  private static DoctorReport report(
      List<InfrastructureResource> resources, boolean partialDiscovery) {
    var diagnosis = ENGINE.diagnose(new DiagnosticContext(resources));
    var graph = new InfrastructureGraphBuilder().build(resources, partialDiscovery);
    return DoctorReportAssembler.assemble(
        resources, diagnosis, ENGINE.rules(), graph, partialDiscovery);
  }

  private static InfrastructureResource network(
      String id, Map<String, String> tags, Map<String, String> attributes) {
    return new InfrastructureResource(
        new ResourceIdentity("aws", "000000000000", "eu-west-1", "network", id), tags, attributes);
  }

  private static InfrastructureResource vpc(
      String id, Map<String, String> tags, Map<String, String> attributes) {
    return new InfrastructureResource(
        new ResourceIdentity("aws", "000000000000", "eu-west-1", "vpc", id), tags, attributes);
  }

  private static InfrastructureResource storage(
      String id, Map<String, String> tags, Map<String, String> attributes) {
    return new InfrastructureResource(
        new ResourceIdentity("aws", "000000000000", "eu-west-1", "storage", id), tags, attributes);
  }
}
