package io.github.maynowak.drmays.ai.privacy;

import io.github.maynowak.drmays.ai.IntelligenceRequest;
import io.github.maynowak.drmays.core.validation.DomainChecks;
import java.util.Objects;

/**
 * Sealed, approved invocation material. The source request, gate-owned approval decision and
 * server-minted authorization stay local and are not publicly constructible.
 */
public final class ApprovedInvocation {

  private final IntelligenceRequest source;
  private final PrivacyProcessingResult processed;
  private final String providerId;
  private final String modelId;
  private final String expertiseId;
  private final boolean approvalRequired;
  private final ProviderInvocationAuthorization authorization;

  ApprovedInvocation(
      IntelligenceRequest source,
      PrivacyProcessingResult processed,
      String providerId,
      String modelId,
      String expertiseId,
      boolean approvalRequired,
      ProviderInvocationAuthorization authorization) {
    this.source = Objects.requireNonNull(source, "source");
    this.processed = Objects.requireNonNull(processed, "processed");
    this.providerId = DomainChecks.requireText(providerId, "providerId");
    this.modelId = DomainChecks.requireText(modelId, "modelId");
    this.expertiseId = DomainChecks.requireText(expertiseId, "expertiseId");
    this.approvalRequired = approvalRequired;
    this.authorization = Objects.requireNonNull(authorization, "authorization");
  }

  public IntelligenceRequest source() {
    return source;
  }

  public PrivacyProcessingResult processed() {
    return processed;
  }

  public String providerId() {
    return providerId;
  }

  public String modelId() {
    return modelId;
  }

  public String expertiseId() {
    return expertiseId;
  }

  public boolean approvalRequired() {
    return approvalRequired;
  }

  public ProviderInvocationAuthorization authorization() {
    return authorization;
  }

  @Override
  public String toString() {
    return "ApprovedInvocation[redacted]";
  }
}
