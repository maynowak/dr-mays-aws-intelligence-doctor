package io.github.maynowak.drmays.core.finding;

import java.util.Objects;

/** Severity of a deterministic finding, ordered from informational to critical. */
public enum Severity {
  INFO(10),
  LOW(20),
  MEDIUM(30),
  HIGH(40),
  CRITICAL(50);

  private final int rank;

  Severity(int rank) {
    this.rank = rank;
  }

  /**
   * Returns the numeric rank used for severity ordering.
   *
   * @return the rank, increasing from {@code INFO} to {@code CRITICAL}
   */
  public int rank() {
    return rank;
  }

  /**
   * Returns whether this severity is at least as severe as the given threshold.
   *
   * @param threshold the severity to compare against
   * @return {@code true} when this severity's rank is greater than or equal to the threshold
   */
  public boolean isAtLeast(Severity threshold) {
    Objects.requireNonNull(threshold, "threshold");
    return rank >= threshold.rank;
  }
}
