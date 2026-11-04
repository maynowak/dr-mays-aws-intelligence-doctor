package io.github.maynowak.drmays.ai.privacy;

import io.github.maynowak.drmays.core.validation.DomainChecks;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Objects;

/** Opaque, single-use approval token with redacted string form. */
public final class ApprovalToken {

  private final String value;

  /**
   * Creates a token from its opaque wire value.
   *
   * @param value opaque token value
   */
  public ApprovalToken(String value) {
    this.value = DomainChecks.requireText(value, "value");
    if (!this.value.matches("[A-Za-z0-9_-]{32,128}")) {
      throw new IllegalArgumentException("value is not a valid approval token");
    }
  }

  /**
   * Returns the opaque wire value. Callers must not log it.
   *
   * @return token value
   */
  public String value() {
    return value;
  }

  @Override
  public boolean equals(Object other) {
    if (!(other instanceof ApprovalToken token)) {
      return false;
    }
    return MessageDigest.isEqual(
        value.getBytes(StandardCharsets.UTF_8), token.value.getBytes(StandardCharsets.UTF_8));
  }

  @Override
  public int hashCode() {
    return Objects.hash(value);
  }

  @Override
  public String toString() {
    return "ApprovalToken[redacted]";
  }
}
