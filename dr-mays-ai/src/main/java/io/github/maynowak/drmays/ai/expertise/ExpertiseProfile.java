package io.github.maynowak.drmays.ai.expertise;

import io.github.maynowak.drmays.core.validation.DomainChecks;

/**
 * Bounded expertise profile.
 *
 * @param id stable profile identifier
 * @param maxExplanations maximum explanations
 * @param maxRecommendations maximum recommendations
 * @param maxTextChars maximum length of any free-text field
 */
public record ExpertiseProfile(
    String id, int maxExplanations, int maxRecommendations, int maxTextChars) {

  public ExpertiseProfile {
    id = DomainChecks.requireText(id, "id");
    if (!id.matches("[a-z][a-z0-9-]{1,31}")) {
      throw new IllegalArgumentException("id is invalid");
    }
    if (maxExplanations <= 0 || maxRecommendations <= 0 || maxTextChars <= 0) {
      throw new IllegalArgumentException("expertise limits must be positive");
    }
  }

  /** Returns the concise profile. */
  public static ExpertiseProfile concise() {
    return new ExpertiseProfile("concise", 5, 5, 1_000);
  }

  /** Returns the default standard profile. */
  public static ExpertiseProfile standard() {
    return new ExpertiseProfile("standard", 20, 20, 4_000);
  }

  /** Returns the deep profile. */
  public static ExpertiseProfile deep() {
    return new ExpertiseProfile("deep", 100, 50, 8_000);
  }
}
