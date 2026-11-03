package io.github.maynowak.drmays.aws.s3;

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
import software.amazon.awssdk.awscore.exception.AwsErrorDetails;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.Bucket;
import software.amazon.awssdk.services.s3.model.BucketLocationConstraint;
import software.amazon.awssdk.services.s3.model.BucketVersioningStatus;
import software.amazon.awssdk.services.s3.model.GetBucketLocationRequest;
import software.amazon.awssdk.services.s3.model.GetBucketLocationResponse;
import software.amazon.awssdk.services.s3.model.GetBucketTaggingRequest;
import software.amazon.awssdk.services.s3.model.GetBucketTaggingResponse;
import software.amazon.awssdk.services.s3.model.GetBucketVersioningRequest;
import software.amazon.awssdk.services.s3.model.GetBucketVersioningResponse;
import software.amazon.awssdk.services.s3.model.GetPublicAccessBlockRequest;
import software.amazon.awssdk.services.s3.model.GetPublicAccessBlockResponse;
import software.amazon.awssdk.services.s3.model.ListBucketsResponse;
import software.amazon.awssdk.services.s3.model.PublicAccessBlockConfiguration;
import software.amazon.awssdk.services.s3.model.S3Exception;
import software.amazon.awssdk.services.s3.model.Tag;

class S3MetadataDiscoveryTest {

  private final AwsResourceMapper mapper = new AwsResourceMapper();

  @Test
  void mapsBucketMetadataCompletely() {
    var client = mock(S3Client.class);
    when(client.listBuckets()).thenReturn(buckets("bucket-1"));
    when(client.getBucketLocation(any(GetBucketLocationRequest.class)))
        .thenReturn(location(REGION));
    when(client.getBucketTagging(any(GetBucketTaggingRequest.class)))
        .thenReturn(tags("owner", "platform"));
    when(client.getBucketVersioning(any(GetBucketVersioningRequest.class)))
        .thenReturn(versioning(BucketVersioningStatus.ENABLED));
    when(client.getPublicAccessBlock(any(GetPublicAccessBlockRequest.class)))
        .thenReturn(publicAccessBlock(true));

    var result = discovery(client).discover(request(10));

    assertThat(result.status()).isEqualTo(DiscoveryStatus.COMPLETE);
    assertThat(result.resources()).hasSize(1);
    assertThat(result.resources().getFirst().attributes())
        .containsEntry("s3.versioning.status", "Enabled")
        .containsEntry("s3.publicAccessBlock.enabled", "true");
    assertThat(result.resources().getFirst().tags()).containsEntry("owner", "platform");
  }

  @Test
  void filtersBucketsOutsideAllowedRegions() {
    var client = mock(S3Client.class);
    when(client.listBuckets()).thenReturn(buckets("bucket-1"));
    when(client.getBucketLocation(any(GetBucketLocationRequest.class)))
        .thenReturn(GetBucketLocationResponse.builder().build());

    var result = discovery(client).discover(request(10));

    assertThat(result.status()).isEqualTo(DiscoveryStatus.COMPLETE);
    assertThat(result.resources()).isEmpty();
  }

  @Test
  void treatsNoSuchTagSetAsEmptyTags() {
    var client = mock(S3Client.class);
    when(client.listBuckets()).thenReturn(buckets("bucket-1"));
    when(client.getBucketLocation(any(GetBucketLocationRequest.class)))
        .thenReturn(location(REGION));
    when(client.getBucketTagging(any(GetBucketTaggingRequest.class)))
        .thenThrow(s3(404, "NoSuchTagSet"));
    when(client.getBucketVersioning(any(GetBucketVersioningRequest.class)))
        .thenReturn(versioning(BucketVersioningStatus.SUSPENDED));
    when(client.getPublicAccessBlock(any(GetPublicAccessBlockRequest.class)))
        .thenReturn(publicAccessBlock(true));

    var result = discovery(client).discover(request(10));

    assertThat(result.resources()).hasSize(1);
    assertThat(result.resources().getFirst().tags()).isEmpty();
  }

  @Test
  void treatsMissingPublicAccessBlockAsNotConfigured() {
    var client = mock(S3Client.class);
    when(client.listBuckets()).thenReturn(buckets("bucket-1"));
    when(client.getBucketLocation(any(GetBucketLocationRequest.class)))
        .thenReturn(location(REGION));
    when(client.getBucketTagging(any(GetBucketTaggingRequest.class)))
        .thenReturn(tags("owner", "platform"));
    when(client.getBucketVersioning(any(GetBucketVersioningRequest.class)))
        .thenReturn(versioning(BucketVersioningStatus.ENABLED));
    when(client.getPublicAccessBlock(any(GetPublicAccessBlockRequest.class)))
        .thenReturn(GetPublicAccessBlockResponse.builder().build());

    var result = discovery(client).discover(request(10));

    assertThat(result.resources().getFirst().attribute("s3.publicAccessBlock.configured"))
        .contains("false");
  }

  @Test
  void mapsAccessDeniedOnTagsToFailedResult() {
    var client = mock(S3Client.class);
    when(client.listBuckets()).thenReturn(buckets("bucket-1"));
    when(client.getBucketLocation(any(GetBucketLocationRequest.class)))
        .thenReturn(location(REGION));
    when(client.getBucketTagging(any(GetBucketTaggingRequest.class)))
        .thenThrow(s3(403, "AccessDenied"));

    var result = discovery(client).discover(request(10));

    assertThat(result.status()).isEqualTo(DiscoveryStatus.FAILED);
    assertThat(result.failures().getFirst().code()).isEqualTo(DiscoveryFailureCode.ACCESS_DENIED);
  }

  @Test
  void mapsListBucketsFailureToFailedResult() {
    var client = mock(S3Client.class);
    when(client.listBuckets()).thenThrow(s3(403, "AccessDenied"));

    var result = discovery(client).discover(request(10));

    assertThat(result.status()).isEqualTo(DiscoveryStatus.FAILED);
    assertThat(result.resources()).isEmpty();
    assertThat(result.failures()).hasSize(1);
  }

  @Test
  void enforcesResourceLimitAsPartial() {
    var client = mock(S3Client.class);
    when(client.listBuckets()).thenReturn(buckets("bucket-1", "bucket-2"));
    when(client.getBucketLocation(any(GetBucketLocationRequest.class)))
        .thenReturn(location(REGION));
    when(client.getBucketTagging(any(GetBucketTaggingRequest.class)))
        .thenReturn(tags("owner", "platform"));
    when(client.getBucketVersioning(any(GetBucketVersioningRequest.class)))
        .thenReturn(versioning(BucketVersioningStatus.ENABLED));
    when(client.getPublicAccessBlock(any(GetPublicAccessBlockRequest.class)))
        .thenReturn(publicAccessBlock(true));

    var result = discovery(client).discover(request(1));

    assertThat(result.status()).isEqualTo(DiscoveryStatus.PARTIAL);
    assertThat(result.resources()).hasSize(1);
    assertThat(result.failures().getFirst().code()).isEqualTo(DiscoveryFailureCode.RESOURCE_LIMIT);
  }

  @Test
  void returnsCompleteEmptyResultForNoBuckets() {
    var client = mock(S3Client.class);
    when(client.listBuckets()).thenReturn(ListBucketsResponse.builder().build());

    var result = discovery(client).discover(request(10));

    assertThat(result.status()).isEqualTo(DiscoveryStatus.COMPLETE);
    assertThat(result.resources()).isEmpty();
  }

  private S3MetadataDiscovery discovery(S3Client client) {
    return new S3MetadataDiscovery(region -> client, mapper);
  }

  private static DiscoveryRequest request(int maxResources) {
    return new DiscoveryRequest(
        new DiscoveryScope(
            "dr-mays-test", ACCOUNT, List.of(REGION), Set.of(AwsService.S3), maxResources));
  }

  private static ListBucketsResponse buckets(String... names) {
    return ListBucketsResponse.builder()
        .buckets(
            java.util.Arrays.stream(names)
                .map(name -> Bucket.builder().name(name).build())
                .toList())
        .build();
  }

  private static GetBucketLocationResponse location(String region) {
    return GetBucketLocationResponse.builder()
        .locationConstraint("eu-west-1".equals(region) ? BucketLocationConstraint.EU_WEST_1 : null)
        .build();
  }

  private static GetBucketTaggingResponse tags(String key, String value) {
    return GetBucketTaggingResponse.builder()
        .tagSet(Tag.builder().key(key).value(value).build())
        .build();
  }

  private static GetBucketVersioningResponse versioning(BucketVersioningStatus status) {
    return GetBucketVersioningResponse.builder().status(status).build();
  }

  private static GetPublicAccessBlockResponse publicAccessBlock(boolean enabled) {
    return GetPublicAccessBlockResponse.builder()
        .publicAccessBlockConfiguration(
            PublicAccessBlockConfiguration.builder()
                .blockPublicAcls(enabled)
                .ignorePublicAcls(enabled)
                .blockPublicPolicy(enabled)
                .restrictPublicBuckets(enabled)
                .build())
        .build();
  }

  private static S3Exception s3(int status, String code) {
    var builder = S3Exception.builder();
    builder.statusCode(status);
    builder.awsErrorDetails(AwsErrorDetails.builder().errorCode(code).build());
    return (S3Exception) builder.build();
  }
}
