package io.github.maynowak.drmays.core.report;

import io.github.maynowak.drmays.core.validation.DomainChecks;
import java.util.Objects;

/**
 * Stable metadata for one deterministic Doctor Report.
 *
 * @param generator report generator identifier
 * @param mode execution mode, for example deterministic-local
 * @param diagnosisSource authoritative diagnosis source
 * @param inputHash deterministic SHA-256 fingerprint of the report input
 * @param rulesFingerprint deterministic SHA-256 fingerprint of the rule catalog
 * @param partialDiscovery whether the source snapshot was declared partial
 * @param resourceCount number of resources in the report scope
 */
public record DoctorReportMetadata(
    String generator,
    String mode,
    String diagnosisSource,
    String inputHash,
    String rulesFingerprint,
    boolean partialDiscovery,
    int resourceCount) {

  public DoctorReportMetadata {
    generator = DomainChecks.requireText(generator, "generator");
    mode = DomainChecks.requireText(mode, "mode");
    diagnosisSource = DomainChecks.requireText(diagnosisSource, "diagnosisSource");
    inputHash = DomainChecks.requireText(inputHash, "inputHash");
    rulesFingerprint = DomainChecks.requireText(rulesFingerprint, "rulesFingerprint");
    if (resourceCount < 0) {
      throw new IllegalArgumentException("resourceCount must not be negative");
    }
    Objects.requireNonNull(inputHash, "inputHash");
  }
}
