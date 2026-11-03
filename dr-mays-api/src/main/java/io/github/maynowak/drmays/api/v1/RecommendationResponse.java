package io.github.maynowak.drmays.api.v1;

/**
 * Suggested action in an intelligence response.
 *
 * @param title short title
 * @param action concrete action
 * @param severity priority
 */
public record RecommendationResponse(String title, String action, String severity) {}
