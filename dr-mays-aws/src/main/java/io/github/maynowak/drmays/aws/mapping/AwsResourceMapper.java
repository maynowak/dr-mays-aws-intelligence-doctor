package io.github.maynowak.drmays.aws.mapping;

import io.github.maynowak.drmays.core.resource.InfrastructureResource;
import io.github.maynowak.drmays.core.resource.ResourceIdentity;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import software.amazon.awssdk.services.ec2.model.IpPermission;
import software.amazon.awssdk.services.ec2.model.SecurityGroup;
import software.amazon.awssdk.services.s3.model.Bucket;
import software.amazon.awssdk.services.s3.model.GetBucketVersioningResponse;
import software.amazon.awssdk.services.s3.model.PublicAccessBlockConfiguration;
import software.amazon.awssdk.services.s3.model.Tag;

/** Maps read-only AWS SDK models into the technology-neutral core resource model. */
public final class AwsResourceMapper {

  public static final String ANY_IPV4 = "0.0.0.0/0";
  public static final String ANY_IPV6 = "::/0";

  public InfrastructureResource toResource(SecurityGroup group, String accountId, String region) {
    Objects.requireNonNull(group, "group");
    var permissions =
        group.ipPermissions() == null ? List.<IpPermission>of() : group.ipPermissions();
    var ipv4Public = permissions.stream().anyMatch(permission -> hasPublicIpv4(permission));
    var ipv6Public = permissions.stream().anyMatch(permission -> hasPublicIpv6(permission));
    var publicRuleCount =
        permissions.stream()
            .filter(permission -> hasPublicIpv4(permission) || hasPublicIpv6(permission))
            .count();
    var attributes = new LinkedHashMap<String, String>();
    attributes.put("ingress.public", Boolean.toString(ipv4Public || ipv6Public));
    attributes.put("ingress.rule.count", Integer.toString(permissions.size()));
    attributes.put("ingress.public.rule.count", Long.toString(publicRuleCount));
    attributes.put("ingress.ipv4.public", Boolean.toString(ipv4Public));
    attributes.put("ingress.ipv6.public", Boolean.toString(ipv6Public));
    attributes.put("ingress.protocols", protocols(permissions));
    attributes.put("ingress.port.ranges", portRanges(permissions));
    attributes.put(
        "ingress.group.reference.count", Long.toString(groupReferenceCount(permissions)));
    if (group.vpcId() != null && !group.vpcId().isBlank()) {
      attributes.put("vpc.id", group.vpcId());
    }
    return new InfrastructureResource(
        new ResourceIdentity("aws", accountId, region, "network", group.groupId()),
        ec2Tags(group.tags()),
        attributes);
  }

  public InfrastructureResource toResource(
      Bucket bucket,
      String region,
      List<Tag> tags,
      GetBucketVersioningResponse versioning,
      PublicAccessBlockConfiguration publicAccessBlock,
      String accountId) {
    Objects.requireNonNull(bucket, "bucket");
    var attributes = new LinkedHashMap<String, String>();
    attributes.put("s3.versioning.status", versioningStatus(versioning));
    attributes.put("s3.publicAccessBlock.configured", Boolean.toString(publicAccessBlock != null));
    attributes.put(
        "s3.publicAccessBlock.enabled",
        Boolean.toString(publicAccessBlockEnabled(publicAccessBlock)));
    attributes.put(
        "s3.publicAccessBlock.blockPublicAcls",
        Boolean.toString(blockPublicAcls(publicAccessBlock)));
    attributes.put(
        "s3.publicAccessBlock.ignorePublicAcls",
        Boolean.toString(ignorePublicAcls(publicAccessBlock)));
    attributes.put(
        "s3.publicAccessBlock.blockPublicPolicy",
        Boolean.toString(blockPublicPolicy(publicAccessBlock)));
    attributes.put(
        "s3.publicAccessBlock.restrictPublicBuckets",
        Boolean.toString(restrictPublicBuckets(publicAccessBlock)));
    attributes.put("s3.metadata.complete", "true");
    return new InfrastructureResource(
        new ResourceIdentity("aws", accountId, region, "s3-bucket", bucket.name()),
        s3Tags(tags),
        attributes);
  }

  private static boolean hasPublicIpv4(IpPermission permission) {
    return permission.ipRanges() != null
        && permission.ipRanges().stream().anyMatch(range -> ANY_IPV4.equals(range.cidrIp()));
  }

  private static boolean hasPublicIpv6(IpPermission permission) {
    return permission.ipv6Ranges() != null
        && permission.ipv6Ranges().stream().anyMatch(range -> ANY_IPV6.equals(range.cidrIpv6()));
  }

  private static long groupReferenceCount(List<IpPermission> permissions) {
    return permissions.stream()
        .flatMap(permission ->
            permission.userIdGroupPairs() == null
                ? java.util.stream.Stream.empty()
                : permission.userIdGroupPairs().stream())
        .count();
  }

  private static String protocols(List<IpPermission> permissions) {
    return permissions.stream()
        .map(IpPermission::ipProtocol)
        .filter(Objects::nonNull)
        .filter(value -> !value.isBlank())
        .distinct()
        .sorted()
        .collect(Collectors.joining(","));
  }

  private static String portRanges(List<IpPermission> permissions) {
    var ranges = new ArrayList<String>();
    for (var permission : permissions) {
      var protocol = permission.ipProtocol() == null ? "all" : permission.ipProtocol();
      var from = permission.fromPort() == null ? "*" : permission.fromPort().toString();
      var to = permission.toPort() == null ? "*" : permission.toPort().toString();
      ranges.add(protocol + ":" + from + "-" + to);
    }
    ranges.sort(String::compareTo);
    return String.join(",", ranges);
  }

  private static Map<String, String> ec2Tags(
      List<software.amazon.awssdk.services.ec2.model.Tag> tags) {
    var result = new LinkedHashMap<String, String>();
    if (tags != null) {
      tags.forEach(tag -> result.putIfAbsent(tag.key(), tag.value()));
    }
    return result;
  }

  private static Map<String, String> s3Tags(List<Tag> tags) {
    var result = new LinkedHashMap<String, String>();
    if (tags != null) {
      tags.forEach(tag -> result.putIfAbsent(tag.key(), tag.value()));
    }
    return result;
  }

  private static String versioningStatus(GetBucketVersioningResponse versioning) {
    if (versioning == null || versioning.statusAsString() == null) {
      return "Unversioned";
    }
    return versioning.statusAsString();
  }

  private static boolean publicAccessBlockEnabled(PublicAccessBlockConfiguration block) {
    return block != null
        && Boolean.TRUE.equals(block.blockPublicAcls())
        && Boolean.TRUE.equals(block.ignorePublicAcls())
        && Boolean.TRUE.equals(block.blockPublicPolicy())
        && Boolean.TRUE.equals(block.restrictPublicBuckets());
  }

  private static boolean blockPublicAcls(PublicAccessBlockConfiguration block) {
    return block != null && Boolean.TRUE.equals(block.blockPublicAcls());
  }

  private static boolean ignorePublicAcls(PublicAccessBlockConfiguration block) {
    return block != null && Boolean.TRUE.equals(block.ignorePublicAcls());
  }

  private static boolean blockPublicPolicy(PublicAccessBlockConfiguration block) {
    return block != null && Boolean.TRUE.equals(block.blockPublicPolicy());
  }

  private static boolean restrictPublicBuckets(PublicAccessBlockConfiguration block) {
    return block != null && Boolean.TRUE.equals(block.restrictPublicBuckets());
  }
}
