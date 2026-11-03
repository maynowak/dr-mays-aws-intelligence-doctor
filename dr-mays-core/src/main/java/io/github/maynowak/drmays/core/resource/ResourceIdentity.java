package io.github.maynowak.drmays.core.resource;

import io.github.maynowak.drmays.core.validation.DomainChecks;

/**
 * Stable identity of an infrastructure resource.
 *
 * <p>The identity is provider-neutral. AWS adapters can supply values such as {@code provider=aws},
 * an account ID, a region, an AWS resource type and the provider resource ID.
 *
 * @param provider the infrastructure provider
 * @param accountId the provider account identifier
 * @param region the provider region
 * @param resourceType the provider resource type
 * @param resourceId the provider resource identifier
 */
public record ResourceIdentity(
    String provider, String accountId, String region, String resourceType, String resourceId) {

  public ResourceIdentity {
    provider = DomainChecks.requireText(provider, "provider");
    accountId = DomainChecks.requireText(accountId, "accountId");
    region = DomainChecks.requireText(region, "region");
    resourceType = DomainChecks.requireText(resourceType, "resourceType");
    resourceId = DomainChecks.requireText(resourceId, "resourceId");
  }

  /**
   * Returns a deterministic key suitable for sorting and log output.
   *
   * @return provider/account/region/type/id joined by {@code |}
   */
  public String canonicalId() {
    return String.join("|", provider, accountId, region, resourceType, resourceId);
  }
}
