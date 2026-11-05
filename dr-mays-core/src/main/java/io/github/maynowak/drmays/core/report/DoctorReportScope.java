package io.github.maynowak.drmays.core.report;

import java.util.List;
import java.util.Objects;

/**
 * Deterministic scope of the reviewed snapshot.
 *
 * @param providers providers present in the snapshot
 * @param accounts accounts present in the snapshot
 * @param regions regions present in the snapshot
 * @param resourceCount number of inventoried resources
 */
public record DoctorReportScope(
    List<String> providers, List<String> accounts, List<String> regions, int resourceCount) {

  public DoctorReportScope {
    providers = List.copyOf(Objects.requireNonNull(providers, "providers"));
    accounts = List.copyOf(Objects.requireNonNull(accounts, "accounts"));
    regions = List.copyOf(Objects.requireNonNull(regions, "regions"));
    if (resourceCount < 0) {
      throw new IllegalArgumentException("resourceCount must not be negative");
    }
  }
}
