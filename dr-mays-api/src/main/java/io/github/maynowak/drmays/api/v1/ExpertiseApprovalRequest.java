package io.github.maynowak.drmays.api.v1;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Approval request for an existing preview.
 *
 * @param previewId opaque preview handle
 */
public record ExpertiseApprovalRequest(@NotBlank @Size(max = 128) String previewId) {}
