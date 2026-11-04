package io.github.maynowak.drmays.api.v1;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import java.util.List;

/**
 * Request for a privacy preview of a provider payload.
 *
 * @param resources synthetic resources, limited to 100
 * @param partialDiscovery whether discovery was incomplete
 * @param providerId selected provider, default mock
 * @param modelId selected model, default deterministic-v1
 * @param expertiseId selected expertise profile, default standard
 */
public record ExpertisePreviewRequest(
    @NotEmpty @Size(max = 100) List<@Valid ResourceRequest> resources,
    Boolean partialDiscovery,
    @Size(max = 32) String providerId,
    @Size(max = 128) String modelId,
    @Size(max = 32) String expertiseId) {

  public ExpertisePreviewRequest {
    resources = resources == null ? List.of() : List.copyOf(resources);
    partialDiscovery = Boolean.TRUE.equals(partialDiscovery);
    providerId = providerId == null || providerId.isBlank() ? "mock" : providerId;
    modelId = modelId == null || modelId.isBlank() ? "deterministic-v1" : modelId;
    expertiseId = expertiseId == null || expertiseId.isBlank() ? "standard" : expertiseId;
  }
}
