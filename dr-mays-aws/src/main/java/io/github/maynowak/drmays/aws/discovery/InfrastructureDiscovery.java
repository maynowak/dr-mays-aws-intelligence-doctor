package io.github.maynowak.drmays.aws.discovery;

/** Service-level discovery contract. */
public interface InfrastructureDiscovery {

  /**
   * @return the AWS service handled by this discovery
   */
  AwsService service();

  /**
   * Performs a scoped discovery.
   *
   * @param request approved request
   * @return deterministic discovery result
   */
  DiscoveryResult discover(DiscoveryRequest request);
}
