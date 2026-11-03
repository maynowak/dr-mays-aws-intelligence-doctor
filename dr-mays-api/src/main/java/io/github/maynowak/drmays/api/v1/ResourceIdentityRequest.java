package io.github.maynowak.drmays.api.v1;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * Wire representation of a resource identity.
 *
 * @param provider infrastructure provider, for example {@code aws}
 * @param accountId provider account identifier
 * @param region provider region
 * @param resourceType provider resource type
 * @param resourceId provider resource identifier
 */
public record ResourceIdentityRequest(
    @NotBlank @Size(max = 64) @Pattern(regexp = "[a-z][a-z0-9-]*") String provider,
    @NotBlank @Size(max = 128) String accountId,
    @NotBlank @Size(max = 64) String region,
    @NotBlank @Size(max = 128) @Pattern(regexp = "[a-z][a-z0-9-]*") String resourceType,
    @NotBlank @Size(max = 256) String resourceId) {}
