package io.github.maynowak.drmays.api.v1;

/**
 * Wire metadata for a Doctor Report.
 *
 * @param generator report generator
 * @param mode execution mode
 * @param diagnosisSource authoritative diagnosis source
 * @param inputHash deterministic input fingerprint
 * @param rulesFingerprint deterministic rule catalog fingerprint
 * @param partialDiscovery whether discovery was declared partial
 * @param resourceCount reviewed resource count
 */
public record DoctorReportMetadataResponse(
    String generator,
    String mode,
    String diagnosisSource,
    String inputHash,
    String rulesFingerprint,
    boolean partialDiscovery,
    int resourceCount) {}
