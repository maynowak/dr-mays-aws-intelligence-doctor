package io.github.maynowak.drmays.api.v1;

/**
 * Wire representation of a resource identity.
 *
 * @param provider infrastructure provider
 * @param accountId provider account identifier
 * @param region provider region
 * @param resourceType provider resource type
 * @param resourceId provider resource identifier
 */
public record ResourceResponse(
    String provider, String accountId, String region, String resourceType, String resourceId) {}
