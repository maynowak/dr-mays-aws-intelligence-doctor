package io.github.maynowak.drmays.ai.expertise;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

class ExpertiseCatalogTest {

  private final ExpertiseCatalog catalog = new ExpertiseCatalog();

  @Test
  void findsStandardProfile() {
    assertThat(catalog.find("standard")).contains(ExpertiseProfile.standard());
    assertThat(catalog.standard().id()).isEqualTo("standard");
  }

  @Test
  void unknownProfileIsEmpty() {
    assertThat(catalog.find("unknown")).isEmpty();
    assertThatThrownBy(() -> catalog.require("unknown"))
        .isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  void rejectsDuplicateProfile() {
    assertThatThrownBy(() -> catalog.register(ExpertiseProfile.standard()))
        .isInstanceOf(IllegalArgumentException.class);
  }
}
