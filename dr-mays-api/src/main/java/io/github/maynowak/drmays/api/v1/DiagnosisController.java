package io.github.maynowak.drmays.api.v1;

import jakarta.validation.Valid;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Versioned REST endpoints for deterministic diagnosis and rule discovery. */
@RestController
@RequestMapping(path = "/api/v1", produces = MediaType.APPLICATION_JSON_VALUE)
public class DiagnosisController {

  private final DiagnosisService service;

  public DiagnosisController(DiagnosisService service) {
    this.service = service;
  }

  /**
   * Runs the registered rules over a synthetic infrastructure snapshot.
   *
   * @param request validated snapshot
   * @return deterministic diagnosis result
   */
  @PostMapping(path = "/diagnoses", consumes = MediaType.APPLICATION_JSON_VALUE)
  public DiagnosisResponse diagnose(@Valid @RequestBody DiagnosisRequest request) {
    return service.diagnose(request);
  }

  /**
   * Lists the registered rules.
   *
   * @return registered rules in rule-ID order
   */
  @GetMapping("/rules")
  public RulesResponse rules() {
    return service.rules();
  }
}
