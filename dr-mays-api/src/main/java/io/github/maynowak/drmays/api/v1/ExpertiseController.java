package io.github.maynowak.drmays.api.v1;

import jakarta.validation.Valid;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Local, additive privacy preview and mock-only expertise endpoints. */
@RestController
@RequestMapping(path = "/api/v1/expertise", produces = MediaType.APPLICATION_JSON_VALUE)
public class ExpertiseController {

  private final ExpertiseService service;

  public ExpertiseController(ExpertiseService service) {
    this.service = service;
  }

  /**
   * Creates a sanitized preview; no provider is invoked.
   *
   * @param request preview request
   * @return safe preview
   */
  @PostMapping(path = "/previews", consumes = MediaType.APPLICATION_JSON_VALUE)
  public ExpertisePreviewResponse preview(@Valid @RequestBody ExpertisePreviewRequest request) {
    return service.preview(request);
  }

  /**
   * Approves an existing preview.
   *
   * @param request approval request
   * @return single-use token
   */
  @PostMapping(path = "/approvals", consumes = MediaType.APPLICATION_JSON_VALUE)
  public ExpertiseApprovalResponse approve(@Valid @RequestBody ExpertiseApprovalRequest request) {
    return service.approve(request);
  }

  /**
   * Executes a previously previewed request.
   *
   * @param request execution request
   * @return structured expertise
   */
  @PostMapping(path = "/explain", consumes = MediaType.APPLICATION_JSON_VALUE)
  public ExpertiseExplainResponse explain(@Valid @RequestBody ExpertiseExplainRequest request) {
    return service.explain(request);
  }
}
