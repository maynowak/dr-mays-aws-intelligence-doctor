package io.github.maynowak.drmays.api.v1;

/**
 * Evidence reference in an intelligence response.
 *
 * @param ruleId finding rule
 * @param resource finding resource
 * @param source evidence source
 */
public record EvidenceReferenceResponse(String ruleId, ResourceResponse resource, String source) {}
