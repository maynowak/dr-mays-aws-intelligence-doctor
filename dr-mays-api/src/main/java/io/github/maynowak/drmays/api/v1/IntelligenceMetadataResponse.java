package io.github.maynowak.drmays.api.v1;

/**
 * Provider/model metadata in an intelligence response.
 *
 * @param provider provider identifier
 * @param model model identifier
 * @param version adapter version
 */
public record IntelligenceMetadataResponse(String provider, String model, String version) {}
