package io.github.maynowak.drmays.ai;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class IntelligenceConfigurationTest {

  @Test
  void defaultConfigurationUsesDeterministicMock() {
    assertThat(new IntelligenceConfiguration().mockIntelligenceAnalyzer())
        .isInstanceOf(MockIntelligenceAnalyzer.class);
  }
}
