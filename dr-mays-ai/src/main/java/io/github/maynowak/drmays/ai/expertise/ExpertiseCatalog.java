package io.github.maynowak.drmays.ai.expertise;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/** Server-controlled expertise profiles; clients cannot define arbitrary profiles. */
public final class ExpertiseCatalog {

  private final Map<String, ExpertiseProfile> profiles = new LinkedHashMap<>();

  /** Creates a catalog with the standard profiles. */
  public ExpertiseCatalog() {
    List.of(ExpertiseProfile.concise(), ExpertiseProfile.standard(), ExpertiseProfile.deep())
        .forEach(this::register);
  }

  /**
   * Registers a profile.
   *
   * @param profile profile to register
   */
  public void register(ExpertiseProfile profile) {
    var value = Objects.requireNonNull(profile, "profile");
    if (profiles.putIfAbsent(value.id(), value) != null) {
      throw new IllegalArgumentException("duplicate expertise profile: " + value.id());
    }
  }

  /**
   * Finds a profile.
   *
   * @param id profile identifier
   * @return profile when known
   */
  public Optional<ExpertiseProfile> find(String id) {
    return Optional.ofNullable(profiles.get(Objects.requireNonNull(id, "id")));
  }

  /**
   * Requires a known profile.
   *
   * @param id profile identifier
   * @return profile
   */
  public ExpertiseProfile require(String id) {
    return find(id).orElseThrow(() -> new IllegalArgumentException("unknown expertise profile"));
  }

  /** Returns the default profile. */
  public ExpertiseProfile standard() {
    return require("standard");
  }
}
