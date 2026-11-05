package io.github.maynowak.drmays.ai.provider;

/**
 * Required controls for any future OpenCode live process. All controls must be true; CLI tool
 * permissions alone are explicitly insufficient.
 *
 * @param osIsolation OS/container isolation
 * @param noProjectWrite no writable project/home filesystem
 * @param noAwsCredentials AWS credentials excluded
 * @param noExistingCredentials existing OpenCode/other credentials excluded
 * @param denyAllTools all tools denied
 * @param noMcpPlugins MCP/plugins disabled
 * @param egressControlled provider-only network egress
 * @param boundedRuntime hard process-tree timeout
 * @param boundedMemory memory limit
 * @param boundedOutput streaming output cap
 * @param ephemeralSession ephemeral isolated session storage
 * @param cleanup session/temp artifacts cleaned
 */
public record OpenCodeSandboxPolicy(
    boolean osIsolation,
    boolean noProjectWrite,
    boolean noAwsCredentials,
    boolean noExistingCredentials,
    boolean denyAllTools,
    boolean noMcpPlugins,
    boolean egressControlled,
    boolean boundedRuntime,
    boolean boundedMemory,
    boolean boundedOutput,
    boolean ephemeralSession,
    boolean cleanup) {

  /** Returns the blocked default. */
  public static OpenCodeSandboxPolicy blocked() {
    return new OpenCodeSandboxPolicy(
        false, false, false, false, false, false, false, false, false, false, false, false);
  }

  /**
   * Returns whether every required control is present.
   *
   * @return true only when all controls are true
   */
  public boolean ready() {
    return osIsolation
        && noProjectWrite
        && noAwsCredentials
        && noExistingCredentials
        && denyAllTools
        && noMcpPlugins
        && egressControlled
        && boundedRuntime
        && boundedMemory
        && boundedOutput
        && ephemeralSession
        && cleanup;
  }

  /** Test-only policy with every control satisfied; no production wiring uses it. */
  static OpenCodeSandboxPolicy allControlsSatisfiedForTest() {
    return new OpenCodeSandboxPolicy(
        true, true, true, true, true, true, true, true, true, true, true, true);
  }
}
