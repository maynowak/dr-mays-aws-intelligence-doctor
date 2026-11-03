package io.github.maynowak.drmays.core.dna;

import io.github.maynowak.drmays.core.evidence.Evidence;
import io.github.maynowak.drmays.core.resource.InfrastructureResource;
import io.github.maynowak.drmays.core.resource.ResourceIdentity;
import java.util.Map;

/** Shared deterministic graph test data. */
final class GraphTestFixtures {

  static final String ACCOUNT = "000000000000";
  static final String REGION = "eu-west-1";

  private GraphTestFixtures() {}

  static ResourceIdentity identity(String type, String id) {
    return identity(type, id, ACCOUNT, REGION);
  }

  static ResourceIdentity identity(String type, String id, String account, String region) {
    return new ResourceIdentity("aws", account, region, type, id);
  }

  static InfrastructureResource network(String id, String vpcId, String referencedIds) {
    var attributes = new java.util.LinkedHashMap<String, String>();
    if (vpcId != null) {
      attributes.put("vpc.id", vpcId);
    }
    if (referencedIds != null) {
      attributes.put("ingress.referenced.securityGroupIds", referencedIds);
    }
    return new InfrastructureResource(
        identity("network", id), Map.of("owner", "platform"), attributes);
  }

  static InfrastructureResource bucket(String id) {
    return new InfrastructureResource(
        identity("s3-bucket", id),
        Map.of("owner", "platform"),
        Map.of("s3.versioning.status", "Enabled", "s3.publicAccessBlock.enabled", "true"));
  }

  static ResourceEdge edge(ResourceIdentity from, ResourceIdentity to) {
    return new ResourceEdge(
        from,
        to,
        RelationshipType.SECURITY_GROUP_IN_VPC,
        new RelationshipEvidence(new Evidence(from, "test", "observed", "expected", Map.of())));
  }
}
