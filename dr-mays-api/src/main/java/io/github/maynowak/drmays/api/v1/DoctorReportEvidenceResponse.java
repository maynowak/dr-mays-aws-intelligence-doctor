package io.github.maynowak.drmays.api.v1;

import java.util.Collections;
import java.util.Map;
import java.util.TreeMap;

/**
 * Wire evidence for a Doctor Report with stable map ordering.
 *
 * @param source technical evidence source
 * @param observed observed value
 * @param expected expected value
 * @param details additional structured evidence
 */
public record DoctorReportEvidenceResponse(
    String source, String observed, String expected, Map<String, String> details) {

  public DoctorReportEvidenceResponse {
    details = Collections.unmodifiableMap(new TreeMap<>(details == null ? Map.of() : details));
  }
}
