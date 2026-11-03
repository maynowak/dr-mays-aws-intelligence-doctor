package io.github.maynowak.drmays.aws.client;

import java.time.Duration;
import software.amazon.awssdk.auth.credentials.ProfileCredentialsProvider;
import software.amazon.awssdk.core.client.config.ClientOverrideConfiguration;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.ec2.Ec2Client;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.sts.StsClient;

/**
 * Creates AWS clients only on demand with explicit profile, region and bounded retries.
 *
 * <p>This class is deliberately not a Spring component; application startup must not open AWS
 * connections or resolve credentials. The default credentials provider is never used.
 */
public final class AwsClientFactory {

  private static final Duration API_CALL_TIMEOUT = Duration.ofSeconds(15);
  private static final Duration ATTEMPT_TIMEOUT = Duration.ofSeconds(5);
  private static final int MAX_RETRIES = 2;

  public Ec2Client ec2Client(AwsClientConfig config) {
    return Ec2Client.builder()
        .credentialsProvider(ProfileCredentialsProvider.create(config.profile()))
        .region(Region.of(config.region()))
        .overrideConfiguration(overrideConfiguration())
        .build();
  }

  public S3Client s3Client(AwsClientConfig config) {
    return S3Client.builder()
        .credentialsProvider(ProfileCredentialsProvider.create(config.profile()))
        .region(Region.of(config.region()))
        .overrideConfiguration(overrideConfiguration())
        .build();
  }

  public StsClient stsClient(AwsClientConfig config) {
    return StsClient.builder()
        .credentialsProvider(ProfileCredentialsProvider.create(config.profile()))
        .region(Region.of(config.region()))
        .overrideConfiguration(overrideConfiguration())
        .build();
  }

  private static ClientOverrideConfiguration overrideConfiguration() {
    return ClientOverrideConfiguration.builder()
        .apiCallTimeout(API_CALL_TIMEOUT)
        .apiCallAttemptTimeout(ATTEMPT_TIMEOUT)
        .retryStrategy(strategy -> strategy.maxAttempts(MAX_RETRIES + 1))
        .build();
  }
}
