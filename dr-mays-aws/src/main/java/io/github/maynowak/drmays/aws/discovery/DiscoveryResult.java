package io.github.maynowak.drmays.aws.discovery;

import io.github.maynowak.drmays.core.resource.InfrastructureResource;
import io.github.maynowak.drmays.core.resource.ResourceIdentity;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;

/**
 * Result of a scoped discovery run with explicit completeness semantics.
 *
 * @param resources discovered resources, sorted by canonical identity
 * @param failures structured failures
 * @param status COMPLETE, PARTIAL or FAILED
 */
public record DiscoveryResult(
    List<InfrastructureResource> resources,
    List<DiscoveryFailure> failures,
    DiscoveryStatus status) {

  public DiscoveryResult {
    Objects.requireNonNull(resources, "resources");
    Objects.requireNonNull(failures, "failures");
    status = Objects.requireNonNull(status, "status");

    var sortedResources = new ArrayList<InfrastructureResource>(resources.size());
    var identities = new HashSet<ResourceIdentity>();
    for (var resource : resources) {
      var value = Objects.requireNonNull(resource, "resource");
      if (!identities.add(value.identity())) {
        throw new IllegalArgumentException(
            "duplicate resource identity: " + value.identity().canonicalId());
      }
      sortedResources.add(value);
    }
    sortedResources.sort(Comparator.comparing(resource -> resource.identity().canonicalId()));

    var sortedFailures = new ArrayList<DiscoveryFailure>(failures.size());
    for (var failure : failures) {
      sortedFailures.add(Objects.requireNonNull(failure, "failure"));
    }
    sortedFailures.sort(
        Comparator.comparing((DiscoveryFailure failure) -> failure.service().name())
            .thenComparing(failure -> failure.code().name())
            .thenComparing(DiscoveryFailure::message));

    if (status == DiscoveryStatus.COMPLETE && !sortedFailures.isEmpty()) {
      throw new IllegalArgumentException("COMPLETE result must not contain failures");
    }
    if (status == DiscoveryStatus.PARTIAL
        && (sortedFailures.isEmpty() || sortedResources.isEmpty())) {
      throw new IllegalArgumentException("PARTIAL result needs resources and failures");
    }
    if (status == DiscoveryStatus.FAILED && !sortedResources.isEmpty()) {
      throw new IllegalArgumentException("FAILED result must not contain resources");
    }

    resources = List.copyOf(sortedResources);
    failures = List.copyOf(sortedFailures);
  }

  /**
   * Creates a complete result.
   *
   * @param resources discovered resources
   * @return complete result
   */
  public static DiscoveryResult complete(List<InfrastructureResource> resources) {
    return new DiscoveryResult(resources, List.of(), DiscoveryStatus.COMPLETE);
  }

  /**
   * Creates a partial result.
   *
   * @param resources usable resources
   * @param failures failures encountered
   * @return partial result
   */
  public static DiscoveryResult partial(
      List<InfrastructureResource> resources, List<DiscoveryFailure> failures) {
    return new DiscoveryResult(resources, failures, DiscoveryStatus.PARTIAL);
  }

  /**
   * Creates a failed result with no usable resources.
   *
   * @param failures failures encountered
   * @return failed result
   */
  public static DiscoveryResult failed(List<DiscoveryFailure> failures) {
    return new DiscoveryResult(List.of(), failures, DiscoveryStatus.FAILED);
  }
}
