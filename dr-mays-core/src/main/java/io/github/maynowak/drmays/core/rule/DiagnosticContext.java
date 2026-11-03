package io.github.maynowak.drmays.core.rule;

import io.github.maynowak.drmays.core.resource.InfrastructureResource;
import io.github.maynowak.drmays.core.resource.ResourceIdentity;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * Immutable view of all resources available to one diagnosis run.
 */
public record DiagnosticContext(List<InfrastructureResource> resources) {

    public DiagnosticContext {
        Objects.requireNonNull(resources, "resources");
        var sorted = new ArrayList<InfrastructureResource>(resources.size());
        for (var resource : resources) {
            sorted.add(Objects.requireNonNull(resource, "resource"));
        }
        sorted.sort(Comparator.comparing(resource -> resource.identity().canonicalId()));

        for (int i = 1; i < sorted.size(); i++) {
            if (sorted.get(i - 1).identity().equals(sorted.get(i).identity())) {
                throw new IllegalArgumentException(
                        "duplicate resource identity: " + sorted.get(i).identity().canonicalId());
            }
        }
        resources = List.copyOf(sorted);
    }

    /**
     * Finds a resource by exact identity.
     *
     * @param identity the identity to find
     * @return the resource, or empty when absent
     */
    public Optional<InfrastructureResource> find(ResourceIdentity identity) {
        Objects.requireNonNull(identity, "identity");
        return resources.stream()
                .filter(resource -> resource.identity().equals(identity))
                .findFirst();
    }
}
