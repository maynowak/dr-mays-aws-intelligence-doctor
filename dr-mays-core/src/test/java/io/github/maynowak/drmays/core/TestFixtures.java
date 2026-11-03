package io.github.maynowak.drmays.core;

import io.github.maynowak.drmays.core.evidence.Evidence;
import io.github.maynowak.drmays.core.finding.DiagnosticFinding;
import io.github.maynowak.drmays.core.finding.Severity;
import io.github.maynowak.drmays.core.resource.InfrastructureResource;
import io.github.maynowak.drmays.core.resource.ResourceIdentity;
import io.github.maynowak.drmays.core.rule.DiagnosticContext;
import io.github.maynowak.drmays.core.rule.RuleId;
import java.util.Map;

/** Shared synthetic data for domain tests. */
public final class TestFixtures {

  public static final ResourceIdentity NETWORK_ID = identity("network", "net-1");
  public static final ResourceIdentity STORAGE_ID = identity("storage", "store-1");

  private TestFixtures() {}

  public static ResourceIdentity identity(String resourceType, String resourceId) {
    return new ResourceIdentity("aws", "123456789012", "eu-central-1", resourceType, resourceId);
  }

  public static InfrastructureResource resource(
      ResourceIdentity identity, Map<String, String> tags, Map<String, String> attributes) {
    return new InfrastructureResource(identity, tags, attributes);
  }

  public static DiagnosticContext context(InfrastructureResource... resources) {
    return new DiagnosticContext(java.util.Arrays.asList(resources));
  }

  public static DiagnosticFinding finding(ResourceIdentity resource, RuleId ruleId) {
    var evidence = new Evidence(resource, "test-source", "observed", "expected", Map.of());
    return new DiagnosticFinding(
        ruleId,
        "1.0",
        Severity.HIGH,
        resource,
        "Test finding",
        "Synthetic test finding",
        evidence,
        "Test recommendation");
  }
}
