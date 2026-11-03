package io.github.maynowak.drmays.aws.discovery;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.assertj.core.api.Assertions.assertThatNullPointerException;

import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;

class DiscoveryScopeTest {

  @Test
  void acceptsValidScope() {
    var scope =
        new DiscoveryScope(
            "dr-mays-test",
            "000000000000",
            List.of("eu-west-1"),
            Set.of(AwsService.EC2, AwsService.S3),
            100);

    assertThat(scope.profile()).isEqualTo("dr-mays-test");
    assertThat(scope.services()).containsExactlyInAnyOrder(AwsService.EC2, AwsService.S3);
  }

  @Test
  void rejectsDefaultProfile() {
    assertThatIllegalArgumentException()
        .isThrownBy(() ->
            new DiscoveryScope(
                "default", "000000000000", List.of("eu-west-1"), Set.of(AwsService.EC2), 10))
        .withMessageContaining("default profile");
  }

  @Test
  void rejectsMissingAccountOrRegions() {
    assertThatIllegalArgumentException()
        .isThrownBy(() ->
            new DiscoveryScope(
                "dr-mays-test", "123", List.of("eu-west-1"), Set.of(AwsService.EC2), 10))
        .withMessageContaining("12-digit");
    assertThatIllegalArgumentException()
        .isThrownBy(() ->
            new DiscoveryScope(
                "dr-mays-test", "000000000000", List.of(), Set.of(AwsService.EC2), 10))
        .withMessageContaining("regions");
  }

  @Test
  void rejectsDuplicateOrBlankRegions() {
    assertThatIllegalArgumentException()
        .isThrownBy(() ->
            new DiscoveryScope(
                "dr-mays-test",
                "000000000000",
                List.of("eu-west-1", "eu-west-1"),
                Set.of(AwsService.EC2),
                10))
        .withMessageContaining("duplicate region");
    assertThatIllegalArgumentException()
        .isThrownBy(() ->
            new DiscoveryScope(
                "dr-mays-test", "000000000000", List.of(" "), Set.of(AwsService.EC2), 10))
        .withMessageContaining("region");
  }

  @Test
  void rejectsEmptyServicesOrIdentityOnly() {
    assertThatIllegalArgumentException()
        .isThrownBy(() ->
            new DiscoveryScope("dr-mays-test", "000000000000", List.of("eu-west-1"), Set.of(), 10))
        .withMessageContaining("services");
    assertThatIllegalArgumentException()
        .isThrownBy(() ->
            new DiscoveryScope(
                "dr-mays-test", "000000000000", List.of("eu-west-1"), Set.of(AwsService.STS), 10))
        .withMessageContaining("EC2 or S3");
  }

  @Test
  void rejectsUnboundedResourceLimits() {
    assertThatIllegalArgumentException()
        .isThrownBy(() ->
            new DiscoveryScope(
                "dr-mays-test", "000000000000", List.of("eu-west-1"), Set.of(AwsService.EC2), 0));
    assertThatIllegalArgumentException()
        .isThrownBy(() ->
            new DiscoveryScope(
                "dr-mays-test",
                "000000000000",
                List.of("eu-west-1"),
                Set.of(AwsService.EC2),
                DiscoveryScope.MAX_RESOURCES_LIMIT + 1));
  }

  @Test
  void rejectsNullArguments() {
    assertThatNullPointerException()
        .isThrownBy(() ->
            new DiscoveryScope(
                null, "000000000000", List.of("eu-west-1"), Set.of(AwsService.EC2), 10))
        .withMessage("profile");
    assertThatNullPointerException()
        .isThrownBy(() ->
            new DiscoveryScope("dr-mays-test", "000000000000", null, Set.of(AwsService.EC2), 10))
        .withMessage("regions");
  }
}
