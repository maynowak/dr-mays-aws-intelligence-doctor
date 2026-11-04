package io.github.maynowak.drmays.ai.provider;

import io.github.maynowak.drmays.core.validation.DomainChecks;
import java.util.Objects;

/**
 * Bounded provider result. Failure content is empty; error codes must not contain secrets.
 *
 * @param status execution status
 * @param providerId provider identifier
 * @param modelId model identifier
 * @param content model output or empty on failure
 * @param inputChars payload size
 * @param outputChars output size
 * @param durationMillis measured duration, or 0 when not measured
 * @param errorCode safe error code
 */
public record ModelInvocationResult(
    ProviderExecutionStatus status,
    String providerId,
    String modelId,
    String content,
    int inputChars,
    int outputChars,
    long durationMillis,
    String errorCode) {

  public ModelInvocationResult {
    status = Objects.requireNonNull(status, "status");
    providerId = DomainChecks.requireText(providerId, "providerId");
    modelId = DomainChecks.requireText(modelId, "modelId");
    content = Objects.requireNonNull(content, "content");
    errorCode = Objects.requireNonNull(errorCode, "errorCode");
    if (inputChars < 0 || outputChars < 0 || durationMillis < 0) {
      throw new IllegalArgumentException("result measurements must not be negative");
    }
  }

  /**
   * Creates a successful result.
   *
   * @param providerId provider identifier
   * @param modelId model identifier
   * @param content output
   * @param inputChars payload size
   * @return success result
   */
  public static ModelInvocationResult success(
      String providerId, String modelId, String content, int inputChars) {
    var value = DomainChecks.requireText(content, "content");
    return new ModelInvocationResult(
        ProviderExecutionStatus.SUCCESS,
        providerId,
        modelId,
        value,
        inputChars,
        value.length(),
        0,
        "");
  }

  /**
   * Creates a failure result with an empty body.
   *
   * @param status failure status
   * @param providerId provider identifier
   * @param modelId model identifier
   * @param errorCode safe code
   * @return failure result
   */
  public static ModelInvocationResult failure(
      ProviderExecutionStatus status, String providerId, String modelId, String errorCode) {
    if (status == ProviderExecutionStatus.SUCCESS) {
      throw new IllegalArgumentException("failure status must not be SUCCESS");
    }
    return new ModelInvocationResult(status, providerId, modelId, "", 0, 0, 0, errorCode);
  }
}
