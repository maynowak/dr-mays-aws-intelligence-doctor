package io.github.maynowak.drmays.ai;

/** Narrow model transport used by the analyzer; keeps provider details out of the analyzer. */
public interface ChatCompletion {

  /**
   * Completes one prompt.
   *
   * @param prompt bounded prompt text
   * @return raw model response
   */
  String complete(String prompt);
}
