package io.github.maynowak.drmays.core.dna;

import io.github.maynowak.drmays.core.validation.DomainChecks;
import java.util.Objects;

/**
 * A non-fatal graph construction issue.
 *
 * @param code stable issue category
 * @param message safe human-readable summary
 */
public record GraphValidationIssue(GraphIssueCode code, String message) {

  public GraphValidationIssue {
    code = Objects.requireNonNull(code, "code");
    message = DomainChecks.requireText(message, "message");
  }
}
