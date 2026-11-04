package io.github.maynowak.drmays.ai.provider;

import java.nio.file.Path;
import java.util.Objects;

/**
 * Disabled OpenCode CLI adapter. It can be exercised with a simulated process runner but never
 * constructs or starts a real process itself.
 */
public final class OpenCodeCliAdapter implements ModelProvider {

  private final boolean liveExecutionEnabled;
  private final Path binary;
  private final Path isolatedHome;
  private final Path workingDirectory;
  private final CliProcessRunner runner;

  /**
   * Creates a disabled adapter with explicit isolation paths.
   *
   * @param binary absolute CLI binary
   * @param isolatedHome private temporary home
   * @param workingDirectory controlled empty directory
   * @param runner simulated or future process runner
   */
  public OpenCodeCliAdapter(
      Path binary, Path isolatedHome, Path workingDirectory, CliProcessRunner runner) {
    this(false, binary, isolatedHome, workingDirectory, runner);
  }

  /**
   * Creates an adapter with explicit live-execution policy.
   *
   * @param liveExecutionEnabled must remain false in this phase
   * @param binary absolute CLI binary
   * @param isolatedHome private temporary home
   * @param workingDirectory controlled empty directory
   * @param runner process runner
   */
  public OpenCodeCliAdapter(
      boolean liveExecutionEnabled,
      Path binary,
      Path isolatedHome,
      Path workingDirectory,
      CliProcessRunner runner) {
    this.liveExecutionEnabled = liveExecutionEnabled;
    this.binary = binary;
    this.isolatedHome = isolatedHome;
    this.workingDirectory = workingDirectory;
    this.runner = Objects.requireNonNull(runner, "runner");
  }

  @Override
  public ModelProviderDescriptor descriptor() {
    return ModelProviderDescriptor.openCode();
  }

  @Override
  public ModelInvocationResult invoke(ModelInvocationRequest request) {
    Objects.requireNonNull(request, "request");
    if (!liveExecutionEnabled) {
      return ModelInvocationResult.failure(
          ProviderExecutionStatus.DISABLED,
          request.providerId(),
          request.modelId(),
          "OPENCODE_LIVE_EXECUTION_BLOCKED");
    }
    var command =
        OpenCodeCliCommandFactory.create(
            binary, request.modelId(), request.payloadJson(), isolatedHome, workingDirectory);
    var result = runner.run(command);
    if (result.exitCode() != 0) {
      return ModelInvocationResult.failure(
          ProviderExecutionStatus.FAILED,
          request.providerId(),
          request.modelId(),
          "CLI_EXIT_" + result.exitCode());
    }
    if (result.stdout().length() > command.maxOutputBytes()) {
      return ModelInvocationResult.failure(
          ProviderExecutionStatus.INVALID_RESPONSE,
          request.providerId(),
          request.modelId(),
          "CLI_OUTPUT_TOO_LARGE");
    }
    if (result.stdout().isBlank()) {
      return ModelInvocationResult.failure(
          ProviderExecutionStatus.INVALID_RESPONSE,
          request.providerId(),
          request.modelId(),
          "CLI_EMPTY_OUTPUT");
    }
    return ModelInvocationResult.success(
        request.providerId(), request.modelId(), result.stdout(), command.stdin().length());
  }
}
