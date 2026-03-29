package io.github.kibruh;

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.Locale;

/**
 * Utility class for formatting numbers for Naemon/Nagios output and performance data.
 */
public final class FormatUtils {

    private static final DecimalFormat PLAIN_FORMAT = createPlainFormat();
    private static final DecimalFormat HUMAN_FORMAT = createHumanFormat();

    private FormatUtils() {
        // Utility class
    }

    private static DecimalFormat createPlainFormat() {
        DecimalFormatSymbols symbols = DecimalFormatSymbols.getInstance(Locale.US);
        DecimalFormat format = new DecimalFormat("0.##########", symbols);
        format.setGroupingUsed(false);
        return format;
    }

    private static DecimalFormat createHumanFormat() {
        DecimalFormatSymbols symbols = DecimalFormatSymbols.getInstance(Locale.US);
        DecimalFormat format = new DecimalFormat("0.###", symbols);
        format.setGroupingUsed(false);
        return format;
    }

    /**
     * Formats a number for performance data output.
     *
     * @param value The number to format.
     * @return A string representation of the number.
     */
    public static String formatNumber(Number value) {
        if (value == null) {
            return "0";
        }

        if (value instanceof Double || value instanceof Float) {
            double d = value.doubleValue();
            if (Double.isNaN(d) || Double.isInfinite(d)) {
                return "0";
            }
            if (d == (long) d && Math.abs(d) < Long.MAX_VALUE) {
                return String.format(Locale.US, "%d", (long) d);
            }
        }

        if (value instanceof Long || value instanceof Integer || value instanceof Short || value instanceof Byte) {
            return PLAIN_FORMAT.format(value.longValue());
        }

        return PLAIN_FORMAT.format(value.doubleValue());
    }

    /**
     * Formats a double value for human-readable output (e.g., in status messages).
     *
     * @param value The double value to format.
     * @return A string representation with limited decimal places.
     */
    public static String formatNumberForHuman(double value) {
        if (Double.isNaN(value) || Double.isInfinite(value)) {
            return "0";
        }
        if (value == (long) value && Math.abs(value) < Long.MAX_VALUE) {
            return String.format(Locale.US, "%d", (long) value);
        }
        return HUMAN_FORMAT.format(value);
    }
}
