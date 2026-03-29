package io.github.kibruh;

import java.util.Objects;

/**
 * Common interface for monitoring thresholds.
 */
public interface Threshold {
    /**
     * Checks if the given value violates the threshold.
     *
     * @param value The value to check (numeric or string depending on mode).
     * @return true if violated, false otherwise.
     */
    boolean isViolated(Object value);

    /**
     * Formats the threshold for performance data output.
     *
     * @return A string representation of the threshold.
     */
    String formatForPerfData();

    /**
     * Returns the Naemon status to return if the threshold is violated.
     *
     * @return The Naemon status.
     */
    NaemonStatus status();

    /**
     * Parses a threshold string into a Threshold object.
     *
     * @param thresholdStr The string to parse.
     * @param status The status if violated.
     * @param isStringMode true if in string mode (regex), false if in numeric mode (range).
     * @return A Threshold object or null if thresholdStr is empty.
     */
    static Threshold parse(String thresholdStr, NaemonStatus status, boolean isStringMode) {
        Objects.requireNonNull(thresholdStr, "Threshold string cannot be null");
        Objects.requireNonNull(status, "Status cannot be null");

        if (thresholdStr.isEmpty()) {
            return null;
        }

        if (isStringMode) {
            return StringThreshold.parse(thresholdStr, status);
        } else {
            return NumericThreshold.parse(thresholdStr, status);
        }
    }
}
