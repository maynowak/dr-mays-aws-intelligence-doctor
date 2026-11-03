package io.github.maynowak.drmays.api.config;

import io.github.maynowak.drmays.core.diagnosis.DiagnosisEngine;
import io.github.maynowak.drmays.core.rule.builtin.MissingOwnerTagRule;
import io.github.maynowak.drmays.core.rule.builtin.PublicIngressRule;
import java.util.List;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** Wires the deterministic core engine and its built-in synthetic rules into the API. */
@Configuration
public class DiagnosisEngineConfiguration {

  /**
   * Creates the shared diagnosis engine.
   *
   * @return an engine with the built-in demonstration rules
   */
  @Bean
  public DiagnosisEngine diagnosisEngine() {
    return new DiagnosisEngine(List.of(new MissingOwnerTagRule(), new PublicIngressRule()));
  }
}
