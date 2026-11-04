package io.github.maynowak.drmays.ai;

import java.util.Objects;

/**
 * Fail-closed legacy analyzer. Raw legacy explanation requests must not reach an external model;
 * callers must use preview/approval on the expertise endpoints instead.
 */
public final class PrivacyGatedLegacyAnalyzer implements IntelligenceAnalyzer {

  private final ModelMetadata metadata;

  /**
   * Creates the guard with provider/model metadata.
   *
   * @param metadata non-secret provider metadata
   */
  public PrivacyGatedLegacyAnalyzer(ModelMetadata metadata) {
    this.metadata = Objects.requireNonNull(metadata, "metadata");
  }

  @Override
  public IntelligenceResult analyze(IntelligenceRequest request) {
    return IntelligenceResult.approvalRequired(metadata);
  }
}
