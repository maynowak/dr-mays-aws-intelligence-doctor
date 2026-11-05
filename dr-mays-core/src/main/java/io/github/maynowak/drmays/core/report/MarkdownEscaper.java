package io.github.maynowak.drmays.core.report;

/** Escapes untrusted values for safe Markdown rendering. */
final class MarkdownEscaper {

  private static final int MAX_FIELD_CHARS = 4_000;

  private MarkdownEscaper() {}

  static String escape(String value) {
    if (value == null) {
      return "";
    }
    var input = value.replace("\r\n", " ").replace('\n', ' ').replace('\r', ' ');
    var output = new StringBuilder(input.length() + 16);
    for (var index = 0; index < input.length(); index++) {
      var character = input.charAt(index);
      if (Character.isISOControl(character)
          || character == '\u2028'
          || character == '\u2029'
          || (character >= '\u202A' && character <= '\u202E')
          || (character >= '\u2066' && character <= '\u2069')) {
        output.append(' ');
        continue;
      }
      switch (character) {
        case '&' -> output.append("&amp;");
        case '<' -> output.append("&lt;");
        case '>' -> output.append("&gt;");
        case '\\' -> output.append("\\\\");
        case '`' -> output.append("\\`");
        case '*' -> output.append("\\*");
        case '_' -> output.append("\\_");
        case '[' -> output.append("\\[");
        case ']' -> output.append("\\]");
        case '(' -> output.append("\\(");
        case ')' -> output.append("\\)");
        case '!' -> output.append("\\!");
        case '#' -> output.append("\\#");
        case '|' -> output.append("\\|");
        case '~' -> output.append("\\~");
        default -> output.append(character);
      }
    }
    if (output.length() > MAX_FIELD_CHARS) {
      return output.substring(0, MAX_FIELD_CHARS) + " [truncated]";
    }
    return output.toString();
  }
}
