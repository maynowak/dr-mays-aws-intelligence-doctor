package io.github.maynowak.drmays.aws.client;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class AwsClientFactoryTest {

  private final AwsClientFactory factory = new AwsClientFactory();
  private final AwsClientConfig config = new AwsClientConfig("dr-mays-test", "eu-west-1");

  @Test
  void buildsClientsWithoutMakingNetworkCalls() {
    try (var ec2 = factory.ec2Client(config);
        var s3 = factory.s3Client(config);
        var sts = factory.stsClient(config)) {
      assertThat(ec2).isNotNull();
      assertThat(s3).isNotNull();
      assertThat(sts).isNotNull();
    }
  }
}
