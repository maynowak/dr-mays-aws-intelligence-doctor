package io.github.maynowak.drmays.api.v1;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import java.util.List;

/**
 * Request for one deterministic diagnosis run.
 *
 * @param resources synthetic infrastructure snapshot, limited to 100 resources
 */
public record DiagnosisRequest(@NotEmpty @Size(max = 100) List<@Valid ResourceRequest> resources) {

  public DiagnosisRequest {
    resources = resources == null ? List.of() : List.copyOf(resources);
  }
}
