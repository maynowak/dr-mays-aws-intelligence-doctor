package io.github.maynowak.drmays.ai.expertise;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

class ExpertiseProfileTest {

  @Test
  void providesBoundedStandardProfile() {
    var profile = ExpertiseProfile.standard();

    assertThat(profile.id()).isEqualTo("standard");
    assertThat(profile.maxExplanations()).isEqualTo(20);
    assertThat(profile.maxRecommendations()).isEqualTo(20);
    assertThat(profile.maxTextChars()).isEqualTo(4_000);
  }

  @Test
  void providesConciseAndDeepProfiles() {
    assertThat(ExpertiseProfile.concise().maxExplanations()).isEqualTo(5);
    assertThat(ExpertiseProfile.deep().maxExplanations()).isEqualTo(100);
  }

  @Test
  void rejectsInvalidId() {
    assertThatThrownBy(() -> new ExpertiseProfile("Invalid", 1, 1, 10))
        .isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  void rejectsNonPositiveLimits() {
    assertThatThrownBy(() -> new ExpertiseProfile("standard", 0, 1, 10))
        .isInstanceOf(IllegalArgumentException.class);
  }
}
