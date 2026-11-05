package io.github.maynowak.drmays.api.v1;

/** Raised when a generated report exceeds the bounded output limit. */
public class ReportLimitExceededException extends RuntimeException {

  /** Creates the exception with a safe message. */
  public ReportLimitExceededException() {
    super("report exceeds the configured output limit");
  }
}
