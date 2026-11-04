package io.github.maynowak.drmays.ai.expertise;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

class ExpertiseParserTest {

  private final ExpertiseParser parser = new ExpertiseParser();

  @Test
  void parsesStructuredJson() {
    var response =
        parser.parse(
            """
            {"summary":"ok","explanations":[],"recommendations":[],"assumptions":[],
             "missingInformation":[],"uncertainties":[]}
            """);

    assertThat(response.summary()).isEqualTo("ok");
    assertThat(response.explanations()).isEmpty();
  }

  @Test
  void parsesFencedJson() {
    var response =
        parser.parse(
            """
            ```json
            {"summary":"fenced","explanations":[],"recommendations":[]}
            ```
            """);

    assertThat(response.summary()).isEqualTo("fenced");
  }

  @Test
  void normalizesMissingLists() {
    var response = parser.parse("{\"summary\":\"only\"}");

    assertThat(response.explanations()).isEmpty();
    assertThat(response.recommendations()).isEmpty();
    assertThat(response.uncertainties()).isEmpty();
  }

  @Test
  void rejectsBlankOutput() {
    assertThatThrownBy(() -> parser.parse(" ")).isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  void rejectsMalformedJson() {
    assertThatThrownBy(() -> parser.parse("{not json"))
        .isInstanceOf(IllegalArgumentException.class);
  }
}
