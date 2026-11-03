package io.github.maynowak.drmays.api.v1;

/**
 * Wire representation of a graph issue.
 *
 * @param code stable issue code
 * @param message safe summary
 */
public record GraphIssueResponse(String code, String message) {}
