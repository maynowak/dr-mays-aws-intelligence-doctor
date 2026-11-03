package io.github.maynowak.drmays.aws.discovery;

import static io.github.maynowak.drmays.aws.AwsTestFixtures.ACCOUNT;
import static io.github.maynowak.drmays.aws.AwsTestFixtures.failure;
import static io.github.maynowak.drmays.aws.AwsTestFixtures.resource;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.assertj.core.api.Assertions.assertThatNullPointerException;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import io.github.maynowak.drmays.aws.identity.AwsIdentity;
import io.github.maynowak.drmays.aws.identity.AwsIdentityException;
import io.github.maynowak.drmays.aws.identity.AwsIdentityVerifier;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;

class AwsDiscoveryAdapterTest {

  private final AwsIdentityVerifier verifier = mock(AwsIdentityVerifier.class);

  @Test
  void failsClosedBeforeDiscoveryOnIdentityMismatch() {
    var scope = scope(10, AwsService.EC2);
    when(verifier.verify(scope))
        .thenThrow(new AwsIdentityException(DiscoveryFailureCode.IDENTITY_MISMATCH, "mismatch"));
    var ec2 =
        new StubDiscovery(
            AwsService.EC2, DiscoveryResult.complete(List.of(resource("network", "net-1"))));

    var result =
        new AwsDiscoveryAdapter(verifier, List.of(ec2)).discover(new DiscoveryRequest(scope));

    assertThat(result.status()).isEqualTo(DiscoveryStatus.FAILED);
    assertThat(result.failures().getFirst().code())
        .isEqualTo(DiscoveryFailureCode.IDENTITY_MISMATCH);
    assertThat(ec2.calls).isZero();
  }

  @Test
  void failsClosedOnUnexpectedVerificationError() {
    var scope = scope(10, AwsService.EC2);
    when(verifier.verify(scope)).thenThrow(new RuntimeException("secret"));

    var result = new AwsDiscoveryAdapter(verifier, List.of()).discover(new DiscoveryRequest(scope));

    assertThat(result.status()).isEqualTo(DiscoveryStatus.FAILED);
    assertThat(result.failures().getFirst().message()).doesNotContain("secret");
  }

  @Test
  void combinesServiceResults() {
    var scope = scope(10, AwsService.EC2, AwsService.S3);
    verify(scope);
    var ec2 =
        new StubDiscovery(
            AwsService.EC2, DiscoveryResult.complete(List.of(resource("network", "net-1"))));
    var s3 =
        new StubDiscovery(
            AwsService.S3, DiscoveryResult.complete(List.of(resource("s3-bucket", "bucket-1"))));

    var result =
        new AwsDiscoveryAdapter(verifier, List.of(s3, ec2)).discover(new DiscoveryRequest(scope));

    assertThat(result.status()).isEqualTo(DiscoveryStatus.COMPLETE);
    assertThat(result.resources())
        .extracting(resource -> resource.identity().resourceId())
        .containsExactly("net-1", "bucket-1");
  }

  @Test
  void propagatesPartialServiceResults() {
    var scope = scope(10, AwsService.EC2, AwsService.S3);
    verify(scope);
    var ec2 =
        new StubDiscovery(
            AwsService.EC2,
            DiscoveryResult.partial(
                List.of(resource("network", "net-1")),
                List.of(failure(AwsService.EC2, DiscoveryFailureCode.THROTTLING, "throttled"))));
    var s3 =
        new StubDiscovery(
            AwsService.S3, DiscoveryResult.complete(List.of(resource("s3-bucket", "bucket-1"))));

    var result =
        new AwsDiscoveryAdapter(verifier, List.of(ec2, s3)).discover(new DiscoveryRequest(scope));

    assertThat(result.status()).isEqualTo(DiscoveryStatus.PARTIAL);
    assertThat(result.resources()).hasSize(2);
    assertThat(result.failures()).hasSize(1);
  }

  @Test
  void reportsMissingServiceRegistration() {
    var scope = scope(10, AwsService.EC2, AwsService.S3);
    verify(scope);
    var ec2 =
        new StubDiscovery(
            AwsService.EC2, DiscoveryResult.complete(List.of(resource("network", "net-1"))));

    var result =
        new AwsDiscoveryAdapter(verifier, List.of(ec2)).discover(new DiscoveryRequest(scope));

    assertThat(result.status()).isEqualTo(DiscoveryStatus.PARTIAL);
    assertThat(result.failures().getFirst().code()).isEqualTo(DiscoveryFailureCode.INVALID_SCOPE);
  }

  @Test
  void enforcesGlobalResourceLimit() {
    var scope = scope(1, AwsService.EC2, AwsService.S3);
    verify(scope);
    var ec2 =
        new StubDiscovery(
            AwsService.EC2, DiscoveryResult.complete(List.of(resource("network", "net-1"))));
    var s3 =
        new StubDiscovery(
            AwsService.S3, DiscoveryResult.complete(List.of(resource("s3-bucket", "bucket-1"))));

    var result =
        new AwsDiscoveryAdapter(verifier, List.of(ec2, s3)).discover(new DiscoveryRequest(scope));

    assertThat(result.resources()).hasSize(1);
    assertThat(result.status()).isEqualTo(DiscoveryStatus.PARTIAL);
    assertThat(result.failures())
        .extracting(failure -> failure.code())
        .contains(DiscoveryFailureCode.RESOURCE_LIMIT);
  }

  @Test
  void detectsDuplicateResourcesAcrossServices() {
    var scope = scope(10, AwsService.EC2, AwsService.S3);
    verify(scope);
    var duplicate = resource("network", "net-1");
    var ec2 = new StubDiscovery(AwsService.EC2, DiscoveryResult.complete(List.of(duplicate)));
    var s3 = new StubDiscovery(AwsService.S3, DiscoveryResult.complete(List.of(duplicate)));

    var result =
        new AwsDiscoveryAdapter(verifier, List.of(ec2, s3)).discover(new DiscoveryRequest(scope));

    assertThat(result.status()).isEqualTo(DiscoveryStatus.PARTIAL);
    assertThat(result.failures().getFirst().code())
        .isEqualTo(DiscoveryFailureCode.DUPLICATE_RESOURCE);
  }

  @Test
  void rejectsDuplicateOrIdentityDiscoveries() {
    assertThatIllegalArgumentException()
        .isThrownBy(() ->
            new AwsDiscoveryAdapter(
                verifier,
                List.of(
                    new StubDiscovery(AwsService.EC2, DiscoveryResult.complete(List.of())),
                    new StubDiscovery(AwsService.EC2, DiscoveryResult.complete(List.of())))))
        .withMessageContaining("duplicate discovery service");
    assertThatIllegalArgumentException()
        .isThrownBy(() ->
            new AwsDiscoveryAdapter(
                verifier,
                List.of(new StubDiscovery(AwsService.STS, DiscoveryResult.complete(List.of())))))
        .withMessageContaining("STS");
  }

  @Test
  void rejectsNullRequest() {
    assertThatNullPointerException()
        .isThrownBy(() -> new AwsDiscoveryAdapter(verifier, List.of()).discover(null))
        .withMessage("request");
  }

  private void verify(DiscoveryScope scope) {
    when(verifier.verify(scope))
        .thenReturn(new AwsIdentity(ACCOUNT, "arn:aws:iam::000000000000:user/test", "AIDA"));
  }

  private static DiscoveryScope scope(int maxResources, AwsService... services) {
    return new DiscoveryScope(
        "dr-mays-test", ACCOUNT, List.of("eu-west-1"), Set.of(services), maxResources);
  }

  private static final class StubDiscovery implements InfrastructureDiscovery {
    private final AwsService service;
    private final DiscoveryResult result;
    private int calls;

    private StubDiscovery(AwsService service, DiscoveryResult result) {
      this.service = service;
      this.result = result;
    }

    @Override
    public AwsService service() {
      return service;
    }

    @Override
    public DiscoveryResult discover(DiscoveryRequest request) {
      calls++;
      return result;
    }
  }
}
