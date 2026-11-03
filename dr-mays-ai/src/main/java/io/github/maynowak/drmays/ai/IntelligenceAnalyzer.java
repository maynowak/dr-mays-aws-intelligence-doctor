package io.github.maynowak.drmays.ai;

/** Technology-neutral contract for explaining verified diagnosis results. */
public interface IntelligenceAnalyzer {

  /**
   * Produces an explanation for the supplied verified request.
   *
   * @param request validated findings/evidence/relationships
   * @return structured result, including explicit failure status
   */
  IntelligenceResult analyze(IntelligenceRequest request);
}
