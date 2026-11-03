package io.github.maynowak.drmays.aws.identity;

import io.github.maynowak.drmays.aws.discovery.DiscoveryScope;

/** Verifies the caller against the approved account before discovery. */
public interface AwsIdentityVerifier {

  /**
   * Verifies the caller identity.
   *
   * @param scope approved scope
   * @return verified identity
   * @throws AwsIdentityException when verification fails or the account does not match
   */
  AwsIdentity verify(DiscoveryScope scope);
}
