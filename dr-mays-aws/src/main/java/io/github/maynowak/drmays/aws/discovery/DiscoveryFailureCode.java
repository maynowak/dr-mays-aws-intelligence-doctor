package io.github.maynowak.drmays.aws.discovery;

/** Stable, safe failure categories for discovery. */
public enum DiscoveryFailureCode {
  ACCESS_DENIED,
  THROTTLING,
  TIMEOUT,
  ENDPOINT,
  SERVICE_ERROR,
  IDENTITY_MISMATCH,
  INVALID_SCOPE,
  RESOURCE_LIMIT,
  DUPLICATE_RESOURCE,
  MALFORMED_RESPONSE,
  UNKNOWN
}
