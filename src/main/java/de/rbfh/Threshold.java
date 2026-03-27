package de.rbfh;

import java.util.Objects;

public class Threshold {
    private final Range range;
    private final NaemonStatus status;

    Threshold(Range range, NaemonStatus status) {
        this.range = Objects.requireNonNull(range, "Range cannot be null");
        this.status = Objects.requireNonNull(status, "Status cannot be null");
    }

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
        return !range.isInRange(value);
    }

    public Range getRange() {
        return range;
    }

    public NaemonStatus getStatus() {
        return status;
    }

    public String formatForPerfData() {
        if (range.getStart() == Double.NEGATIVE_INFINITY && range.getEnd() == Double.POSITIVE_INFINITY) {
            return "";
        }
        if (range.getStart() == Double.NEGATIVE_INFINITY) {
            return "~:" + formatEnd(range.getEnd());
        }
        if (range.getEnd() == Double.POSITIVE_INFINITY) {
            return formatStart(range.getStart()) + ":";
        }
        return formatStart(range.getStart()) + ":" + formatEnd(range.getEnd());
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
        return range.toString();
    }
}
