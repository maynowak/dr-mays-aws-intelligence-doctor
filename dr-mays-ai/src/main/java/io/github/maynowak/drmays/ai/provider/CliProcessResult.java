package io.github.maynowak.drmays.ai.provider;

import java.util.Objects;

/**
 * Result of a simulated or future bounded CLI process.
 *
 * @param exitCode process exit code
 * @param stdout bounded standard output
 * @param stderr bounded standard error
 */
public record CliProcessResult(int exitCode, String stdout, String stderr) {

  public CliProcessResult {
    stdout = Objects.requireNonNull(stdout, "stdout");
    stderr = Objects.requireNonNull(stderr, "stderr");
  }
}
