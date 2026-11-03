package io.github.maynowak.drmays.aws.identity;

import io.github.maynowak.drmays.aws.discovery.DiscoveryFailureCode;
import java.util.Objects;

/** Fail-closed identity verification error. */
public class AwsIdentityException extends RuntimeException {

  private final DiscoveryFailureCode code;

  public AwsIdentityException(DiscoveryFailureCode code, String message) {
    super(Objects.requireNonNull(message, "message"));
    this.code = Objects.requireNonNull(code, "code");
  }

  public DiscoveryFailureCode code() {
    return code;
  }
}
