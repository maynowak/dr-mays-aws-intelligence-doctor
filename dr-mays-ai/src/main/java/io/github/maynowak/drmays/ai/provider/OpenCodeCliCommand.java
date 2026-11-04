package io.github.maynowak.drmays.ai.provider;

import java.nio.file.Path;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Fixed, non-shell OpenCode process description.
 *
 * @param arguments exact argv, never a shell string
 * @param environment sanitized environment
 * @param workingDirectory controlled working directory
 * @param timeout hard timeout
 * @param maxOutputBytes output cap
 * @param stdin bounded prompt payload
 */
public record OpenCodeCliCommand(
    List<String> arguments,
    Map<String, String> environment,
    Path workingDirectory,
    Duration timeout,
    int maxOutputBytes,
    String stdin) {

  public OpenCodeCliCommand {
    arguments = List.copyOf(Objects.requireNonNull(arguments, "arguments"));
    if (arguments.isEmpty()) {
      throw new IllegalArgumentException("arguments must not be empty");
    }
    environment = Map.copyOf(Objects.requireNonNull(environment, "environment"));
    workingDirectory = Objects.requireNonNull(workingDirectory, "workingDirectory");
    timeout = Objects.requireNonNull(timeout, "timeout");
    if (timeout.isZero() || timeout.isNegative()) {
      throw new IllegalArgumentException("timeout must be positive");
    }
    if (maxOutputBytes <= 0) {
      throw new IllegalArgumentException("maxOutputBytes must be positive");
    }
    stdin = Objects.requireNonNull(stdin, "stdin");
  }

  @Override
  public String toString() {
    return "OpenCodeCliCommand[redacted]";
  }
}
