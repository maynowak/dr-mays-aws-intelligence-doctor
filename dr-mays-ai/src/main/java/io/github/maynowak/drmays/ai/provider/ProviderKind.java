package io.github.maynowak.drmays.ai.provider;

/** Execution category of a model provider. */
public enum ProviderKind {
  DETERMINISTIC_MOCK,
  LOCAL_MODEL,
  EXTERNAL_MODEL,
  CLI_AGENT
}
