package io.github.maynowak.drmays.aws.mapping;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import org.junit.jupiter.api.Test;
import software.amazon.awssdk.services.ec2.model.IpPermission;
import software.amazon.awssdk.services.ec2.model.IpRange;
import software.amazon.awssdk.services.ec2.model.Ipv6Range;
import software.amazon.awssdk.services.ec2.model.SecurityGroup;
import software.amazon.awssdk.services.ec2.model.Tag;
import software.amazon.awssdk.services.ec2.model.UserIdGroupPair;
import software.amazon.awssdk.services.s3.model.Bucket;
import software.amazon.awssdk.services.s3.model.BucketVersioningStatus;
import software.amazon.awssdk.services.s3.model.GetBucketVersioningResponse;
import software.amazon.awssdk.services.s3.model.PublicAccessBlockConfiguration;

class AwsResourceMapperTest {

  private final AwsResourceMapper mapper = new AwsResourceMapper();

  @Test
  void mapsSecurityGroupIdentityTagsAndVpc() {
    var group =
        SecurityGroup.builder()
            .groupId("sg-1")
            .vpcId("vpc-1")
            .tags(Tag.builder().key("owner").value("platform").build())
            .ipPermissions(List.of())
            .build();

    var resource = mapper.toResource(group, "000000000000", "eu-west-1");

    assertThat(resource.identity().resourceType()).isEqualTo("network");
    assertThat(resource.identity().resourceId()).isEqualTo("sg-1");
    assertThat(resource.tags()).containsEntry("owner", "platform");
    assertThat(resource.attribute("vpc.id")).contains("vpc-1");
    assertThat(resource.attribute("ingress.public")).contains("false");
  }

  @Test
  void detectsPublicIpv4Ingress() {
    var group =
        securityGroup(
            IpPermission.builder()
                .ipProtocol("tcp")
                .fromPort(22)
                .toPort(22)
                .ipRanges(IpRange.builder().cidrIp("0.0.0.0/0").build())
                .build());

    var resource = mapper.toResource(group, "000000000000", "eu-west-1");

    assertThat(resource.attribute("ingress.public")).contains("true");
    assertThat(resource.attribute("ingress.ipv4.public")).contains("true");
    assertThat(resource.attribute("ingress.ipv6.public")).contains("false");
    assertThat(resource.attribute("ingress.port.ranges")).contains("tcp:22-22");
  }

  @Test
  void detectsPublicIpv6Ingress() {
    var group =
        securityGroup(
            IpPermission.builder()
                .ipProtocol("udp")
                .fromPort(53)
                .toPort(53)
                .ipv6Ranges(Ipv6Range.builder().cidrIpv6("::/0").build())
                .build());

    var resource = mapper.toResource(group, "000000000000", "eu-west-1");

    assertThat(resource.attribute("ingress.public")).contains("true");
    assertThat(resource.attribute("ingress.ipv4.public")).contains("false");
    assertThat(resource.attribute("ingress.ipv6.public")).contains("true");
  }

  @Test
  void treatsPrivateCidrAsNonPublic() {
    var group =
        securityGroup(
            IpPermission.builder()
                .ipProtocol("tcp")
                .fromPort(443)
                .toPort(443)
                .ipRanges(IpRange.builder().cidrIp("10.0.0.0/16").build())
                .build());

    assertThat(mapper.toResource(group, "000000000000", "eu-west-1").attribute("ingress.public"))
        .contains("false");
  }

  @Test
  void capturesProtocolsPortRangesAndGroupReferences() {
    var group =
        securityGroup(
            IpPermission.builder()
                .ipProtocol("tcp")
                .fromPort(443)
                .toPort(443)
                .userIdGroupPairs(UserIdGroupPair.builder().groupId("sg-source").build())
                .build(),
            IpPermission.builder().ipProtocol("-1").build());

    var resource = mapper.toResource(group, "000000000000", "eu-west-1");

    assertThat(resource.attribute("ingress.rule.count")).contains("2");
    assertThat(resource.attribute("ingress.group.reference.count")).contains("1");
    assertThat(resource.attribute("ingress.protocols")).contains("-1,tcp");
    assertThat(resource.attribute("ingress.port.ranges")).contains("-1:*-*,tcp:443-443");
  }

  @Test
  void handlesEmptyIngressAndMissingVpc() {
    var group = SecurityGroup.builder().groupId("sg-empty").build();

    var resource = mapper.toResource(group, "000000000000", "eu-west-1");

    assertThat(resource.attribute("ingress.rule.count")).contains("0");
    assertThat(resource.attribute("ingress.public")).contains("false");
    assertThat(resource.attribute("vpc.id")).isEmpty();
  }

  @Test
  void mapsS3VersioningAndPublicAccessBlock() {
    var block =
        PublicAccessBlockConfiguration.builder()
            .blockPublicAcls(true)
            .ignorePublicAcls(true)
            .blockPublicPolicy(true)
            .restrictPublicBuckets(true)
            .build();
    var versioning =
        GetBucketVersioningResponse.builder().status(BucketVersioningStatus.ENABLED).build();

    var resource =
        mapper.toResource(
            Bucket.builder().name("bucket-1").build(),
            "eu-west-1",
            List.of(
                software.amazon.awssdk.services.s3.model.Tag.builder()
                    .key("owner")
                    .value("platform")
                    .build()),
            versioning,
            block,
            "000000000000");

    assertThat(resource.identity().resourceType()).isEqualTo("s3-bucket");
    assertThat(resource.attributes())
        .containsEntry("s3.versioning.status", "Enabled")
        .containsEntry("s3.publicAccessBlock.enabled", "true")
        .containsEntry("s3.metadata.complete", "true");
    assertThat(resource.tags()).containsEntry("owner", "platform");
  }

  @Test
  void marksMissingPublicAccessBlockAsNotConfigured() {
    var resource =
        mapper.toResource(
            Bucket.builder().name("bucket-2").build(),
            "eu-west-1",
            List.of(),
            GetBucketVersioningResponse.builder().build(),
            null,
            "000000000000");

    assertThat(resource.attributes())
        .containsEntry("s3.publicAccessBlock.configured", "false")
        .containsEntry("s3.publicAccessBlock.enabled", "false")
        .containsEntry("s3.versioning.status", "Unversioned");
  }

  @Test
  void mapsPartialPublicAccessBlockFlags() {
    var block = PublicAccessBlockConfiguration.builder().blockPublicAcls(true).build();

    var resource =
        mapper.toResource(
            Bucket.builder().name("bucket-3").build(),
            "eu-west-1",
            List.of(),
            GetBucketVersioningResponse.builder().build(),
            block,
            "000000000000");

    assertThat(resource.attributes())
        .containsEntry("s3.publicAccessBlock.blockPublicAcls", "true")
        .containsEntry("s3.publicAccessBlock.ignorePublicAcls", "false")
        .containsEntry("s3.publicAccessBlock.enabled", "false");
  }

  private static SecurityGroup securityGroup(IpPermission... permissions) {
    return SecurityGroup.builder().groupId("sg-1").ipPermissions(List.of(permissions)).build();
  }
}
