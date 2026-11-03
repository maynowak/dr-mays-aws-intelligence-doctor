package io.github.maynowak.drmays.aws.discovery;

import java.util.Objects;

/**
 * Request for one scoped discovery run.
 *
 * @param scope approved discovery scope
 */
public record DiscoveryRequest(DiscoveryScope scope) {

  public DiscoveryRequest {
    scope = Objects.requireNonNull(scope, "scope");
  }
}
