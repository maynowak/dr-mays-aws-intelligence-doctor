package io.github.maynowak.drmays.aws.discovery;

import java.util.Objects;

/**
 * Structured, safe discovery failure.
 *
 * @param service AWS service that failed
 * @param code stable failure category
 * @param message non-sensitive human-readable summary
 */
public record DiscoveryFailure(AwsService service, DiscoveryFailureCode code, String message) {

  public DiscoveryFailure {
    service = Objects.requireNonNull(service, "service");
    code = Objects.requireNonNull(code, "code");
    if (message == null || message.isBlank()) {
      throw new IllegalArgumentException("message must not be blank");
    }
  }
}
