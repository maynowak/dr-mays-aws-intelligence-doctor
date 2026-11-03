package io.github.maynowak.drmays.api.v1;

import jakarta.validation.Valid;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Local, unauthenticated AI explanation endpoint. */
@RestController
@RequestMapping(path = "/api/v1/intelligence", produces = MediaType.APPLICATION_JSON_VALUE)
public class IntelligenceController {

  private final IntelligenceService service;

  public IntelligenceController(IntelligenceService service) {
    this.service = service;
  }

  /**
   * Explains deterministic findings and verified DNA relationships.
   *
   * @param request synthetic resource request
   * @return structured AI explanation or explicit failure status
   */
  @PostMapping(path = "/explain", consumes = MediaType.APPLICATION_JSON_VALUE)
  public IntelligenceExplainResponse explain(
      @Valid @RequestBody IntelligenceExplainRequest request) {
    return service.explain(request);
  }
}
