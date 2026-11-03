package io.github.maynowak.drmays.api.v1;

/**
 * One registered diagnostic rule.
 *
 * @param ruleId stable rule identifier
 * @param version rule version
 * @param description rule description
 */
public record RuleResponse(String ruleId, String version, String description) {}
