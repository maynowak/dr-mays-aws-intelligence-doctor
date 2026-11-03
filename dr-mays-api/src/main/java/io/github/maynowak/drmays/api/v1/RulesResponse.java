package io.github.maynowak.drmays.api.v1;

import java.util.List;

/**
 * Registered rules available to the diagnosis endpoint.
 *
 * @param rules rules in rule-ID order
 */
public record RulesResponse(List<RuleResponse> rules) {

  public RulesResponse {
    rules = List.copyOf(rules);
  }
}
