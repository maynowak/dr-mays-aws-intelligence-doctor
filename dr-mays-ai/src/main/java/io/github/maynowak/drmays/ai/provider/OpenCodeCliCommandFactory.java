package io.github.maynowak.drmays.ai.provider;

import java.nio.file.Path;
import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Objects;

/** Builds a bounded, non-shell OpenCode invocation for the disabled adapter. */
public final class OpenCodeCliCommandFactory {

  private static final Duration DEFAULT_TIMEOUT = Duration.ofSeconds(30);
  private static final int DEFAULT_MAX_OUTPUT = 64 * 1024;

  private OpenCodeCliCommandFactory() {}

  /**
   * Builds a command with fixed arguments and a sanitized environment.
   *
   * @param binary absolute CLI binary
   * @param modelId explicit model identifier
   * @param payloadJson sanitized JSON payload
   * @param isolatedHome private temporary home
   * @param workingDirectory controlled empty working directory
   * @return command description, not an executed process
   */
  public static OpenCodeCliCommand create(
      Path binary, String modelId, String payloadJson, Path isolatedHome, Path workingDirectory) {
    Objects.requireNonNull(binary, "binary");
    Objects.requireNonNull(modelId, "modelId");
    Objects.requireNonNull(payloadJson, "payloadJson");
    Objects.requireNonNull(isolatedHome, "isolatedHome");
    Objects.requireNonNull(workingDirectory, "workingDirectory");
    if (!modelId.matches("[A-Za-z0-9._:/-]{1,128}")) {
      throw new IllegalArgumentException("modelId is invalid");
    }
    var home = isolatedHome.toAbsolutePath().normalize();
    var environment = new LinkedHashMap<String, String>();
    environment.put("HOME", home.toString());
    environment.put("XDG_CONFIG_HOME", home.resolve("config").toString());
    environment.put("XDG_DATA_HOME", home.resolve("data").toString());
    environment.put("XDG_CACHE_HOME", home.resolve("cache").toString());
    environment.put("OPENCODE_CONFIG_DIR", home.resolve("config").toString());
    environment.put("OPENCODE_DISABLE_PROJECT_CONFIG", "1");
    environment.put("OPENCODE_DISABLE_AUTOUPDATE", "1");
    environment.put("OPENCODE_DISABLE_MODELS_FETCH", "1");
    environment.put("PATH", "/usr/bin:/bin");
    var arguments =
        List.of(
            binary.toAbsolutePath().normalize().toString(),
            "run",
            "--format",
            "json",
            "--model",
            modelId);
    return new OpenCodeCliCommand(
        arguments,
        environment,
        workingDirectory.toAbsolutePath().normalize(),
        DEFAULT_TIMEOUT,
        DEFAULT_MAX_OUTPUT,
        payloadJson);
  }
}
