package io.github.kibruh;

import java.util.Objects;

public record Range(double start, double end, boolean invert, boolean startInclusive, boolean endInclusive) {

    public static Range parse(String rangeStr) {
        Objects.requireNonNull(rangeStr, "Range string cannot be null");

        boolean invert = false;
        String parsed = rangeStr;

        if (rangeStr.startsWith("@")) {
            invert = true;
            parsed = rangeStr.substring(1);
        }

        if (parsed.isEmpty()) {
            throw new IllegalArgumentException("Invalid range: " + rangeStr);
        }

        double start;
        double end;
        boolean startInclusive = true;
        boolean endInclusive = true;

        if (parsed.equals("~")) {
            start = Double.NEGATIVE_INFINITY;
            end = Double.POSITIVE_INFINITY;
        } else if (parsed.startsWith("~:")) {
            start = Double.NEGATIVE_INFINITY;
            end = parseEnd(parsed.substring(2));
        } else if (parsed.endsWith(":~")) {
            start = parseStart(parsed.substring(0, parsed.length() - 2));
            end = Double.POSITIVE_INFINITY;
        } else if (parsed.contains(":")) {
            String[] parts = parsed.split(":", 2);
            start = parseStart(parts[0]);
            end = parseEnd(parts[1]);
        } else {
            start = 0;
            end = parseEnd(parsed);
        }

        if (start > end) {
            throw new IllegalArgumentException("Invalid range: start must be less than or equal to end");
        }

        return new Range(start, end, invert, startInclusive, endInclusive);
    }

    private static double parseStart(String s) {
        if (s.isEmpty()) {
            return 0;
        }
        return Double.parseDouble(s);
    }

    private static double parseEnd(String s) {
        if (s.isEmpty()) {
            return Double.POSITIVE_INFINITY;
        }
        return Double.parseDouble(s);
    }

    public boolean isInRange(double value) {
        boolean aboveStart = startInclusive() ? value >= start() : value > start();
        boolean belowEnd = endInclusive() ? value <= end() : value < end();
        boolean inside = aboveStart && belowEnd;

        return invert() ? !inside : inside;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        if (invert()) {
            sb.append("@");
        }
        if (start() == Double.NEGATIVE_INFINITY) {
            sb.append("~");
        } else {
            sb.append(formatNumber(start()));
        }
        sb.append(":");
        if (end() == Double.POSITIVE_INFINITY) {
            sb.append("~");
        } else {
            sb.append(formatNumber(end()));
        }
        return sb.toString();
    }

    private static String formatNumber(double value) {
        if (value == (long) value) {
            return String.format("%d", (long) value);
        }
        return String.format("%s", value);
    }
}
