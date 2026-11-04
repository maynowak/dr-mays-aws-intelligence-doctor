package io.github.maynowak.drmays.ai.provider;

/** A provider that can analyze an already sanitized payload. */
public interface ModelProvider {

  /**
   * Returns immutable provider metadata.
   *
   * @return provider descriptor
   */
  ModelProviderDescriptor descriptor();

  /**
   * Invokes the provider exactly once with a sanitized payload.
   *
   * @param request bounded invocation request
   * @return bounded result
   */
  ModelInvocationResult invoke(ModelInvocationRequest request);
}
