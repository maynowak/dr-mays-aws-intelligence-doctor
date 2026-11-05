package io.github.maynowak.drmays.core.report;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class MarkdownEscaperTest {

  @Test
  void escapesMarkdownAndHtmlControlCharacters() {
    var escaped = MarkdownEscaper.escape("& <script> [x](javascript:alert(1)) | # ` * _ ! ~");

    assertThat(escaped).contains("&amp;");
    assertThat(escaped).contains("&lt;script&gt;");
    assertThat(escaped).doesNotContain("<script>");
    assertThat(escaped).doesNotContain("](javascript:");
    assertThat(escaped).contains("\\|");
    assertThat(escaped).contains("\\#");
    assertThat(escaped).contains("\\`");
    assertThat(escaped).contains("\\*");
    assertThat(escaped).contains("\\_");
    assertThat(escaped).contains("\\!");
    assertThat(escaped).contains("\\~");
  }

  @Test
  void flattensLineSeparatorsAndBidiControls() {
    var escaped = MarkdownEscaper.escape("a\nb\r\nc\u2028d\u2029e\u202Ef");

    assertThat(escaped).doesNotContain("\n").doesNotContain("\r");
    assertThat(escaped).doesNotContain("\u2028").doesNotContain("\u2029").doesNotContain("\u202E");
  }

  @Test
  void truncatesLongFields() {
    var escaped = MarkdownEscaper.escape("x".repeat(5_000));

    assertThat(escaped).endsWith(" [truncated]");
    assertThat(escaped.length()).isLessThan(5_000);
  }
}
