package io.github.maynowak.drmays.api.v1;

/**
 * Wire representation of a directed relationship.
 *
 * @param from source resource
 * @param to target resource
 * @param type relationship type
 * @param evidence relationship provenance
 */
public record GraphEdgeResponse(
    ResourceResponse from, ResourceResponse to, String type, EvidenceResponse evidence) {}
