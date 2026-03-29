package io.github.kibruh;

import java.util.Objects;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

/**
 * String threshold implementation using a regular expression.
 */
public record StringThreshold(String regex, boolean negated, NaemonStatus status) implements Threshold {

    public static StringThreshold parse(String thresholdStr, NaemonStatus status) {
        Objects.requireNonNull(thresholdStr, "Threshold string cannot be null");
        Objects.requireNonNull(status, "Status cannot be null");

        if (thresholdStr.isEmpty()) {
            throw new IllegalArgumentException("Threshold string cannot be empty");
        }

        boolean negated = false;
        String regex = thresholdStr;

        if (thresholdStr.startsWith("!") || thresholdStr.startsWith("~")) {
            negated = true;
            regex = thresholdStr.substring(1);
        }

        // Validate regex
        try {
            Pattern.compile(regex);
        } catch (PatternSyntaxException e) {
            throw new IllegalArgumentException("Invalid regex: " + regex, e);
        }

        return new StringThreshold(regex, negated, status);
    }

    @Override
    public boolean isViolated(Object value) {
        String s = String.valueOf(value);
        try {
            boolean matched = Pattern.compile(regex()).matcher(s).find();
            return negated() ? !matched : matched;
        } catch (PatternSyntaxException e) {
            return true; // If regex fails at runtime, it's a violation
        }
    }

    @Override
    public String formatForPerfData() {
        return "";
    }

    @Override
    public String toString() {
        return (negated() ? "!" : "") + regex();
    }
}
