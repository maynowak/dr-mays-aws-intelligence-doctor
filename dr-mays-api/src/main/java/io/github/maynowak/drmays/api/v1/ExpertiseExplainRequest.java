package io.github.maynowak.drmays.api.v1;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Execution request for an already approved preview.
 *
 * @param previewId opaque preview handle
 * @param approvalToken optional single-use token; required for remote providers
 */
public record ExpertiseExplainRequest(
    @NotBlank @Size(max = 128) String previewId, @Size(max = 128) String approvalToken) {

  @Override
  public String toString() {
    return "ExpertiseExplainRequest[previewId=" + previewId + ", approvalToken=redacted]";
  }
}
