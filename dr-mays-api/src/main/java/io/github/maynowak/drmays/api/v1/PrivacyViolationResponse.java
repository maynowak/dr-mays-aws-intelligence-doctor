package io.github.maynowak.drmays.api.v1;

/**
 * Safe privacy violation description.
 *
 * @param code stable issue code
 * @param field affected field
 * @param message safe explanation
 */
public record PrivacyViolationResponse(String code, String field, String message) {}
