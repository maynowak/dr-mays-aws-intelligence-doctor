package io.github.maynowak.drmays.ai.privacy;

import io.github.maynowak.drmays.core.validation.DomainChecks;
import java.util.EnumSet;
import java.util.Objects;
import java.util.Set;

/**
 * Versioned allowlist and size limit for provider-facing privacy projection.
 *
 * @param version policy version included in approval bindings
 * @param allowedFields fields that may be populated in the sanitized payload
 * @param maxPayloadChars maximum serialized payload size
 */
public record PrivacyPolicy(String version, Set<PrivacyField> allowedFields, int maxPayloadChars) {

  public static final String DEFAULT_VERSION = "privacy-v1";
  public static final int DEFAULT_MAX_PAYLOAD_CHARS = 32_000;

  public PrivacyPolicy {
    version = DomainChecks.requireText(version, "version");
    Objects.requireNonNull(allowedFields, "allowedFields");
    allowedFields = Set.copyOf(allowedFields);
    if (allowedFields.isEmpty()) {
      throw new IllegalArgumentException("allowedFields must not be empty");
    }
    if (maxPayloadChars <= 0) {
      throw new IllegalArgumentException("maxPayloadChars must be positive");
    }
  }

  /**
   * Returns the default policy with every supported field allowed.
   *
   * @return default policy
   */
  public static PrivacyPolicy defaultPolicy() {
    return new PrivacyPolicy(
        DEFAULT_VERSION, EnumSet.allOf(PrivacyField.class), DEFAULT_MAX_PAYLOAD_CHARS);
  }

  /**
   * Returns whether a field may be populated.
   *
   * @param field field to check
   * @return true when allowed
   */
  public boolean allows(PrivacyField field) {
    return allowedFields.contains(Objects.requireNonNull(field, "field"));
  }
}
