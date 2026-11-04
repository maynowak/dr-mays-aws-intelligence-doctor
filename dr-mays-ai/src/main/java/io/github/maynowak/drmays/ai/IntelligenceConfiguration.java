package io.github.maynowak.drmays.ai;

import io.github.maynowak.drmays.ai.expertise.PrivacyFirstExpertiseEngine;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Selects the deterministic mock by default. The former raw Ollama path is fail-closed: external
 * providers require the privacy preview/approval flow instead.
 */
@Configuration
public class IntelligenceConfiguration {

  @Bean
  @ConditionalOnProperty(name = "dr.mays.ai.enabled", havingValue = "false", matchIfMissing = true)
  public IntelligenceAnalyzer mockIntelligenceAnalyzer() {
    return new MockIntelligenceAnalyzer();
  }

  /**
   * Keeps the legacy endpoint from sending raw infrastructure data when AI is enabled.
   *
   * @return approval-required legacy analyzer
   */
  @Bean
  @ConditionalOnProperty(name = "dr.mays.ai.enabled", havingValue = "true")
  public IntelligenceAnalyzer legacyPrivacyGate() {
    return new PrivacyGatedLegacyAnalyzer(new ModelMetadata("legacy", "approval-required", "1.0"));
  }

  /**
   * Provides the mock-first privacy orchestration engine without contacting a provider.
   *
   * @return privacy-first expertise engine
   */
  @Bean
  @ConditionalOnMissingBean
  public PrivacyFirstExpertiseEngine privacyFirstExpertiseEngine() {
    return new PrivacyFirstExpertiseEngine();
  }
}
