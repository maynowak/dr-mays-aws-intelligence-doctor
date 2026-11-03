package io.github.maynowak.drmays.aws.ec2;

import io.github.maynowak.drmays.aws.discovery.AwsService;
import io.github.maynowak.drmays.aws.discovery.DiscoveryFailure;
import io.github.maynowak.drmays.aws.discovery.DiscoveryFailureCode;
import io.github.maynowak.drmays.aws.discovery.DiscoveryRequest;
import io.github.maynowak.drmays.aws.discovery.DiscoveryResult;
import io.github.maynowak.drmays.aws.discovery.InfrastructureDiscovery;
import io.github.maynowak.drmays.aws.failure.AwsFailureMapper;
import io.github.maynowak.drmays.aws.mapping.AwsResourceMapper;
import io.github.maynowak.drmays.core.resource.InfrastructureResource;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import software.amazon.awssdk.services.ec2.model.DescribeSecurityGroupsRequest;

/** Read-only EC2 security-group discovery with bounded pagination and resource limits. */
public final class Ec2SecurityGroupDiscovery implements InfrastructureDiscovery {

  private static final int PAGE_SIZE = 100;

  private final Ec2ClientProvider clientProvider;
  private final AwsResourceMapper mapper;

  public Ec2SecurityGroupDiscovery(Ec2ClientProvider clientProvider, AwsResourceMapper mapper) {
    this.clientProvider = Objects.requireNonNull(clientProvider, "clientProvider");
    this.mapper = Objects.requireNonNull(mapper, "mapper");
  }

  @Override
  public AwsService service() {
    return AwsService.EC2;
  }

  @Override
  public DiscoveryResult discover(DiscoveryRequest request) {
    Objects.requireNonNull(request, "request");
    var scope = request.scope();
    var resources = new ArrayList<InfrastructureResource>();
    var failures = new ArrayList<DiscoveryFailure>();
    var identities = new HashSet<String>();
    var limitReached = false;

    for (var region : scope.regions().stream().sorted().toList()) {
      if (limitReached || resources.size() >= scope.maxResources()) {
        limitReached = true;
        break;
      }
      try {
        var client = clientProvider.client(region);
        String nextToken = null;
        var seenTokens = new HashSet<String>();

        do {
          var remaining = scope.maxResources() - resources.size();
          var response =
              client.describeSecurityGroups(
                  DescribeSecurityGroupsRequest.builder()
                      .maxResults(Math.min(PAGE_SIZE, Math.max(1, remaining)))
                      .nextToken(nextToken)
                      .build());

          for (var group : response.securityGroups()) {
            if (resources.size() >= scope.maxResources()) {
              failures.add(limitFailure());
              limitReached = true;
              break;
            }
            var resource = mapper.toResource(group, scope.expectedAccountId(), region);
            if (!identities.add(resource.identity().canonicalId())) {
              failures.add(
                  new DiscoveryFailure(
                      AwsService.EC2,
                      DiscoveryFailureCode.DUPLICATE_RESOURCE,
                      "duplicate security group identity"));
              continue;
            }
            resources.add(resource);
          }

          nextToken = response.nextToken();
          if (nextToken == null || nextToken.isBlank()) {
            break;
          }
          if (!seenTokens.add(nextToken)) {
            failures.add(
                new DiscoveryFailure(
                    AwsService.EC2,
                    DiscoveryFailureCode.MALFORMED_RESPONSE,
                    "pagination token repeated"));
            break;
          }
        } while (!limitReached);
      } catch (RuntimeException exception) {
        failures.add(AwsFailureMapper.failure(AwsService.EC2, exception));
      }
    }

    if (limitReached
        && failures.stream()
            .noneMatch(failure -> failure.code() == DiscoveryFailureCode.RESOURCE_LIMIT)) {
      failures.add(limitFailure());
    }
    return result(resources, failures);
  }

  private static DiscoveryFailure limitFailure() {
    return new DiscoveryFailure(
        AwsService.EC2, DiscoveryFailureCode.RESOURCE_LIMIT, "resource limit reached");
  }

  private static DiscoveryResult result(
      List<InfrastructureResource> resources, List<DiscoveryFailure> failures) {
    if (failures.isEmpty()) {
      return DiscoveryResult.complete(resources);
    }
    return resources.isEmpty()
        ? DiscoveryResult.failed(failures)
        : DiscoveryResult.partial(resources, failures);
  }
}
