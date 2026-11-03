package io.github.maynowak.drmays.core.validation;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.assertj.core.api.Assertions.assertThatNullPointerException;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.LinkedHashMap;
import java.util.Map;
import org.junit.jupiter.api.Test;

class DomainChecksTest {

  @Test
  void requiresNonBlankText() {
    assertThat(DomainChecks.requireText("value", "field")).isEqualTo("value");
    assertThatIllegalArgumentException()
        .isThrownBy(() -> DomainChecks.requireText("  ", "field"))
        .withMessage("field must not be blank");
    assertThatNullPointerException()
        .isThrownBy(() -> DomainChecks.requireText(null, "field"))
        .withMessage("field");
  }

  @Test
  void copiesTextMapImmutablyAndPreservesOrder() {
    var input = new LinkedHashMap<String, String>();
    input.put("first", "1");
    input.put("second", "2");

    var copy = DomainChecks.immutableTextMap(input, "details");

    assertThat(copy).containsExactly(Map.entry("first", "1"), Map.entry("second", "2"));
    assertThatThrownBy(() -> copy.put("third", "3"))
        .isInstanceOf(UnsupportedOperationException.class);
  }

  @Test
  void rejectsInvalidMapEntries() {
    var nullValue = new LinkedHashMap<String, String>();
    nullValue.put("key", null);

    assertThatIllegalArgumentException()
        .isThrownBy(() -> DomainChecks.immutableTextMap(Map.of(" ", "value"), "tags"))
        .withMessage("tags key must not be blank");
    assertThatNullPointerException()
        .isThrownBy(() -> DomainChecks.immutableTextMap(nullValue, "tags"))
        .withMessage("tags value for key");
  }
}
