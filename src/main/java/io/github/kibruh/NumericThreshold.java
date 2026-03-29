package io.github.kibruh;

import java.util.Objects;

/**
 * Numeric threshold implementation using a Range.
 */
public record NumericThreshold(Range range, NaemonStatus status) implements Threshold {

    public static NumericThreshold parse(String thresholdStr, NaemonStatus status) {
        Objects.requireNonNull(thresholdStr, "Threshold string cannot be null");
        Objects.requireNonNull(status, "Status cannot be null");

        Range range = Range.parse(thresholdStr);
        return new NumericThreshold(range, status);
    }

    @Override
    public boolean isViolated(Object value) {
        if (value instanceof Number number) {
            return !range().isInRange(number.doubleValue());
        }
        if (value instanceof String s) {
            try {
                return !range().isInRange(Double.parseDouble(s));
            } catch (NumberFormatException e) {
                return true; // If not a number, it violates the numeric threshold
            }
        }
        return true;
    }

    @Override
    public String formatForPerfData() {
        return range().formatForPerfData();
    }

    @Override
    public String toString() {
        return range().toString();
    }
}
