package io.github.maynowak.drmays.aws.ec2;

import static io.github.maynowak.drmays.aws.AwsTestFixtures.ACCOUNT;
import static io.github.maynowak.drmays.aws.AwsTestFixtures.REGION;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import io.github.maynowak.drmays.aws.discovery.AwsService;
import io.github.maynowak.drmays.aws.discovery.DiscoveryFailureCode;
import io.github.maynowak.drmays.aws.discovery.DiscoveryRequest;
import io.github.maynowak.drmays.aws.discovery.DiscoveryScope;
import io.github.maynowak.drmays.aws.discovery.DiscoveryStatus;
import io.github.maynowak.drmays.aws.mapping.AwsResourceMapper;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;
import software.amazon.awssdk.core.exception.ApiCallTimeoutException;
import software.amazon.awssdk.services.ec2.Ec2Client;
import software.amazon.awssdk.services.ec2.model.DescribeSecurityGroupsRequest;
import software.amazon.awssdk.services.ec2.model.DescribeSecurityGroupsResponse;
import software.amazon.awssdk.services.ec2.model.Ec2Exception;
import software.amazon.awssdk.services.ec2.model.IpPermission;
import software.amazon.awssdk.services.ec2.model.IpRange;
import software.amazon.awssdk.services.ec2.model.Ipv6Range;
import software.amazon.awssdk.services.ec2.model.SecurityGroup;
import software.amazon.awssdk.services.ec2.model.Tag;

class Ec2SecurityGroupDiscoveryTest {

  private final AwsResourceMapper mapper = new AwsResourceMapper();

  @Test
  void returnsCompleteResultForSecurityGroups() {
    var client = mock(Ec2Client.class);
    when(client.describeSecurityGroups(any(DescribeSecurityGroupsRequest.class)))
        .thenReturn(response(securityGroup("sg-1", "0.0.0.0/0")));

    var result = discovery(client).discover(request(10, List.of(REGION)));

    assertThat(result.status()).isEqualTo(DiscoveryStatus.COMPLETE);
    assertThat(result.resources()).hasSize(1);
    assertThat(result.resources().getFirst().attribute("ingress.public")).contains("true");
  }

  @Test
  void returnsCompleteEmptyResultForNoGroups() {
    var client = mock(Ec2Client.class);
    when(client.describeSecurityGroups(any(DescribeSecurityGroupsRequest.class)))
        .thenReturn(DescribeSecurityGroupsResponse.builder().build());

    var result = discovery(client).discover(request(10, List.of(REGION)));

    assertThat(result.status()).isEqualTo(DiscoveryStatus.COMPLETE);
    assertThat(result.resources()).isEmpty();
  }

  @Test
  void followsPaginationUntilTokenIsAbsent() {
    var client = mock(Ec2Client.class);
    when(client.describeSecurityGroups(any(DescribeSecurityGroupsRequest.class)))
        .thenReturn(
            DescribeSecurityGroupsResponse.builder()
                .securityGroups(securityGroup("sg-1", "10.0.0.0/8"))
                .nextToken("next")
                .build(),
            response(securityGroup("sg-2", "172.16.0.0/12")));

    var result = discovery(client).discover(request(10, List.of(REGION)));

    assertThat(result.resources()).hasSize(2);
    assertThat(result.status()).isEqualTo(DiscoveryStatus.COMPLETE);
  }

  @Test
  void mapsAccessDeniedToFailedResult() {
    var client = mock(Ec2Client.class);
    when(client.describeSecurityGroups(any(DescribeSecurityGroupsRequest.class)))
        .thenThrow(Ec2Exception.builder().statusCode(403).build());

    var result = discovery(client).discover(request(10, List.of(REGION)));

    assertThat(result.status()).isEqualTo(DiscoveryStatus.FAILED);
    assertThat(result.failures())
        .extracting(failure -> failure.code())
        .containsExactly(DiscoveryFailureCode.ACCESS_DENIED);
  }

  @Test
  void mapsThrottlingAndTimeoutToFailures() {
    var throttled = mock(Ec2Client.class);
    when(throttled.describeSecurityGroups(any(DescribeSecurityGroupsRequest.class)))
        .thenThrow(Ec2Exception.builder().statusCode(429).build());
    var timedOut = mock(Ec2Client.class);
    when(timedOut.describeSecurityGroups(any(DescribeSecurityGroupsRequest.class)))
        .thenThrow(ApiCallTimeoutException.create(1000L));

    assertThat(discovery(throttled).discover(request(10, List.of(REGION))).failures())
        .extracting(failure -> failure.code())
        .containsExactly(DiscoveryFailureCode.THROTTLING);
    assertThat(discovery(timedOut).discover(request(10, List.of(REGION))).failures())
        .extracting(failure -> failure.code())
        .containsExactly(DiscoveryFailureCode.TIMEOUT);
  }

  @Test
  void returnsPartialWhenOnlyOneRegionFails() {
    var successful = mock(Ec2Client.class);
    when(successful.describeSecurityGroups(any(DescribeSecurityGroupsRequest.class)))
        .thenReturn(response(securityGroup("sg-1", "10.0.0.0/8")));
    var failing = mock(Ec2Client.class);
    when(failing.describeSecurityGroups(any(DescribeSecurityGroupsRequest.class)))
        .thenThrow(Ec2Exception.builder().statusCode(403).build());
    var provider = new TestEc2Provider(successful, failing);

    var result =
        new Ec2SecurityGroupDiscovery(provider, mapper)
            .discover(request(10, List.of("eu-west-1", "us-east-1")));

    assertThat(result.status()).isEqualTo(DiscoveryStatus.PARTIAL);
    assertThat(result.resources()).hasSize(1);
    assertThat(result.failures()).hasSize(1);
  }

  @Test
  void enforcesResourceLimitAsPartial() {
    var client = mock(Ec2Client.class);
    when(client.describeSecurityGroups(any(DescribeSecurityGroupsRequest.class)))
        .thenReturn(
            DescribeSecurityGroupsResponse.builder()
                .securityGroups(
                    List.of(
                        securityGroup("sg-1", "10.0.0.0/8"), securityGroup("sg-2", "10.0.0.0/8")))
                .build());

    var result = discovery(client).discover(request(1, List.of(REGION)));

    assertThat(result.status()).isEqualTo(DiscoveryStatus.PARTIAL);
    assertThat(result.resources()).hasSize(1);
    assertThat(result.failures())
        .extracting(failure -> failure.code())
        .contains(DiscoveryFailureCode.RESOURCE_LIMIT);
  }

  @Test
  void reportsDuplicateSecurityGroupIdentities() {
    var client = mock(Ec2Client.class);
    var duplicate = securityGroup("sg-1", "10.0.0.0/8");
    when(client.describeSecurityGroups(any(DescribeSecurityGroupsRequest.class)))
        .thenReturn(
            DescribeSecurityGroupsResponse.builder()
                .securityGroups(List.of(duplicate, duplicate))
                .build());

    var result = discovery(client).discover(request(10, List.of(REGION)));

    assertThat(result.status()).isEqualTo(DiscoveryStatus.PARTIAL);
    assertThat(result.failures())
        .extracting(failure -> failure.code())
        .contains(DiscoveryFailureCode.DUPLICATE_RESOURCE);
  }

  @Test
  void capturesIpv6AndPortMetadata() {
    var client = mock(Ec2Client.class);
    var group =
        SecurityGroup.builder()
            .groupId("sg-ipv6")
            .ipPermissions(
                IpPermission.builder()
                    .ipProtocol("tcp")
                    .fromPort(443)
                    .toPort(443)
                    .ipv6Ranges(Ipv6Range.builder().cidrIpv6("::/0").build())
                    .build())
            .build();
    when(client.describeSecurityGroups(any(DescribeSecurityGroupsRequest.class)))
        .thenReturn(response(group));

    var resource = discovery(client).discover(request(10, List.of(REGION))).resources().getFirst();

    assertThat(resource.attribute("ingress.ipv6.public")).contains("true");
    assertThat(resource.attribute("ingress.port.ranges")).contains("tcp:443-443");
  }

  private Ec2SecurityGroupDiscovery discovery(Ec2Client client) {
    return new Ec2SecurityGroupDiscovery(region -> client, mapper);
  }

  private static DiscoveryRequest request(int maxResources, List<String> regions) {
    return new DiscoveryRequest(
        new DiscoveryScope("dr-mays-test", ACCOUNT, regions, Set.of(AwsService.EC2), maxResources));
  }

  private static DescribeSecurityGroupsResponse response(SecurityGroup... groups) {
    return DescribeSecurityGroupsResponse.builder().securityGroups(List.of(groups)).build();
  }

  private static SecurityGroup securityGroup(String id, String cidr) {
    return SecurityGroup.builder()
        .groupId(id)
        .ipPermissions(
            IpPermission.builder()
                .ipProtocol("tcp")
                .fromPort(22)
                .toPort(22)
                .ipRanges(IpRange.builder().cidrIp(cidr).build())
                .build())
        .tags(Tag.builder().key("owner").value("platform").build())
        .build();
  }

  private static final class TestEc2Provider implements Ec2ClientProvider {
    private final Ec2Client first;
    private final Ec2Client second;
    private int calls;

    private TestEc2Provider(Ec2Client first, Ec2Client second) {
      this.first = first;
      this.second = second;
    }

    @Override
    public Ec2Client client(String region) {
      return calls++ == 0 ? first : second;
    }
  }
}
