package io.github.maynowak.drmays.api.v1;

/**
 * One field decision in a privacy preview.
 *
 * @param field field name
 * @param classification sensitivity class
 * @param included whether populated
 * @param reason safe explanation
 */
public record ExpertiseFieldDecisionResponse(
    String field, String classification, boolean included, String reason) {}
