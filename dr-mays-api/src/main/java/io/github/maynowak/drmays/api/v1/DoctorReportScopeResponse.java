package io.github.maynowak.drmays.api.v1;

import java.util.List;

/**
 * Wire scope for a Doctor Report.
 *
 * @param providers providers in the snapshot
 * @param accounts accounts in the snapshot
 * @param regions regions in the snapshot
 * @param resourceCount inventoried resource count
 */
public record DoctorReportScopeResponse(
    List<String> providers, List<String> accounts, List<String> regions, int resourceCount) {

  public DoctorReportScopeResponse {
    providers = List.copyOf(providers);
    accounts = List.copyOf(accounts);
    regions = List.copyOf(regions);
  }
}
