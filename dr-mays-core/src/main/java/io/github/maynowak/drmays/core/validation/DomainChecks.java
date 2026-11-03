package io.github.maynowak.drmays.core.validation;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

/**
 * Small validation helpers shared by domain value objects.
 */
public final class DomainChecks {

    private DomainChecks() {
    }

    /**
     * Requires a non-null, non-blank string.
     *
     * @param value the value to validate
     * @param field the field name used in error messages
     * @return the original value
     */
    public static String requireText(String value, String field) {
        Objects.requireNonNull(value, field);
        if (value.isBlank()) {
            throw new IllegalArgumentException(field + " must not be blank");
        }
        return value;
    }

    /**
     * Copies a string map, rejecting blank keys and null values.
     * Iteration order of the input is preserved.
     *
     * @param values the map to copy
     * @param field the field name used in error messages
     * @return an unmodifiable copy
     */
    public static Map<String, String> immutableTextMap(Map<String, String> values, String field) {
        Objects.requireNonNull(values, field);
        var copy = new LinkedHashMap<String, String>();
        for (var entry : values.entrySet()) {
            var key = requireText(entry.getKey(), field + " key");
            var value = Objects.requireNonNull(entry.getValue(), field + " value for " + key);
            copy.put(key, value);
        }
        return Collections.unmodifiableMap(copy);
    }
}
