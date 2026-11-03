package io.github.maynowak.drmays.api.v1;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.Map;

/**
 * One synthetic infrastructure resource in a diagnosis request.
 *
 * @param identity required resource identity
 * @param tags optional tags, limited to 50 entries
 * @param attributes optional technical attributes, limited to 50 entries
 */
public record ResourceRequest(
    @NotNull @Valid ResourceIdentityRequest identity,
    @Size(max = 50) Map<@NotBlank @Size(max = 128) String, @NotNull @Size(max = 1024) String> tags,
    @Size(max = 50)
        Map<@NotBlank @Size(max = 128) String, @NotNull @Size(max = 1024) String> attributes) {

  public ResourceRequest {
    tags = tags == null ? Map.of() : Map.copyOf(tags);
    attributes = attributes == null ? Map.of() : Map.copyOf(attributes);
  }
}
