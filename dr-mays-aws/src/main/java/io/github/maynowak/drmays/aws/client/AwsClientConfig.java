package io.github.maynowak.drmays.aws.client;

import io.github.maynowak.drmays.core.validation.DomainChecks;

/**
 * Explicit client configuration; no default profile fallback.
 *
 * @param profile named AWS profile
 * @param region AWS region
 */
public record AwsClientConfig(String profile, String region) {

  public AwsClientConfig {
    profile = DomainChecks.requireText(profile, "profile");
    if ("default".equalsIgnoreCase(profile)) {
      throw new IllegalArgumentException("profile must not be the AWS default profile");
    }
    region = DomainChecks.requireText(region, "region");
  }
}
