package io.github.maynowak.drmays.api.v1;

import io.github.maynowak.drmays.core.dna.InfrastructureGraphBuilder;
import io.github.maynowak.drmays.core.report.DoctorReport;
import io.github.maynowak.drmays.core.report.DoctorReportAssembler;
import io.github.maynowak.drmays.core.report.DoctorReportMarkdownRenderer;
import java.nio.charset.StandardCharsets;
import org.springframework.stereotype.Service;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;

/** Aggregates existing diagnosis and DNA results into deterministic Doctor Reports. */
@Service
public class DoctorReportService {

  static final int MAX_JSON_BYTES = 1_048_576;
  static final int MAX_MARKDOWN_BYTES = 262_144;

  private final DiagnosisService diagnosisService;
  private final ObjectMapper objectMapper;
  private final DoctorReportMarkdownRenderer markdownRenderer = new DoctorReportMarkdownRenderer();

  /**
   * Creates the service.
   *
   * @param diagnosisService existing deterministic diagnosis service
   * @param objectMapper Jackson 3 mapper for bounded JSON output
   */
  public DoctorReportService(DiagnosisService diagnosisService, ObjectMapper objectMapper) {
    this.diagnosisService = diagnosisService;
    this.objectMapper = objectMapper;
  }

  /**
   * Builds the report from one diagnosis run and one graph build.
   *
   * @param request validated report request
   * @return deterministic report
   */
  DoctorReport build(DoctorReportRequest request) {
    var diagnosisRequest = new DiagnosisRequest(request.resources());
    var snapshot = diagnosisService.diagnoseSnapshot(diagnosisRequest);
    var graphBuild =
        new InfrastructureGraphBuilder().build(snapshot.resources(), request.partialDiscovery());
    return DoctorReportAssembler.assemble(
        snapshot.resources(),
        snapshot.result(),
        snapshot.rules(),
        graphBuild,
        request.partialDiscovery());
  }

  /**
   * Serializes the JSON report and enforces the output limit.
   *
   * @param request validated report request
   * @return bounded JSON bytes
   */
  public byte[] reportJson(DoctorReportRequest request) {
    try {
      var bytes = objectMapper.writeValueAsBytes(DoctorReportDtoMapper.map(build(request)));
      if (bytes.length > MAX_JSON_BYTES) {
        throw new ReportLimitExceededException();
      }
      return bytes;
    } catch (JacksonException exception) {
      throw new IllegalStateException("report serialization failed", exception);
    }
  }

  /**
   * Renders the Markdown report and enforces the output limit.
   *
   * @param request validated report request
   * @return bounded Markdown bytes
   */
  public byte[] reportMarkdown(DoctorReportRequest request) {
    var markdown = markdownRenderer.render(build(request));
    var bytes = markdown.getBytes(StandardCharsets.UTF_8);
    if (bytes.length > MAX_MARKDOWN_BYTES) {
      throw new ReportLimitExceededException();
    }
    return bytes;
  }
}
