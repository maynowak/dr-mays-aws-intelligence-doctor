package io.github.maynowak.drmays.ai;

import java.util.Objects;
import org.springframework.ai.chat.client.ChatClient;

/** Spring AI ChatClient transport; no tools are registered. */
final class SpringAiChatCompletion implements ChatCompletion {

  private final ChatClient chatClient;

  public SpringAiChatCompletion(ChatClient chatClient) {
    this.chatClient = Objects.requireNonNull(chatClient, "chatClient");
  }

  @Override
  public String complete(String prompt) {
    return chatClient.prompt().user(prompt).call().content();
  }
}
