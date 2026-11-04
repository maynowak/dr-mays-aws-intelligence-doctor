package io.github.maynowak.drmays.ai.provider;

/**
 * Bounded capabilities of a provider.
 *
 * @param structuredJson whether JSON output can be requested
 * @param localOnly whether the provider is expected to stay on the local machine
 * @param toolsAllowed whether the provider may use tools (must be false for this phase)
 * @param maxPayloadChars maximum accepted payload size
 * @param maxOutputChars maximum accepted output size
 */
public record ProviderCapabilities(
    boolean structuredJson,
    boolean localOnly,
    boolean toolsAllowed,
    int maxPayloadChars,
    int maxOutputChars) {

  public ProviderCapabilities {
    if (maxPayloadChars <= 0 || maxOutputChars <= 0) {
      throw new IllegalArgumentException("provider limits must be positive");
    }
  }
}
