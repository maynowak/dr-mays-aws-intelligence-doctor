package io.github.maynowak.drmays.aws.discovery;

import io.github.maynowak.drmays.core.validation.DomainChecks;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

/**
 * Explicit, approved scope for a future AWS discovery run.
 *
 * @param profile explicit non-default AWS profile name
 * @param expectedAccountId expected 12-digit account ID
 * @param regions allowed regions; at least one
 * @param services allowed services; must include EC2 or S3
 * @param maxResources hard upper bound on emitted resources
 */
public record DiscoveryScope(
    String profile,
    String expectedAccountId,
    List<String> regions,
    Set<AwsService> services,
    int maxResources) {

  public static final int MAX_RESOURCES_LIMIT = 10_000;

  public DiscoveryScope {
    profile = DomainChecks.requireText(profile, "profile");
    if ("default".equalsIgnoreCase(profile)) {
      throw new IllegalArgumentException("profile must not be the AWS default profile");
    }
    expectedAccountId = DomainChecks.requireText(expectedAccountId, "expectedAccountId");
    if (!expectedAccountId.matches("\\d{12}")) {
      throw new IllegalArgumentException("expectedAccountId must be a 12-digit account ID");
    }

    Objects.requireNonNull(regions, "regions");
    if (regions.isEmpty()) {
      throw new IllegalArgumentException("regions must not be empty");
    }
    var uniqueRegions = new HashSet<String>();
    for (var region : regions) {
      var value = DomainChecks.requireText(region, "region");
      if (!uniqueRegions.add(value)) {
        throw new IllegalArgumentException("duplicate region: " + value);
      }
    }
    regions = List.copyOf(regions);

    Objects.requireNonNull(services, "services");
    if (services.isEmpty()) {
      throw new IllegalArgumentException("services must not be empty");
    }
    services = Set.copyOf(services);
    if (services.stream()
        .noneMatch(service -> service == AwsService.EC2 || service == AwsService.S3)) {
      throw new IllegalArgumentException("services must include EC2 or S3");
    }

    if (maxResources < 1 || maxResources > MAX_RESOURCES_LIMIT) {
      throw new IllegalArgumentException(
          "maxResources must be between 1 and " + MAX_RESOURCES_LIMIT);
    }
  }
}
