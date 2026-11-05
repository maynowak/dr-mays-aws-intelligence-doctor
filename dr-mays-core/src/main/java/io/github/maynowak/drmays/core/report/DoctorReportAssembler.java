package io.github.maynowak.drmays.core.report;

import io.github.maynowak.drmays.core.diagnosis.DiagnosisResult;
import io.github.maynowak.drmays.core.dna.GraphAnalyzer;
import io.github.maynowak.drmays.core.dna.GraphBuildResult;
import io.github.maynowak.drmays.core.dna.GraphCompleteness;
import io.github.maynowak.drmays.core.finding.DiagnosticFinding;
import io.github.maynowak.drmays.core.finding.Severity;
import io.github.maynowak.drmays.core.resource.InfrastructureResource;
import io.github.maynowak.drmays.core.rule.DiagnosticOutcome;
import io.github.maynowak.drmays.core.rule.DiagnosticRule;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

/**
 * Assembles a deterministic Doctor Report from one existing diagnosis result and one verified graph
 * build. It never evaluates a rule, adds a finding or creates a graph relationship.
 */
public final class DoctorReportAssembler {

  private DoctorReportAssembler() {}

  /**
   * Assembles a report from already verified domain results.
   *
   * @param resources reviewed resources
   * @param diagnosis diagnosis result from the existing engine
   * @param rules registered rule catalog
   * @param graphBuild verified graph build result
   * @param partialDiscovery whether the source snapshot is declared partial
   * @return deterministic report
   */
  public static DoctorReport assemble(
      List<InfrastructureResource> resources,
      DiagnosisResult diagnosis,
      List<DiagnosticRule> rules,
      GraphBuildResult graphBuild,
      boolean partialDiscovery) {
    Objects.requireNonNull(resources, "resources");
    Objects.requireNonNull(diagnosis, "diagnosis");
    Objects.requireNonNull(rules, "rules");
    Objects.requireNonNull(graphBuild, "graphBuild");
    if (partialDiscovery != (graphBuild.completeness() == GraphCompleteness.PARTIAL)) {
      throw new IllegalArgumentException("partialDiscovery must match graph completeness");
    }

    var orderedRules =
        rules.stream()
            .sorted(Comparator.comparing(rule -> rule.id().value()))
            .map(
                rule -> new DoctorReportRule(rule.id().value(), rule.version(), rule.description()))
            .toList();
    var ruleVersions = new TreeMap<String, String>();
    orderedRules.forEach(rule -> ruleVersions.put(rule.ruleId(), rule.version()));

    var findings = diagnosis.findings().stream().sorted(FINDING_ORDER).toList();
    var outcomes =
        diagnosis.outcomes().stream()
            .sorted(OUTCOME_ORDER)
            .map(outcome -> toOutcome(outcome, ruleVersions))
            .toList();
    assertScope(resources, orderedRules, findings, outcomes, graphBuild);
    var analyzer = new GraphAnalyzer(graphBuild.graph());
    var graph =
        new DoctorReportGraph(
            graphBuild.graph(),
            graphBuild.issues(),
            graphBuild.completeness(),
            analyzer.components().size(),
            analyzer.hasCycles());
    var scope = scope(resources);
    var summary = summary(resources.size(), findings, outcomes.size());
    var limitations = limitations(partialDiscovery, graph);
    var metadata =
        new DoctorReportMetadata(
            "dr-mays",
            "deterministic-local",
            "DiagnosisEngine",
            hashResources(resources, partialDiscovery),
            hashRules(orderedRules),
            partialDiscovery,
            resources.size());
    return new DoctorReport(
        DoctorReport.SCHEMA_VERSION,
        metadata,
        scope,
        summary,
        findings,
        outcomes,
        graph,
        orderedRules,
        limitations);
  }

  private static final Comparator<io.github.maynowak.drmays.core.resource.ResourceIdentity>
      RESOURCE_ORDER =
          Comparator.comparing(io.github.maynowak.drmays.core.resource.ResourceIdentity::provider)
              .thenComparing(io.github.maynowak.drmays.core.resource.ResourceIdentity::accountId)
              .thenComparing(io.github.maynowak.drmays.core.resource.ResourceIdentity::region)
              .thenComparing(io.github.maynowak.drmays.core.resource.ResourceIdentity::resourceType)
              .thenComparing(io.github.maynowak.drmays.core.resource.ResourceIdentity::resourceId);

  private static final Comparator<DiagnosticFinding> FINDING_ORDER =
      Comparator.comparing(DiagnosticFinding::resource, RESOURCE_ORDER)
          .thenComparing(finding -> finding.ruleId().value());

  private static final Comparator<DiagnosticOutcome> OUTCOME_ORDER =
      Comparator.comparing(DiagnosticOutcome::resource, RESOURCE_ORDER)
          .thenComparing(outcome -> outcome.ruleId().value())
          .thenComparing(DiagnosticOutcome::summary);

  private static void assertScope(
      List<InfrastructureResource> resources,
      List<DoctorReportRule> rules,
      List<DiagnosticFinding> findings,
      List<DoctorReportOutcome> outcomes,
      GraphBuildResult graphBuild) {
    if (graphBuild.completeness() == GraphCompleteness.UNKNOWN) {
      throw new IllegalArgumentException("UNKNOWN graph completeness cannot be reported");
    }
    var resourceIds =
        resources.stream()
            .map(InfrastructureResource::identity)
            .collect(HashSet::new, Set::add, Set::addAll);
    for (var finding : findings) {
      if (!resourceIds.contains(finding.resource())) {
        throw new IllegalArgumentException("finding resource is outside the report scope");
      }
    }
    for (var outcome : outcomes) {
      if (!resourceIds.contains(outcome.resource())) {
        throw new IllegalArgumentException("outcome resource is outside the report scope");
      }
    }
    if (outcomes.size() != resources.size() * rules.size()) {
      throw new IllegalArgumentException("outcome coverage does not match resources and rules");
    }
    for (var node : graphBuild.graph().nodes()) {
      if (node.inventoried() && !resourceIds.contains(node.identity())) {
        throw new IllegalArgumentException("inventoried node is outside the report scope");
      }
    }
  }

  private static DoctorReportOutcome toOutcome(
      DiagnosticOutcome outcome, Map<String, String> ruleVersions) {
    return switch (outcome) {
      case DiagnosticOutcome.FindingDetected detected -> {
        var finding = detected.finding();
        yield new DoctorReportOutcome(
            finding.ruleId().value(), finding.ruleVersion(), finding.resource(), "FINDING", null);
      }
      case DiagnosticOutcome.NoFinding noFinding ->
          new DoctorReportOutcome(
              noFinding.ruleId().value(),
              version(ruleVersions, noFinding.ruleId().value()),
              noFinding.resource(),
              "NO_FINDING",
              noFinding.reason());
      case DiagnosticOutcome.NotApplicable notApplicable ->
          new DoctorReportOutcome(
              notApplicable.ruleId().value(),
              version(ruleVersions, notApplicable.ruleId().value()),
              notApplicable.resource(),
              "NOT_APPLICABLE",
              notApplicable.reason());
    };
  }

  private static String version(Map<String, String> versions, String ruleId) {
    var version = versions.get(ruleId);
    if (version == null) {
      throw new IllegalStateException("no rule version for outcome rule " + ruleId);
    }
    return version;
  }

  private static DoctorReportScope scope(List<InfrastructureResource> resources) {
    var providers = new TreeSet<String>();
    var accounts = new TreeSet<String>();
    var regions = new TreeSet<String>();
    for (var resource : resources) {
      providers.add(resource.identity().provider());
      accounts.add(resource.identity().accountId());
      regions.add(resource.identity().region());
    }
    return new DoctorReportScope(
        List.copyOf(providers), List.copyOf(accounts), List.copyOf(regions), resources.size());
  }

  private static DoctorReportSummary summary(
      int resourceCount, List<DiagnosticFinding> findings, int outcomeCount) {
    var counts = new TreeMap<Severity, Integer>();
    for (var severity : Severity.values()) {
      counts.put(severity, 0);
    }
    for (var finding : findings) {
      counts.compute(finding.severity(), (key, value) -> value + 1);
    }
    var highest = "NONE";
    for (var severity : Severity.values()) {
      if (counts.get(severity) > 0) {
        highest = severity.name();
      }
    }
    return new DoctorReportSummary(
        resourceCount,
        findings.size(),
        outcomeCount,
        counts.get(Severity.INFO),
        counts.get(Severity.LOW),
        counts.get(Severity.MEDIUM),
        counts.get(Severity.HIGH),
        counts.get(Severity.CRITICAL),
        highest);
  }

  private static List<String> limitations(boolean partial, DoctorReportGraph graph) {
    var values = new ArrayList<String>();
    if (partial) {
      values.add("PARTIAL DISCOVERY — RESULTS DO NOT COVER UNKNOWN RESOURCES");
    }
    values.add("Completeness is declared by the request, not verified by a live discovery run.");
    values.add("Referenced resources are not inventoried; their metadata is unavailable.");
    values.add("Relationships are explicit resource references, not inferred reachability.");
    values.add("No AI provider was called; this report contains deterministic local data only.");
    values.add("The report contains local resource metadata; treat exported files as sensitive.");
    values.add("Absence of a finding does not prove that the infrastructure is secure.");
    if (graph.cyclic()) {
      values.add("The graph contains a directed cycle; participating nodes are not inferred.");
    }
    return List.copyOf(values);
  }

  private static String hashResources(List<InfrastructureResource> resources, boolean partial) {
    var canonical = new StringBuilder("partial=").append(partial).append('\n');
    resources.stream()
        .sorted(Comparator.comparing(resource -> resource.identity().canonicalId()))
        .forEach(resource -> {
          canonical.append("resource=").append(resource.identity().canonicalId()).append('\n');
          new TreeMap<>(resource.tags())
              .forEach((key, value) ->
                  canonical.append("tag:").append(key).append('=').append(value).append('\n'));
          new TreeMap<>(resource.attributes())
              .forEach((key, value) ->
                  canonical
                      .append("attribute:")
                      .append(key)
                      .append('=')
                      .append(value)
                      .append('\n'));
        });
    return sha256(canonical.toString());
  }

  private static String hashRules(List<DoctorReportRule> rules) {
    var canonical = new StringBuilder();
    rules.forEach(
        rule -> canonical.append(rule.ruleId()).append('@').append(rule.version()).append('\n'));
    return sha256(canonical.toString());
  }

  private static String sha256(String value) {
    try {
      var digest = MessageDigest.getInstance("SHA-256");
      return "sha256:"
          + HexFormat.of().formatHex(digest.digest(value.getBytes(StandardCharsets.UTF_8)));
    } catch (NoSuchAlgorithmException exception) {
      throw new IllegalStateException("SHA-256 is unavailable", exception);
    }
  }
}
