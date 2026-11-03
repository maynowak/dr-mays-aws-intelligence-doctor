package io.github.maynowak.drmays.api.v1;

import jakarta.validation.Valid;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Offline Infrastructure DNA graph endpoint. */
@RestController
@RequestMapping(path = "/api/v1/dna", produces = MediaType.APPLICATION_JSON_VALUE)
public class DnaController {

  private final GraphService service;

  public DnaController(GraphService service) {
    this.service = service;
  }

  /**
   * Builds a graph from synthetic resources.
   *
   * @param request validated graph request
   * @return nodes, edges, evidence, issues and statistics
   */
  @PostMapping(path = "/graphs", consumes = MediaType.APPLICATION_JSON_VALUE)
  public GraphResponse graph(@Valid @RequestBody GraphRequest request) {
    return service.graph(request);
  }
}
