package io.github.maynowak.drmays.api.v1;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import java.util.List;

/**
 * Request for an offline Infrastructure DNA graph.
 *
 * @param resources synthetic resources, limited to 100
 * @param partialDiscovery whether the source discovery was incomplete
 */
public record GraphRequest(
    @NotEmpty @Size(max = 100) List<@Valid ResourceRequest> resources, Boolean partialDiscovery) {

  public GraphRequest {
    resources = resources == null ? List.of() : List.copyOf(resources);
    partialDiscovery = Boolean.TRUE.equals(partialDiscovery);
  }
}
