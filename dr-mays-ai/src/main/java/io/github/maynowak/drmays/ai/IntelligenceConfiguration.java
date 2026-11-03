package io.github.maynowak.drmays.ai;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import tools.jackson.databind.ObjectMapper;

/**
 * Selects the deterministic mock by default and the Ollama adapter only when explicitly enabled.
 */
@Configuration
public class IntelligenceConfiguration {

  @Bean
  @ConditionalOnProperty(name = "dr.mays.ai.enabled", havingValue = "false", matchIfMissing = true)
  public IntelligenceAnalyzer mockIntelligenceAnalyzer() {
    return new MockIntelligenceAnalyzer();
  }

  @Bean
  @ConditionalOnProperty(name = "dr.mays.ai.enabled", havingValue = "true")
  public IntelligenceAnalyzer ollamaIntelligenceAnalyzer(
      ChatClient.Builder chatClientBuilder, @Value("${dr.mays.ai.model:llama3.2}") String model) {
    return new OllamaIntelligenceAnalyzer(
        new SpringAiChatCompletion(chatClientBuilder.build()),
        new PromptBuilder(),
        new ObjectMapper(),
        new ModelMetadata("ollama", model, "1.0"));
  }
}
