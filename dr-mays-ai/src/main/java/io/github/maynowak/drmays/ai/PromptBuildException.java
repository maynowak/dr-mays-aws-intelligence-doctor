package io.github.maynowak.drmays.ai;

/** Raised when a prompt cannot be loaded or exceeds the size limit. */
public class PromptBuildException extends RuntimeException {

  public PromptBuildException(String message) {
    super(message);
  }

  public PromptBuildException(String message, Throwable cause) {
    super(message, cause);
  }
}
