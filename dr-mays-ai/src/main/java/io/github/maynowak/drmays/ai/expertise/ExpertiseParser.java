package io.github.maynowak.drmays.ai.expertise;

import java.util.Objects;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;

/** Parses a bounded JSON model response, including an optional Markdown fence. */
public final class ExpertiseParser {

  private final ObjectMapper objectMapper;

  public ExpertiseParser() {
    this(new ObjectMapper());
  }

  /**
   * Creates a parser with an explicit JSON mapper.
   *
   * @param objectMapper JSON mapper
   */
  public ExpertiseParser(ObjectMapper objectMapper) {
    this.objectMapper = Objects.requireNonNull(objectMapper, "objectMapper");
  }

  /**
   * Parses a model response.
   *
   * @param content raw model output
   * @return parsed response
   * @throws IllegalArgumentException when the output is blank or malformed
   */
  public ExpertiseModelResponse parse(String content) {
    if (content == null || content.isBlank()) {
      throw new IllegalArgumentException("model output is empty");
    }
    try {
      return objectMapper.readValue(stripFences(content), ExpertiseModelResponse.class);
    } catch (JacksonException exception) {
      throw new IllegalArgumentException("model output is not valid JSON");
    }
  }

  private static String stripFences(String content) {
    var trimmed = content.strip();
    if (trimmed.startsWith("```")) {
      var firstNewline = trimmed.indexOf('\n');
      var lastFence = trimmed.lastIndexOf("```");
      if (firstNewline >= 0 && lastFence > firstNewline) {
        return trimmed.substring(firstNewline + 1, lastFence).strip();
      }
    }
    return trimmed;
  }
}
