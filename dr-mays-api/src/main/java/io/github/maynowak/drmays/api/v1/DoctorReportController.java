package io.github.maynowak.drmays.api.v1;

import jakarta.validation.Valid;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Local, deterministic Doctor Report endpoints. No provider or AWS call is made. */
@RestController
@RequestMapping(path = "/api/v1/reports")
public class DoctorReportController {

  private static final MediaType MARKDOWN = MediaType.parseMediaType("text/markdown;charset=UTF-8");

  private final DoctorReportService service;

  public DoctorReportController(DoctorReportService service) {
    this.service = service;
  }

  /**
   * Returns the structured JSON Doctor Report.
   *
   * @param request validated synthetic snapshot
   * @return bounded JSON report
   */
  @PostMapping(
      consumes = MediaType.APPLICATION_JSON_VALUE,
      produces = MediaType.APPLICATION_JSON_VALUE)
  public ResponseEntity<byte[]> report(@Valid @RequestBody DoctorReportRequest request) {
    return response(MediaType.APPLICATION_JSON, service.reportJson(request));
  }

  /**
   * Returns the Markdown Doctor Report.
   *
   * @param request validated synthetic snapshot
   * @return bounded Markdown report
   */
  @PostMapping(
      path = "/markdown",
      consumes = MediaType.APPLICATION_JSON_VALUE,
      produces = "text/markdown;charset=UTF-8")
  public ResponseEntity<byte[]> markdown(@Valid @RequestBody DoctorReportRequest request) {
    return response(MARKDOWN, service.reportMarkdown(request));
  }

  private static ResponseEntity<byte[]> response(MediaType type, byte[] body) {
    return ResponseEntity.ok()
        .contentType(type)
        .header("Cache-Control", "no-store")
        .header("X-Content-Type-Options", "nosniff")
        .body(body);
  }
}
