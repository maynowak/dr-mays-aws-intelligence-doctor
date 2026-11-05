package io.github.maynowak.drmays.api.v1;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.github.maynowak.drmays.core.diagnosis.DiagnosisEngine;
import io.github.maynowak.drmays.core.report.DoctorReport;
import io.github.maynowak.drmays.core.rule.builtin.MissingOwnerTagRule;
import io.github.maynowak.drmays.core.rule.builtin.PublicIngressRule;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.ObjectMapper;

class DoctorReportServiceTest {

  private final DiagnosisService diagnosisService =
      new DiagnosisService(
          new DiagnosisEngine(List.of(new MissingOwnerTagRule(), new PublicIngressRule())));
  private final DoctorReportService service =
      new DoctorReportService(diagnosisService, new ObjectMapper());

  @Test
  void buildsJsonReportFromOneDiagnosisSnapshot() {
    var report = service.build(request(false, List.of(publicNetwork())));
    var json = new String(service.reportJson(request(false, List.of(publicNetwork()))));

    assertThat(report.schemaVersion()).isEqualTo(DoctorReport.SCHEMA_VERSION);
    assertThat(report.summary().findingCount()).isEqualTo(2);
    assertThat(report.summary().highestSeverity()).isEqualTo("HIGH");
    assertThat(json).contains("\"schemaVersion\":\"doctor-report-v1\"");
    assertThat(json).contains("\"ruleId\":\"NET-PUBLIC-001\"");
    assertThat(json).contains("\"inputHash\":\"sha256:");
    assertThat(json).doesNotContain("approvalToken");
  }

  @Test
  void rendersMarkdownWithoutProviderCall() {
    var markdown = new String(service.reportMarkdown(request(false, List.of(publicNetwork()))));

    assertThat(markdown).startsWith("# Dr. Mays Infrastructure Doctor Report");
    assertThat(markdown).contains("## 5. Infrastructure DNA");
    assertThat(markdown).contains("No AI model was called");
    assertThat(markdown).contains("NET-PUBLIC-001");
    assertThat(markdown).doesNotContain("approvalToken");
  }

  @Test
  void partialDiscoveryIsExplicitInBothFormats() {
    var partial = request(true, List.of(publicNetwork()));
    var json = new String(service.reportJson(partial));
    var markdown = new String(service.reportMarkdown(partial));

    assertThat(json).contains("\"partialDiscovery\":true");
    assertThat(json).contains("\"completeness\":\"PARTIAL\"");
    assertThat(markdown).contains("PARTIAL DISCOVERY — RESULTS DO NOT COVER UNKNOWN RESOURCES");
  }

  @Test
  void outputLimitFailsClosed() {
    var resources = new ArrayList<ResourceRequest>();
    for (var index = 0; index < 100; index++) {
      var tags = new LinkedHashMap<String, String>();
      for (var tag = 0; tag < 50; tag++) {
        tags.put("tag-" + tag, "x".repeat(1024));
      }
      resources.add(
          new ResourceRequest(
              new ResourceIdentityRequest(
                  "aws", "000000000000", "eu-west-1", "storage", "store-" + index),
              tags,
              Map.of()));
    }
    var oversized = request(false, resources);

    assertThatThrownBy(() -> service.reportJson(oversized))
        .isInstanceOf(ReportLimitExceededException.class);
    assertThatThrownBy(() -> service.reportMarkdown(oversized))
        .isInstanceOf(ReportLimitExceededException.class);
  }

  private static DoctorReportRequest request(boolean partial, List<ResourceRequest> resources) {
    return new DoctorReportRequest(resources, partial);
  }

  private static ResourceRequest publicNetwork() {
    return new ResourceRequest(
        new ResourceIdentityRequest("aws", "000000000000", "eu-west-1", "network", "net-1"),
        Map.of(),
        Map.of("ingress.public", "true"));
  }
}
