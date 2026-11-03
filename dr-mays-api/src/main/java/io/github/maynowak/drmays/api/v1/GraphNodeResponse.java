package io.github.maynowak.drmays.api.v1;

import java.util.Map;

/**
 * Wire representation of a graph node.
 *
 * @param resource resource identity
 * @param status INVENTORIED or REFERENCED
 * @param tags node tags
 * @param attributes node attributes
 */
public record GraphNodeResponse(
    ResourceResponse resource,
    String status,
    Map<String, String> tags,
    Map<String, String> attributes) {

  public GraphNodeResponse {
    tags = tags == null ? Map.of() : Map.copyOf(tags);
    attributes = attributes == null ? Map.of() : Map.copyOf(attributes);
  }
}
