package io.github.maynowak.drmays.aws.discovery;

import static io.github.maynowak.drmays.aws.AwsTestFixtures.failure;
import static io.github.maynowak.drmays.aws.AwsTestFixtures.resource;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

import java.util.List;
import org.junit.jupiter.api.Test;

class DiscoveryResultTest {

  @Test
  void sortsResourcesAndFailures() {
    var result =
        DiscoveryResult.partial(
            List.of(resource("network", "net-2"), resource("network", "net-1")),
            List.of(
                failure(AwsService.S3, DiscoveryFailureCode.ACCESS_DENIED, "denied"),
                failure(AwsService.EC2, DiscoveryFailureCode.TIMEOUT, "timeout")));

    assertThat(result.resources())
        .extracting(resource -> resource.identity().resourceId())
        .containsExactly("net-1", "net-2");
    assertThat(result.failures())
        .extracting(DiscoveryFailure::service)
        .containsExactly(AwsService.EC2, AwsService.S3);
  }

  @Test
  void rejectsCompleteResultWithFailures() {
    assertThatIllegalArgumentException()
        .isThrownBy(() ->
            new DiscoveryResult(
                List.of(resource("network", "net-1")),
                List.of(failure(AwsService.EC2, DiscoveryFailureCode.TIMEOUT, "timeout")),
                DiscoveryStatus.COMPLETE));
  }

  @Test
  void rejectsPartialWithoutBothParts() {
    assertThatIllegalArgumentException()
        .isThrownBy(() ->
            new DiscoveryResult(
                List.of(),
                List.of(failure(AwsService.EC2, DiscoveryFailureCode.TIMEOUT, "x")),
                DiscoveryStatus.PARTIAL));
    assertThatIllegalArgumentException()
        .isThrownBy(() ->
            new DiscoveryResult(
                List.of(resource("network", "net-1")), List.of(), DiscoveryStatus.PARTIAL));
  }

  @Test
  void rejectsFailedResultWithResources() {
    assertThatIllegalArgumentException()
        .isThrownBy(() ->
            new DiscoveryResult(
                List.of(resource("network", "net-1")),
                List.of(failure(AwsService.EC2, DiscoveryFailureCode.TIMEOUT, "x")),
                DiscoveryStatus.FAILED));
  }

  @Test
  void rejectsDuplicateResourceIdentities() {
    var duplicate = resource("network", "net-1");

    assertThatIllegalArgumentException()
        .isThrownBy(() ->
            new DiscoveryResult(List.of(duplicate, duplicate), List.of(), DiscoveryStatus.COMPLETE))
        .withMessageContaining("duplicate resource identity");
  }

  @Test
  void createsFailedResultWithoutResources() {
    var result =
        DiscoveryResult.failed(
            List.of(failure(AwsService.S3, DiscoveryFailureCode.ACCESS_DENIED, "denied")));

    assertThat(result.status()).isEqualTo(DiscoveryStatus.FAILED);
    assertThat(result.resources()).isEmpty();
  }
}
