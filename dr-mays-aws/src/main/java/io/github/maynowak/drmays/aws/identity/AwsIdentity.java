package io.github.maynowak.drmays.aws.identity;

import io.github.maynowak.drmays.core.validation.DomainChecks;

/**
 * Caller identity resolved from STS.
 *
 * @param accountId 12-digit account ID
 * @param arn caller ARN
 * @param userId caller user ID
 */
public record AwsIdentity(String accountId, String arn, String userId) {

  public AwsIdentity {
    accountId = DomainChecks.requireText(accountId, "accountId");
    arn = DomainChecks.requireText(arn, "arn");
    userId = DomainChecks.requireText(userId, "userId");
  }
}
