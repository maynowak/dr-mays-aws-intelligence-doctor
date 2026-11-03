package io.github.maynowak.drmays.aws.identity;

import static io.github.maynowak.drmays.aws.AwsTestFixtures.ACCOUNT;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import io.github.maynowak.drmays.aws.discovery.AwsService;
import io.github.maynowak.drmays.aws.discovery.DiscoveryFailureCode;
import io.github.maynowak.drmays.aws.discovery.DiscoveryScope;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;
import software.amazon.awssdk.services.sts.StsClient;
import software.amazon.awssdk.services.sts.model.GetCallerIdentityRequest;
import software.amazon.awssdk.services.sts.model.GetCallerIdentityResponse;
import software.amazon.awssdk.services.sts.model.StsException;

class StsAwsIdentityVerifierTest {

  private final DiscoveryScope scope =
      new DiscoveryScope("dr-mays-test", ACCOUNT, List.of("eu-west-1"), Set.of(AwsService.EC2), 10);

  @Test
  void returnsVerifiedIdentity() {
    var client = mock(StsClient.class);
    when(client.getCallerIdentity(any(GetCallerIdentityRequest.class)))
        .thenReturn(
            GetCallerIdentityResponse.builder()
                .account(ACCOUNT)
                .arn("arn:aws:iam::000000000000:user/test")
                .userId("AIDAEXAMPLE")
                .build());

    var identity = new StsAwsIdentityVerifier(client).verify(scope);

    assertThat(identity.accountId()).isEqualTo(ACCOUNT);
    assertThat(identity.arn()).contains("user/test");
  }

  @Test
  void failsClosedOnAccountMismatch() {
    var client = mock(StsClient.class);
    when(client.getCallerIdentity(any(GetCallerIdentityRequest.class)))
        .thenReturn(
            GetCallerIdentityResponse.builder()
                .account("999999999999")
                .arn("arn:aws:iam::999999999999:user/test")
                .userId("AIDAEXAMPLE")
                .build());

    assertThatThrownBy(() -> new StsAwsIdentityVerifier(client).verify(scope))
        .isInstanceOfSatisfying(
            AwsIdentityException.class,
            exception ->
                assertThat(exception.code()).isEqualTo(DiscoveryFailureCode.IDENTITY_MISMATCH));
  }

  @Test
  void failsClosedWhenAccountIsMissing() {
    var client = mock(StsClient.class);
    when(client.getCallerIdentity(any(GetCallerIdentityRequest.class)))
        .thenReturn(GetCallerIdentityResponse.builder().build());

    assertThatThrownBy(() -> new StsAwsIdentityVerifier(client).verify(scope))
        .isInstanceOfSatisfying(
            AwsIdentityException.class,
            exception ->
                assertThat(exception.code()).isEqualTo(DiscoveryFailureCode.MALFORMED_RESPONSE));
  }

  @Test
  void mapsSdkFailureWithoutLeakingMessage() {
    var client = mock(StsClient.class);
    when(client.getCallerIdentity(any(GetCallerIdentityRequest.class)))
        .thenThrow(StsException.builder().statusCode(403).message("secret").build());

    assertThatThrownBy(() -> new StsAwsIdentityVerifier(client).verify(scope))
        .isInstanceOfSatisfying(
            AwsIdentityException.class,
            exception -> {
              assertThat(exception.code()).isEqualTo(DiscoveryFailureCode.ACCESS_DENIED);
              assertThat(exception.getMessage()).doesNotContain("secret");
            });
  }

  @Test
  void rejectsNullScopeAndClient() {
    assertThatThrownBy(() -> new StsAwsIdentityVerifier(null))
        .isInstanceOf(NullPointerException.class)
        .hasMessage("stsClient");
    assertThatThrownBy(() -> new StsAwsIdentityVerifier(mock(StsClient.class)).verify(null))
        .isInstanceOf(NullPointerException.class)
        .hasMessage("scope");
  }
}
