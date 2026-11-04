package io.github.maynowak.drmays.ai.privacy;

/** Sensitivity class of a field proposed for provider transmission. */
public enum PrivacyClassification {
  PUBLIC,
  INTERNAL,
  IDENTIFIER,
  QUASI_IDENTIFIER,
  SENSITIVE,
  SECRET
}
