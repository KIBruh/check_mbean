package io.github.kibruh;

import java.util.Objects;

public record Threshold(Range range, NaemonStatus status) {

    public static Threshold parse(String thresholdStr, NaemonStatus status) {
        Objects.requireNonNull(thresholdStr, "Threshold string cannot be null");
        Objects.requireNonNull(status, "Status cannot be null");

        if (thresholdStr.isEmpty()) {
            return null;
        }

        Range range = Range.parse(thresholdStr);
        return new Threshold(range, status);
    }

    public boolean isViolated(double value) {
        return !range().isInRange(value);
    }

    public String formatForPerfData() {
        if (range().start() == Double.NEGATIVE_INFINITY && range().end() == Double.POSITIVE_INFINITY) {
            return "";
        }
        if (range().start() == Double.NEGATIVE_INFINITY) {
            return "~:" + formatEnd(range().end());
        }
        if (range().end() == Double.POSITIVE_INFINITY) {
            return formatStart(range().start()) + ":";
        }
        return formatStart(range().start()) + ":" + formatEnd(range().end());
    }

    private String formatStart(double value) {
        if (value == (long) value) {
            return String.format("%d", (long) value);
        }
        return String.format("%s", value);
    }

    private String formatEnd(double value) {
        if (value == (long) value) {
            return String.format("%d", (long) value);
        }
        return String.format("%s", value);
    }

    @Override
    public String toString() {
        return range().toString();
    }
}
