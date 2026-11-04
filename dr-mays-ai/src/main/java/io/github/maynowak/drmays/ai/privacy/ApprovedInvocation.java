package io.github.maynowak.drmays.ai.privacy;

import io.github.maynowak.drmays.ai.IntelligenceRequest;
import io.github.maynowak.drmays.core.validation.DomainChecks;
import java.util.Objects;

/**
 * Sealed, approved invocation material. The source request stays local.
 *
 * @param source original verified request for final validation
 * @param processed sanitized payload and alias map
 * @param providerId approved provider
 * @param modelId approved model
 * @param expertiseId approved expertise profile
 */
public record ApprovedInvocation(
    IntelligenceRequest source,
    PrivacyProcessingResult processed,
    String providerId,
    String modelId,
    String expertiseId) {

  public ApprovedInvocation {
    source = Objects.requireNonNull(source, "source");
    processed = Objects.requireNonNull(processed, "processed");
    providerId = DomainChecks.requireText(providerId, "providerId");
    modelId = DomainChecks.requireText(modelId, "modelId");
    expertiseId = DomainChecks.requireText(expertiseId, "expertiseId");
  }
}
