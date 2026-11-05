package io.github.maynowak.drmays.api.v1;

import java.util.Collections;
import java.util.Map;
import java.util.TreeMap;

/**
 * Wire graph node for a Doctor Report with stable map ordering.
 *
 * @param resource node identity
 * @param status INVENTORIED or REFERENCED
 * @param tags sorted node tags
 * @param attributes sorted node attributes
 */
public record DoctorReportNodeResponse(
    ResourceResponse resource,
    String status,
    Map<String, String> tags,
    Map<String, String> attributes) {

  public DoctorReportNodeResponse {
    tags = sorted(tags);
    attributes = sorted(attributes);
  }

  private static Map<String, String> sorted(Map<String, String> values) {
    return Collections.unmodifiableMap(new TreeMap<>(values == null ? Map.of() : values));
  }
}
