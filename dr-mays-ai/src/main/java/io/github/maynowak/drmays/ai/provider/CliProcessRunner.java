package io.github.maynowak.drmays.ai.provider;

/** Executes a bounded CLI command. No production implementation is enabled in this phase. */
@FunctionalInterface
public interface CliProcessRunner {

  /**
   * Runs one command.
   *
   * @param command fixed command description
   * @return bounded process result
   */
  CliProcessResult run(OpenCodeCliCommand command);
}
