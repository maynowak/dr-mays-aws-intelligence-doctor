package io.github.maynowak.drmays.ai.expertise;

import io.github.maynowak.drmays.ai.FindingExplanation;
import io.github.maynowak.drmays.ai.IntelligenceRequest;
import io.github.maynowak.drmays.ai.IntelligenceResult;
import io.github.maynowak.drmays.ai.IntelligenceResultValidator;
import io.github.maynowak.drmays.ai.ModelExecutionStatus;
import io.github.maynowak.drmays.ai.Recommendation;
import io.github.maynowak.drmays.ai.privacy.PrivacyValidator;
import io.github.maynowak.drmays.ai.provider.ProviderExecutionStatus;

/** Applies profile bounds, free-text safety and the existing evidence validator. */
public final class ExpertiseValidator {

  private final IntelligenceResultValidator resultValidator = new IntelligenceResultValidator();
  private final PrivacyValidator privacyValidator = new PrivacyValidator();

  /**
   * Validates a report against the source request and profile.
   *
   * @param source original verified request
   * @param report candidate report
   * @param profile bounded profile
   * @return report or an INVALID_RESPONSE failure
   */
  public ExpertiseReport validate(
      IntelligenceRequest source, ExpertiseReport report, ExpertiseProfile profile) {
    if (report.status() != ProviderExecutionStatus.SUCCESS) {
      return report;
    }
    if (!safe(report.summary(), profile)
        || report.explanations().size() > profile.maxExplanations()
        || report.recommendations().size() > profile.maxRecommendations()) {
      return invalid(report);
    }
    for (var explanation : report.explanations()) {
      if (!safe(explanation, profile)) {
        return invalid(report);
      }
    }
    for (var recommendation : report.recommendations()) {
      if (!safe(recommendation, profile)) {
        return invalid(report);
      }
    }
    if (report.assumptions().stream().anyMatch(value -> !safe(value, profile))
        || report.missingInformation().stream().anyMatch(value -> !safe(value, profile))
        || report.uncertainties().stream().anyMatch(value -> !safe(value, profile))) {
      return invalid(report);
    }
    var result =
        new IntelligenceResult(
            ModelExecutionStatus.SUCCESS,
            report.metadata(),
            report.summary(),
            report.explanations(),
            report.recommendations(),
            report.evidenceReferences(),
            report.verifiedRelationships(),
            report.uncertainties());
    if (resultValidator.validate(source, result).status() != ModelExecutionStatus.SUCCESS) {
      return invalid(report);
    }
    return report;
  }

  private boolean safe(FindingExplanation explanation, ExpertiseProfile profile) {
    return explanation.explanation().length() <= profile.maxTextChars()
        && !privacyValidator.containsUnsafeText(explanation.explanation());
  }

  private boolean safe(Recommendation recommendation, ExpertiseProfile profile) {
    return recommendation.title().length() <= profile.maxTextChars()
        && recommendation.action().length() <= profile.maxTextChars()
        && !privacyValidator.containsUnsafeText(recommendation.title())
        && !privacyValidator.containsUnsafeText(recommendation.action());
  }

  private boolean safe(String text, ExpertiseProfile profile) {
    return text.length() <= profile.maxTextChars() && !privacyValidator.containsUnsafeText(text);
  }

  private ExpertiseReport invalid(ExpertiseReport report) {
    return ExpertiseReport.failure(
        ProviderExecutionStatus.INVALID_RESPONSE,
        report.metadata(),
        report.facts(),
        report.privacyReceipt());
  }
}
