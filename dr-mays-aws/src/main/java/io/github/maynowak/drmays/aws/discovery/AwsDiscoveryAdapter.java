package io.github.maynowak.drmays.aws.discovery;

import io.github.maynowak.drmays.aws.identity.AwsIdentityException;
import io.github.maynowak.drmays.aws.identity.AwsIdentityVerifier;
import io.github.maynowak.drmays.core.resource.InfrastructureResource;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;

/** Fail-closed composition of identity verification and service discoveries. */
public final class AwsDiscoveryAdapter {

  private final AwsIdentityVerifier identityVerifier;
  private final List<InfrastructureDiscovery> discoveries;

  public AwsDiscoveryAdapter(
      AwsIdentityVerifier identityVerifier, List<InfrastructureDiscovery> discoveries) {
    this.identityVerifier = Objects.requireNonNull(identityVerifier, "identityVerifier");
    Objects.requireNonNull(discoveries, "discoveries");
    var services = new HashSet<AwsService>();
    for (var discovery : discoveries) {
      var value = Objects.requireNonNull(discovery, "discovery");
      if (value.service() == AwsService.STS) {
        throw new IllegalArgumentException("STS is reserved for identity verification");
      }
      if (!services.add(value.service())) {
        throw new IllegalArgumentException("duplicate discovery service: " + value.service());
      }
    }
    this.discoveries = List.copyOf(discoveries);
  }

  /**
   * Verifies identity first, then runs the allowed service discoveries.
   *
   * @param request approved request
   * @return fail-closed discovery result
   */
  public DiscoveryResult discover(DiscoveryRequest request) {
    Objects.requireNonNull(request, "request");
    var scope = request.scope();
    try {
      identityVerifier.verify(scope);
    } catch (AwsIdentityException exception) {
      return DiscoveryResult.failed(
          List.of(new DiscoveryFailure(AwsService.STS, exception.code(), exception.getMessage())));
    } catch (RuntimeException exception) {
      return DiscoveryResult.failed(
          List.of(
              new DiscoveryFailure(
                  AwsService.STS, DiscoveryFailureCode.UNKNOWN, "identity verification failed")));
    }

    var resources = new ArrayList<InfrastructureResource>();
    var failures = new ArrayList<DiscoveryFailure>();
    var identities = new HashSet<String>();

    for (var service : scope.services().stream().sorted().toList()) {
      if (service == AwsService.STS) {
        continue;
      }
      var discovery = discoveries.stream().filter(value -> value.service() == service).findFirst();
      if (discovery.isEmpty()) {
        failures.add(
            new DiscoveryFailure(
                service,
                DiscoveryFailureCode.INVALID_SCOPE,
                "no discovery registered for service"));
        continue;
      }
      var result = discovery.get().discover(request);
      failures.addAll(result.failures());
      for (var resource : result.resources()) {
        if (resources.size() >= scope.maxResources()) {
          failures.add(
              new DiscoveryFailure(
                  service, DiscoveryFailureCode.RESOURCE_LIMIT, "resource limit reached"));
          break;
        }
        if (!identities.add(resource.identity().canonicalId())) {
          failures.add(
              new DiscoveryFailure(
                  service, DiscoveryFailureCode.DUPLICATE_RESOURCE, "duplicate resource identity"));
          continue;
        }
        resources.add(resource);
      }
    }

    if (failures.isEmpty()) {
      return DiscoveryResult.complete(resources);
    }
    return resources.isEmpty()
        ? DiscoveryResult.failed(failures)
        : DiscoveryResult.partial(resources, failures);
  }
}
