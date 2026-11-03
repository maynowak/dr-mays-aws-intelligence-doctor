package io.github.maynowak.drmays.core.rule;

import io.github.maynowak.drmays.core.validation.DomainChecks;

import java.util.regex.Pattern;

/**
 * Stable identifier of a diagnostic rule, for example {@code TAG-OWNER-001}.
 */
public record RuleId(String value) {

    private static final Pattern FORMAT = Pattern.compile("[A-Z][A-Z0-9]*(?:-[A-Z0-9]+)*");
    private static final int MAX_LENGTH = 64;

    public RuleId {
        value = DomainChecks.requireText(value, "value");
        if (value.length() > MAX_LENGTH || !FORMAT.matcher(value).matches()) {
            throw new IllegalArgumentException(
                    "value must be 1-" + MAX_LENGTH + " uppercase alphanumeric segments separated by '-'");
        }
    }
}
