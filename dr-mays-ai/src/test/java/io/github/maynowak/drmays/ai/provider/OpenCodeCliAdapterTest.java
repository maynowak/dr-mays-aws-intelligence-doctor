package io.github.maynowak.drmays.ai.provider;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.file.Path;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;

class OpenCodeCliAdapterTest {

  private static final Path BINARY = Path.of("/usr/bin/opencode");
  private static final Path HOME = Path.of("/tmp/opencode/cli-home");
  private static final Path WORK = Path.of("/tmp/opencode/cli-work");

  @Test
  void disabledAdapterNeverInvokesRunner() {
    var calls = new AtomicInteger();
    var adapter =
        new OpenCodeCliAdapter(
            BINARY,
            HOME,
            WORK,
            command -> {
              calls.incrementAndGet();
              return new CliProcessResult(0, "{}", "");
            });

    var result = adapter.invoke(request());

    assertThat(result.status()).isEqualTo(ProviderExecutionStatus.DISABLED);
    assertThat(calls).hasValue(0);
  }

  @Test
  void commandBuilderUsesFixedArgvAndSanitizedEnvironment() {
    var command = OpenCodeCliCommandFactory.create(BINARY, "openai/gpt", "{\"a\":1}", HOME, WORK);

    assertThat(command.arguments())
        .containsExactly("/usr/bin/opencode", "run", "--format", "json", "--model", "openai/gpt");
    assertThat(command.environment()).doesNotContainKeys("AWS_ACCESS_KEY_ID", "OPENAI_API_KEY");
    assertThat(command.environment()).containsKeys("HOME", "OPENCODE_DISABLE_PROJECT_CONFIG");
    assertThat(command.stdin()).isEqualTo("{\"a\":1}");
    assertThat(command.toString()).isEqualTo("OpenCodeCliCommand[redacted]");
  }

  @Test
  void enabledAdapterPassesBoundedCommandToRunner() {
    var captured = new AtomicReference<OpenCodeCliCommand>();
    var adapter =
        new OpenCodeCliAdapter(
            true,
            BINARY,
            HOME,
            WORK,
            command -> {
              captured.set(command);
              return new CliProcessResult(0, "{\"status\":\"ok\"}", "");
            },
            OpenCodeSandboxPolicy.allControlsSatisfiedForTest());

    var result = adapter.invoke(request());

    assertThat(result.status()).isEqualTo(ProviderExecutionStatus.SUCCESS);
    assertThat(captured.get().arguments()).contains("run");
    assertThat(captured.get().timeout()).isPositive();
    assertThat(captured.get().maxOutputBytes()).isEqualTo(64 * 1024);
  }

  @Test
  void nonzeroExitIsFailure() {
    var adapter =
        new OpenCodeCliAdapter(
            true,
            BINARY,
            HOME,
            WORK,
            command -> new CliProcessResult(7, "", "denied"),
            OpenCodeSandboxPolicy.allControlsSatisfiedForTest());

    assertThat(adapter.invoke(request()).status()).isEqualTo(ProviderExecutionStatus.FAILED);
  }

  @Test
  void oversizedOutputIsRejected() {
    var adapter =
        new OpenCodeCliAdapter(
            true,
            BINARY,
            HOME,
            WORK,
            command -> new CliProcessResult(0, "x".repeat(70_000), ""),
            OpenCodeSandboxPolicy.allControlsSatisfiedForTest());

    assertThat(adapter.invoke(request()).status())
        .isEqualTo(ProviderExecutionStatus.INVALID_RESPONSE);
  }

  @Test
  void blankOutputIsRejected() {
    var adapter =
        new OpenCodeCliAdapter(
            true,
            BINARY,
            HOME,
            WORK,
            command -> new CliProcessResult(0, " ", ""),
            OpenCodeSandboxPolicy.allControlsSatisfiedForTest());

    assertThat(adapter.invoke(request()).status())
        .isEqualTo(ProviderExecutionStatus.INVALID_RESPONSE);
  }

  private static ModelInvocationRequest request() {
    return ProviderTestFixtures.request(
        "opencode",
        "openai/gpt",
        ProviderCredentialReference.environment("opencode", "DR_MAYS_OPENCODE_KEY"));
  }
}
