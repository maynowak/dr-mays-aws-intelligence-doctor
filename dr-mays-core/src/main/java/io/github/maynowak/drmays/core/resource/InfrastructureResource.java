package io.github.maynowak.drmays.core.resource;

import io.github.maynowak.drmays.core.validation.DomainChecks;

import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/**
 * An immutable infrastructure resource with schemaless tags and attributes.
 */
public record InfrastructureResource(
        ResourceIdentity identity,
        Map<String, String> tags,
        Map<String, String> attributes) {

    public InfrastructureResource {
        identity = Objects.requireNonNull(identity, "identity");
        tags = DomainChecks.immutableTextMap(tags, "tags");
        attributes = DomainChecks.immutableTextMap(attributes, "attributes");
    }

    /**
     * Returns a tag value when the tag exists.
     *
     * @param key tag key
     * @return the tag value, or empty when absent
     */
    public Optional<String> tag(String key) {
        DomainChecks.requireText(key, "key");
        return Optional.ofNullable(tags.get(key));
    }

    /**
     * Returns an attribute value when the attribute exists.
     *
     * @param key attribute key
     * @return the attribute value, or empty when absent
     */
    public Optional<String> attribute(String key) {
        DomainChecks.requireText(key, "key");
        return Optional.ofNullable(attributes.get(key));
    }
}
