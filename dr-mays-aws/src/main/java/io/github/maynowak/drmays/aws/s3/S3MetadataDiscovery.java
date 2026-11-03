package io.github.maynowak.drmays.aws.s3;

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
import software.amazon.awssdk.services.s3.model.GetBucketLocationRequest;
import software.amazon.awssdk.services.s3.model.GetBucketTaggingRequest;
import software.amazon.awssdk.services.s3.model.GetBucketVersioningRequest;
import software.amazon.awssdk.services.s3.model.GetPublicAccessBlockRequest;
import software.amazon.awssdk.services.s3.model.PublicAccessBlockConfiguration;
import software.amazon.awssdk.services.s3.model.S3Exception;
import software.amazon.awssdk.services.s3.model.Tag;

/** Read-only S3 bucket metadata discovery; never reads object contents. */
public final class S3MetadataDiscovery implements InfrastructureDiscovery {

  private static final String DEFAULT_REGION = "us-east-1";

  private final S3ClientProvider clientProvider;
  private final AwsResourceMapper mapper;

  public S3MetadataDiscovery(S3ClientProvider clientProvider, AwsResourceMapper mapper) {
    this.clientProvider = Objects.requireNonNull(clientProvider, "clientProvider");
    this.mapper = Objects.requireNonNull(mapper, "mapper");
  }

  @Override
  public AwsService service() {
    return AwsService.S3;
  }

  @Override
  public DiscoveryResult discover(DiscoveryRequest request) {
    Objects.requireNonNull(request, "request");
    var scope = request.scope();
    var resources = new ArrayList<InfrastructureResource>();
    var failures = new ArrayList<DiscoveryFailure>();
    var identities = new HashSet<String>();

    try {
      var listingClient = clientProvider.client(scope.regions().get(0));
      var buckets =
          listingClient.listBuckets().buckets().stream()
              .sorted(java.util.Comparator.comparing(bucket -> bucket.name()))
              .toList();

      for (var bucket : buckets) {
        if (resources.size() >= scope.maxResources()) {
          failures.add(limitFailure());
          break;
        }
        try {
          var region = bucketRegion(listingClient, bucket.name());
          if (!scope.regions().contains(region)) {
            continue;
          }
          var client = clientProvider.client(region);
          var tags = bucketTags(client, bucket.name());
          var versioning =
              client.getBucketVersioning(
                  GetBucketVersioningRequest.builder().bucket(bucket.name()).build());
          var publicAccessBlock = publicAccessBlock(client, bucket.name());
          var resource =
              mapper.toResource(
                  bucket, region, tags, versioning, publicAccessBlock, scope.expectedAccountId());
          if (!identities.add(resource.identity().canonicalId())) {
            failures.add(
                new DiscoveryFailure(
                    AwsService.S3,
                    DiscoveryFailureCode.DUPLICATE_RESOURCE,
                    "duplicate bucket identity"));
            continue;
          }
          resources.add(resource);
        } catch (RuntimeException exception) {
          failures.add(AwsFailureMapper.failure(AwsService.S3, exception));
        }
      }
    } catch (RuntimeException exception) {
      failures.add(AwsFailureMapper.failure(AwsService.S3, exception));
    }

    return result(resources, failures);
  }

  private static String bucketRegion(
      software.amazon.awssdk.services.s3.S3Client client, String bucket) {
    var location =
        client.getBucketLocation(GetBucketLocationRequest.builder().bucket(bucket).build());
    var value = location.locationConstraintAsString();
    return value == null || value.isBlank() ? DEFAULT_REGION : value;
  }

  private static List<Tag> bucketTags(
      software.amazon.awssdk.services.s3.S3Client client, String bucket) {
    try {
      return client
          .getBucketTagging(GetBucketTaggingRequest.builder().bucket(bucket).build())
          .tagSet();
    } catch (S3Exception exception) {
      if (isMissing(exception, "NoSuchTagSet")) {
        return List.of();
      }
      throw exception;
    }
  }

  private static PublicAccessBlockConfiguration publicAccessBlock(
      software.amazon.awssdk.services.s3.S3Client client, String bucket) {
    try {
      return client
          .getPublicAccessBlock(GetPublicAccessBlockRequest.builder().bucket(bucket).build())
          .publicAccessBlockConfiguration();
    } catch (S3Exception exception) {
      if (isMissing(exception, "NoSuchPublicAccessBlockConfiguration")) {
        return PublicAccessBlockConfiguration.builder().build();
      }
      throw exception;
    }
  }

  private static boolean isMissing(S3Exception exception, String errorCode) {
    return exception.statusCode() == 404;
  }

  private static DiscoveryFailure limitFailure() {
    return new DiscoveryFailure(
        AwsService.S3, DiscoveryFailureCode.RESOURCE_LIMIT, "resource limit reached");
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
