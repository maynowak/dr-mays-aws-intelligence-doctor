package io.github.maynowak.drmays.api.v1;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.List;

/**
 * Request for one deterministic Doctor Report.
 *
 * @param resources synthetic infrastructure snapshot, limited to 100 resources
 * @param partialDiscovery whether the source discovery was incomplete
 */
public record DoctorReportRequest(
    @NotEmpty @Size(max = 100) List<@Valid ResourceRequest> resources,
    @NotNull Boolean partialDiscovery) {

  public DoctorReportRequest {
    resources = resources == null ? List.of() : List.copyOf(resources);
    if (partialDiscovery == null) {
      throw new IllegalArgumentException("partialDiscovery is required");
    }
  }
}
