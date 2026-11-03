package io.github.maynowak.drmays.ai;

import io.github.maynowak.drmays.core.validation.DomainChecks;

/**
 * Provider/model information for an intelligence run.
 *
 * @param provider provider identifier, e.g. mock or ollama
 * @param model model identifier
 * @param version adapter/contract version
 */
public record ModelMetadata(String provider, String model, String version) {

  public ModelMetadata {
    provider = DomainChecks.requireText(provider, "provider");
    model = DomainChecks.requireText(model, "model");
    version = DomainChecks.requireText(version, "version");
  }
}
