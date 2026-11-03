package io.github.maynowak.drmays.core.rule;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.assertj.core.api.Assertions.assertThatNullPointerException;

import org.junit.jupiter.api.Test;

class RuleIdTest {

  @Test
  void acceptsStableUppercaseIdentifiers() {
    assertThat(new RuleId("TAG-OWNER-001").value()).isEqualTo("TAG-OWNER-001");
    assertThat(new RuleId("A").value()).isEqualTo("A");
  }

  @Test
  void rejectsMalformedIdentifiers() {
    assertThatIllegalArgumentException().isThrownBy(() -> new RuleId("tag-owner-001"));
    assertThatIllegalArgumentException().isThrownBy(() -> new RuleId("TAG_OWNER_001"));
    assertThatIllegalArgumentException().isThrownBy(() -> new RuleId("TAG--001"));
    assertThatIllegalArgumentException().isThrownBy(() -> new RuleId("TAG-001 "));
    assertThatIllegalArgumentException().isThrownBy(() -> new RuleId("-TAG-001"));
  }

  @Test
  void rejectsBlankOrNullValues() {
    assertThatIllegalArgumentException()
        .isThrownBy(() -> new RuleId(" "))
        .withMessage("value must not be blank");
    assertThatNullPointerException().isThrownBy(() -> new RuleId(null)).withMessage("value");
  }

  @Test
  void rejectsValuesLongerThan64Characters() {
    var tooLong = "A".repeat(65);
    assertThatIllegalArgumentException().isThrownBy(() -> new RuleId(tooLong));
  }
}
