package io.github.maynowak.drmays.ai.privacy;

import io.github.maynowak.drmays.core.validation.DomainChecks;
import java.util.Objects;

/**
 * Safe privacy validation issue. Messages must never contain the offending raw value.
 *
 * @param code stable issue code
 * @param field affected payload field
 * @param message safe explanation
 */
public record PrivacyViolation(String code, String field, String message) {

  public static final String RAW_ACCOUNT_ID = "RAW_ACCOUNT_ID";
  public static final String RAW_RESOURCE_ID = "RAW_RESOURCE_ID";
  public static final String EMAIL_ADDRESS = "EMAIL_ADDRESS";
  public static final String IP_ADDRESS = "IP_ADDRESS";
  public static final String HOSTNAME = "HOSTNAME";
  public static final String URL = "URL";
  public static final String SECRET_PATTERN = "SECRET_PATTERN";
  public static final String CONTROL_CHARACTER = "CONTROL_CHARACTER";
  public static final String PAYLOAD_TOO_LARGE = "PAYLOAD_TOO_LARGE";
  public static final String INVALID_FIELD = "INVALID_FIELD";

  public PrivacyViolation {
    code = DomainChecks.requireText(code, "code");
    field = DomainChecks.requireText(field, "field");
    message = DomainChecks.requireText(message, "message");
    Objects.requireNonNull(field, "field");
  }
}
