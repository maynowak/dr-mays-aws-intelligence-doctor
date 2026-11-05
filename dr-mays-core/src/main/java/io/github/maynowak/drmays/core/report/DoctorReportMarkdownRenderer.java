package io.github.maynowak.drmays.core.report;

import io.github.maynowak.drmays.core.evidence.Evidence;
import io.github.maynowak.drmays.core.finding.DiagnosticFinding;
import io.github.maynowak.drmays.core.resource.ResourceIdentity;
import java.util.Map;
import java.util.Objects;
import java.util.TreeMap;

/** Renders a deterministic, injection-safe Markdown Doctor Report. */
public final class DoctorReportMarkdownRenderer {

  /**
   * Renders the report.
   *
   * @param report deterministic report
   * @return Markdown document
   */
  public String render(DoctorReport report) {
    Objects.requireNonNull(report, "report");
    var markdown = new StringBuilder(8_192);
    markdown.append("# Dr. Mays Infrastructure Doctor Report\n\n");
    markdown
        .append("**Schema:** ")
        .append(MarkdownEscaper.escape(report.schemaVersion()))
        .append("\n\n");
    markdown
        .append("**Generator:** ")
        .append(MarkdownEscaper.escape(report.metadata().generator()))
        .append("\n\n");
    markdown
        .append("**Mode:** ")
        .append(MarkdownEscaper.escape(report.metadata().mode()))
        .append("\n\n");
    markdown
        .append("**Diagnosis source:** ")
        .append(MarkdownEscaper.escape(report.metadata().diagnosisSource()))
        .append("\n\n");
    markdown
        .append("**Input hash:** ")
        .append(MarkdownEscaper.escape(report.metadata().inputHash()))
        .append("\n\n");
    markdown
        .append("**Rules fingerprint:** ")
        .append(MarkdownEscaper.escape(report.metadata().rulesFingerprint()))
        .append("\n\n");
    markdown.append("**Resources:** ").append(report.scope().resourceCount()).append("\n\n");

    if (report.metadata().partialDiscovery()) {
      markdown.append("> **PARTIAL DISCOVERY — RESULTS DO NOT COVER UNKNOWN RESOURCES**\n\n");
    }

    appendSummary(markdown, report);
    appendScope(markdown, report);
    appendFindings(markdown, report);
    appendGraph(markdown, report);
    appendOutcomes(markdown, report);
    appendRecommendations(markdown, report);
    appendLimitations(markdown, report);
    appendAppendix(markdown, report);
    return markdown.toString();
  }

  private static void appendSummary(StringBuilder markdown, DoctorReport report) {
    markdown.append("## 1. Executive Summary\n\n");
    var summary = report.summary();
    markdown
        .append("The deterministic diagnosis engine produced **")
        .append(summary.findingCount())
        .append(" finding(s)** across **")
        .append(summary.resourceCount())
        .append(" resource(s)**. Highest severity: **")
        .append(MarkdownEscaper.escape(summary.highestSeverity()))
        .append("**.\n\n");
    markdown.append("| Severity | Count |\n| --- | ---: |\n");
    markdown.append("| INFO | ").append(summary.info()).append(" |\n");
    markdown.append("| LOW | ").append(summary.low()).append(" |\n");
    markdown.append("| MEDIUM | ").append(summary.medium()).append(" |\n");
    markdown.append("| HIGH | ").append(summary.high()).append(" |\n");
    markdown.append("| CRITICAL | ").append(summary.critical()).append(" |\n\n");
    markdown.append(
        "No AI model was called. This report aggregates existing deterministic findings and "
            + "verified Infrastructure DNA data.\n\n");
  }

  private static void appendScope(StringBuilder markdown, DoctorReport report) {
    markdown.append("## 2. Scope & Completeness\n\n");
    markdown.append("- **Providers:** ").append(join(report.scope().providers())).append("\n");
    markdown.append("- **Accounts:** ").append(join(report.scope().accounts())).append("\n");
    markdown.append("- **Regions:** ").append(join(report.scope().regions())).append("\n");
    markdown
        .append("- **Inventoried resources:** ")
        .append(report.scope().resourceCount())
        .append("\n");
    markdown
        .append("- **Graph completeness:** ")
        .append(MarkdownEscaper.escape(report.graph().completeness().name()))
        .append("\n\n");
    if (report.metadata().partialDiscovery()) {
      markdown.append(
          "This snapshot is declared **partial**. Unknown resources and relationships are not "
              + "covered and must not be interpreted as absent.\n\n");
    } else {
      markdown.append(
          "Completeness is declared by the request. No live discovery verification was performed.\n\n");
    }
  }

  private static void appendFindings(StringBuilder markdown, DoctorReport report) {
    markdown.append("## 3. Findings Overview\n\n");
    if (report.findings().isEmpty()) {
      markdown.append(
          "No deterministic findings were produced. This does **not** prove that the infrastructure "
              + "is secure.\n\n");
      markdown.append("## 4. Detailed Findings\n\n");
      markdown.append("No detailed findings.\n\n");
      return;
    }
    markdown.append(
        "| # | Rule | Severity | Resource | Title |\n| ---: | --- | --- | --- | --- |\n");
    var index = 0;
    for (var finding : report.findings()) {
      index++;
      markdown
          .append("| ")
          .append(index)
          .append(" | ")
          .append(MarkdownEscaper.escape(finding.ruleId().value()))
          .append(" | ")
          .append(MarkdownEscaper.escape(finding.severity().name()))
          .append(" | ")
          .append(resource(finding.resource()))
          .append(" | ")
          .append(MarkdownEscaper.escape(finding.title()))
          .append(" |\n");
    }
    markdown.append("\n## 4. Detailed Findings\n\n");
    index = 0;
    for (var finding : report.findings()) {
      index++;
      appendFinding(markdown, index, finding);
    }
  }

  private static void appendFinding(StringBuilder markdown, int index, DiagnosticFinding finding) {
    markdown
        .append("### 4.")
        .append(index)
        .append(' ')
        .append(MarkdownEscaper.escape(finding.title()))
        .append("\n\n");
    markdown
        .append("- **Rule:** ")
        .append(MarkdownEscaper.escape(finding.ruleId().value()))
        .append(" (version ")
        .append(MarkdownEscaper.escape(finding.ruleVersion()))
        .append(")\n");
    markdown
        .append("- **Severity:** ")
        .append(MarkdownEscaper.escape(finding.severity().name()))
        .append("\n");
    markdown.append("- **Resource:** ").append(resource(finding.resource())).append("\n");
    markdown
        .append("- **Description:** ")
        .append(MarkdownEscaper.escape(finding.description()))
        .append("\n");
    appendEvidence(markdown, finding.evidence());
    markdown
        .append("- **Recommendation:** ")
        .append(MarkdownEscaper.escape(finding.recommendation()))
        .append("\n\n");
  }

  private static void appendEvidence(StringBuilder markdown, Evidence evidence) {
    markdown
        .append("- **Evidence source:** ")
        .append(MarkdownEscaper.escape(evidence.source()))
        .append("\n");
    markdown
        .append("- **Observed:** ")
        .append(MarkdownEscaper.escape(evidence.observed()))
        .append("\n");
    markdown
        .append("- **Expected:** ")
        .append(MarkdownEscaper.escape(evidence.expected()))
        .append("\n");
    if (!evidence.details().isEmpty()) {
      markdown.append("- **Evidence details:** ");
      var first = true;
      for (var entry : new TreeMap<>(evidence.details()).entrySet()) {
        if (!first) {
          markdown.append("; ");
        }
        first = false;
        markdown
            .append(MarkdownEscaper.escape(entry.getKey()))
            .append('=')
            .append(MarkdownEscaper.escape(entry.getValue()));
      }
      markdown.append("\n");
    }
  }

  private static void appendGraph(StringBuilder markdown, DoctorReport report) {
    markdown.append("## 5. Infrastructure DNA\n\n");
    var graph = report.graph();
    markdown.append("- **Nodes:** ").append(graph.nodeCount()).append("\n");
    markdown.append("- **Edges:** ").append(graph.edgeCount()).append("\n");
    markdown.append("- **Inventoried nodes:** ").append(graph.inventoriedCount()).append("\n");
    markdown.append("- **Referenced nodes:** ").append(graph.referencedCount()).append("\n");
    markdown.append("- **Connected components:** ").append(graph.componentCount()).append("\n");
    markdown.append("- **Directed cycle:** ").append(graph.cyclic() ? "yes" : "no").append("\n");
    markdown
        .append("- **Completeness:** ")
        .append(MarkdownEscaper.escape(graph.completeness().name()))
        .append("\n\n");

    markdown.append("### Inventoried resources\n\n");
    if (graph.graph().inventoriedNodes().isEmpty()) {
      markdown.append("No inventoried resources.\n\n");
    } else {
      for (var node : graph.graph().inventoriedNodes()) {
        markdown.append("- **").append(resource(node.identity())).append("**\n");
        appendMap(markdown, "Tags", node.tags());
        appendMap(markdown, "Attributes", node.attributes());
      }
      markdown.append("\n");
    }

    markdown.append("### Referenced resources\n\n");
    if (graph.graph().referencedNodes().isEmpty()) {
      markdown.append("No referenced-only resources.\n\n");
    } else {
      markdown.append(
          "Referenced resources are **not inventoried**; their metadata is unavailable.\n\n");
      for (var node : graph.graph().referencedNodes()) {
        markdown.append("- **").append(resource(node.identity())).append("**\n");
      }
      markdown.append("\n");
    }

    markdown.append("### Verified relationships\n\n");
    if (graph.graph().edges().isEmpty()) {
      markdown.append("No verified relationships.\n\n");
    } else {
      markdown.append("| From | To | Type | Evidence |\n| --- | --- | --- | --- |\n");
      for (var edge : graph.graph().edges()) {
        markdown
            .append("| ")
            .append(resource(edge.from()))
            .append(" | ")
            .append(resource(edge.to()))
            .append(" | ")
            .append(MarkdownEscaper.escape(edge.type().name()))
            .append(" | ")
            .append(MarkdownEscaper.escape(edge.evidence().evidence().source()))
            .append(": ")
            .append(MarkdownEscaper.escape(edge.evidence().evidence().observed()))
            .append(" |\n");
      }
      markdown.append("\n");
    }

    markdown.append("### Graph validation issues\n\n");
    if (graph.issues().isEmpty()) {
      markdown.append("No graph validation issues.\n\n");
    } else {
      for (var issue : graph.issues()) {
        markdown
            .append("- **")
            .append(MarkdownEscaper.escape(issue.code().name()))
            .append(":** ")
            .append(MarkdownEscaper.escape(issue.message()))
            .append("\n");
      }
      markdown.append("\n");
    }
  }

  private static void appendMap(StringBuilder markdown, String label, Map<String, String> values) {
    if (values.isEmpty()) {
      return;
    }
    markdown.append("  - **").append(label).append(":** ");
    var first = true;
    for (var entry : new TreeMap<>(values).entrySet()) {
      if (!first) {
        markdown.append("; ");
      }
      first = false;
      markdown
          .append(MarkdownEscaper.escape(entry.getKey()))
          .append('=')
          .append(MarkdownEscaper.escape(entry.getValue()));
    }
    markdown.append("\n");
  }

  private static void appendOutcomes(StringBuilder markdown, DoctorReport report) {
    markdown.append("## 6. Rule Outcomes\n\n");
    if (report.outcomes().isEmpty()) {
      markdown.append("No rule outcomes.\n\n");
      return;
    }
    markdown.append("| Rule | Version | Resource | Outcome | Reason |\n");
    markdown.append("| --- | --- | --- | --- | --- |\n");
    for (var outcome : report.outcomes()) {
      markdown
          .append("| ")
          .append(MarkdownEscaper.escape(outcome.ruleId()))
          .append(" | ")
          .append(MarkdownEscaper.escape(outcome.ruleVersion()))
          .append(" | ")
          .append(resource(outcome.resource()))
          .append(" | ")
          .append(MarkdownEscaper.escape(outcome.type()))
          .append(" | ")
          .append(outcome.reason() == null ? "" : MarkdownEscaper.escape(outcome.reason()))
          .append(" |\n");
    }
    markdown.append("\n");
  }

  private static void appendRecommendations(StringBuilder markdown, DoctorReport report) {
    markdown.append("## 7. Recommendations\n\n");
    if (report.findings().isEmpty()) {
      markdown.append("No deterministic recommendations were produced.\n\n");
      return;
    }
    for (var finding : report.findings()) {
      markdown
          .append("- **")
          .append(MarkdownEscaper.escape(finding.ruleId().value()))
          .append(":** ")
          .append(MarkdownEscaper.escape(finding.recommendation()))
          .append("\n");
    }
    markdown.append("\n");
  }

  private static void appendLimitations(StringBuilder markdown, DoctorReport report) {
    markdown.append("## 8. Missing Information & Limitations\n\n");
    for (var limitation : report.limitations()) {
      markdown.append("- ").append(MarkdownEscaper.escape(limitation)).append("\n");
    }
    markdown.append("\n");
  }

  private static void appendAppendix(StringBuilder markdown, DoctorReport report) {
    markdown.append("## 9. Technical Appendix\n\n");
    markdown.append("| Rule | Version | Description |\n| --- | --- | --- |\n");
    for (var rule : report.rules()) {
      markdown
          .append("| ")
          .append(MarkdownEscaper.escape(rule.ruleId()))
          .append(" | ")
          .append(MarkdownEscaper.escape(rule.version()))
          .append(" | ")
          .append(MarkdownEscaper.escape(rule.description()))
          .append(" |\n");
    }
    markdown.append("\n");
  }

  private static String resource(ResourceIdentity identity) {
    return MarkdownEscaper.escape(
        String.join(
            " / ",
            identity.provider(),
            identity.accountId(),
            identity.region(),
            identity.resourceType(),
            identity.resourceId()));
  }

  private static String join(java.util.List<String> values) {
    if (values.isEmpty()) {
      return "none";
    }
    return values.stream()
        .map(MarkdownEscaper::escape)
        .reduce((left, right) -> left + ", " + right)
        .orElse("none");
  }
}
