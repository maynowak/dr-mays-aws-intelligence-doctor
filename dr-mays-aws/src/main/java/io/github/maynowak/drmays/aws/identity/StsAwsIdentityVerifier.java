package io.github.maynowak.drmays.aws.identity;

import io.github.maynowak.drmays.aws.discovery.DiscoveryFailureCode;
import io.github.maynowak.drmays.aws.discovery.DiscoveryScope;
import io.github.maynowak.drmays.aws.failure.AwsFailureMapper;
import java.util.Objects;
import software.amazon.awssdk.services.sts.StsClient;
import software.amazon.awssdk.services.sts.model.GetCallerIdentityRequest;

/** STS-based identity verification; fail closed on mismatch or any SDK failure. */
public final class StsAwsIdentityVerifier implements AwsIdentityVerifier {

  private final StsClient stsClient;

  public StsAwsIdentityVerifier(StsClient stsClient) {
    this.stsClient = Objects.requireNonNull(stsClient, "stsClient");
  }

  @Override
  public AwsIdentity verify(DiscoveryScope scope) {
    Objects.requireNonNull(scope, "scope");
    try {
      var response = stsClient.getCallerIdentity(GetCallerIdentityRequest.builder().build());
      if (response.account() == null || response.account().isBlank()) {
        throw new AwsIdentityException(
            DiscoveryFailureCode.MALFORMED_RESPONSE, "STS returned no account identity");
      }
      if (!scope.expectedAccountId().equals(response.account())) {
        throw new AwsIdentityException(
            DiscoveryFailureCode.IDENTITY_MISMATCH,
            "Caller account does not match the approved account");
      }
      return new AwsIdentity(response.account(), response.arn(), response.userId());
    } catch (AwsIdentityException exception) {
      throw exception;
    } catch (RuntimeException exception) {
      throw new AwsIdentityException(
          AwsFailureMapper.mapCode(exception), "STS identity verification failed");
    }
  }
}
