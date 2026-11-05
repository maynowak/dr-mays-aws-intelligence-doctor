package io.github.maynowak.drmays.ai.provider;

import io.github.maynowak.drmays.ai.privacy.SanitizedExpertiseRequest;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.Objects;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;

/** Canonical serialization and hash checks for provider payload integrity. */
public final class PayloadIntegrity {

  private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

  private PayloadIntegrity() {}

  /**
   * Serializes a sanitized payload in the canonical form used for approval binding.
   *
   * @param payload sanitized payload
   * @return canonical JSON
   */
  public static String canonicalJson(SanitizedExpertiseRequest payload) {
    try {
      return OBJECT_MAPPER.writeValueAsString(Objects.requireNonNull(payload, "payload"));
    } catch (JacksonException exception) {
      throw new IllegalArgumentException("payload cannot be serialized", exception);
    }
  }

  /**
   * Returns the SHA-256 hash of exact JSON bytes.
   *
   * @param payloadJson payload JSON
   * @return lowercase hex hash
   */
  public static String hash(String payloadJson) {
    try {
      var digest = MessageDigest.getInstance("SHA-256");
      return HexFormat.of()
          .formatHex(
              digest.digest(
                  Objects.requireNonNull(payloadJson, "payloadJson")
                      .getBytes(StandardCharsets.UTF_8)));
    } catch (NoSuchAlgorithmException exception) {
      throw new IllegalStateException("SHA-256 is unavailable", exception);
    }
  }

  /**
   * Verifies that the JSON is the canonical serialization of the typed payload.
   *
   * @param payload sanitized payload
   * @param payloadJson claimed JSON
   * @return true when they match exactly
   */
  public static boolean matches(SanitizedExpertiseRequest payload, String payloadJson) {
    return canonicalJson(payload).equals(Objects.requireNonNull(payloadJson, "payloadJson"));
  }
}
