package io.github.maynowak.drmays.aws.client;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.assertj.core.api.Assertions.assertThatNullPointerException;

import org.junit.jupiter.api.Test;

class AwsClientConfigTest {

  @Test
  void acceptsExplicitProfileAndRegion() {
    var config = new AwsClientConfig("dr-mays-test", "eu-west-1");

    assertThat(config.profile()).isEqualTo("dr-mays-test");
    assertThat(config.region()).isEqualTo("eu-west-1");
  }

  @Test
  void rejectsDefaultProfile() {
    assertThatIllegalArgumentException()
        .isThrownBy(() -> new AwsClientConfig("DEFAULT", "eu-west-1"))
        .withMessageContaining("default profile");
  }

  @Test
  void rejectsBlankValues() {
    assertThatIllegalArgumentException()
        .isThrownBy(() -> new AwsClientConfig(" ", "eu-west-1"))
        .withMessage("profile must not be blank");
    assertThatIllegalArgumentException()
        .isThrownBy(() -> new AwsClientConfig("dr-mays-test", ""))
        .withMessage("region must not be blank");
  }

  @Test
  void rejectsNullValues() {
    assertThatNullPointerException()
        .isThrownBy(() -> new AwsClientConfig(null, "eu-west-1"))
        .withMessage("profile");
  }
}
