package io.github.maynowak.drmays.ai;

import io.github.maynowak.drmays.core.finding.Severity;
import io.github.maynowak.drmays.core.validation.DomainChecks;
import java.util.Objects;

/**
 * Suggested action, explicitly separate from technical findings.
 *
 * @param title short recommendation title
 * @param action concrete action
 * @param severity priority
 */
public record Recommendation(String title, String action, Severity severity) {

  public Recommendation {
    title = DomainChecks.requireText(title, "title");
    action = DomainChecks.requireText(action, "action");
    severity = Objects.requireNonNull(severity, "severity");
  }
}
