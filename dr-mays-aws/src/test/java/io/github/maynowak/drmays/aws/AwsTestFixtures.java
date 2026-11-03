package io.github.maynowak.drmays.aws;

import io.github.maynowak.drmays.aws.discovery.AwsService;
import io.github.maynowak.drmays.aws.discovery.DiscoveryFailure;
import io.github.maynowak.drmays.aws.discovery.DiscoveryFailureCode;
import io.github.maynowak.drmays.aws.discovery.DiscoveryScope;
import io.github.maynowak.drmays.core.resource.InfrastructureResource;
import io.github.maynowak.drmays.core.resource.ResourceIdentity;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** Shared deterministic AWS test data. */
public final class AwsTestFixtures {

  public static final String ACCOUNT = "000000000000";
  public static final String REGION = "eu-west-1";

  private AwsTestFixtures() {}

  public static DiscoveryScope scope(AwsService... services) {
    return scope(10, services);
  }

  public static DiscoveryScope scope(int maxResources, AwsService... services) {
    return new DiscoveryScope(
        "dr-mays-test", ACCOUNT, List.of(REGION), Set.of(services), maxResources);
  }

  public static InfrastructureResource resource(String type, String id) {
    return new InfrastructureResource(
        new ResourceIdentity("aws", ACCOUNT, REGION, type, id), Map.of(), Map.of());
  }

  public static DiscoveryFailure failure(
      AwsService service, DiscoveryFailureCode code, String message) {
    return new DiscoveryFailure(service, code, message);
  }
}
