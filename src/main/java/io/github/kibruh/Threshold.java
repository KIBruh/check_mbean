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
        return range().formatForPerfData();
    }

    @Override
    public String toString() {
        return range().toString();
    }
}
