package io.github.kibruh;

import java.util.Objects;

public class NaemonOutput {
    private final NaemonStatus status;
    private final String message;
    private final StringBuilder perfData;

    public NaemonOutput(NaemonStatus status, String message) {
        this.status = Objects.requireNonNull(status, "Status cannot be null");
        this.message = message != null ? message : "";
        this.perfData = new StringBuilder();
    }

    public NaemonOutput addPerfData(String label, Number value, String unit, Threshold warning, Threshold critical) {
        if (perfData.length() > 0) {
            perfData.append(" ");
        }

        perfData.append("'");
        perfData.append(escapePerfDataLabel(label));
        perfData.append("'=");
        perfData.append(FormatUtils.formatNumber(value));
        if (unit != null && !unit.isEmpty()) {
            perfData.append(unit);
        }

        String warnStr = warning != null ? warning.formatForPerfData() : "";
        String critStr = critical != null ? critical.formatForPerfData() : "";
        perfData.append(";");
        perfData.append(warnStr);
        perfData.append(";");
        perfData.append(critStr);
        perfData.append(";;");

        return this;
    }

    public NaemonOutput addPerfData(String label, Number value, String unit) {
        return addPerfData(label, value, unit, null, null);
    }

    public NaemonOutput addExecTime(double seconds) {
        if (perfData.length() > 0) {
            perfData.append(" ");
        }
        perfData.append("'time'=");
        perfData.append(FormatUtils.formatNumber(seconds));
        perfData.append("s");
        perfData.append(";;;");
        return this;
    }

    public static String formatNumber(Number value) {
        return FormatUtils.formatNumber(value);
    }

    public static String formatNumberForHuman(double value) {
        return FormatUtils.formatNumberForHuman(value);
    }

    private static String escapePerfDataLabel(String label) {
        if (label == null) {
            return "";
        }
        return label.replace("'", "_").replace("=", "_").replace(";", "_");
    }

    public NaemonStatus getStatus() {
        return status;
    }

    public String getMessage() {
        return message;
    }

    public String getPerfData() {
        return perfData.toString();
    }

    public String build() {
        StringBuilder sb = new StringBuilder();
        sb.append(status.getLabel());
        sb.append(" - ");
        sb.append(message);
        if (perfData.length() > 0) {
            sb.append(" | ");
            sb.append(perfData);
        }
        return sb.toString();
    }

    public static NaemonOutput ok(String message) {
        return new NaemonOutput(NaemonStatus.OK, message);
    }

    public static NaemonOutput warning(String message) {
        return new NaemonOutput(NaemonStatus.WARNING, message);
    }

    public static NaemonOutput critical(String message) {
        return new NaemonOutput(NaemonStatus.CRITICAL, message);
    }

    public static NaemonOutput unknown(String message) {
        return new NaemonOutput(NaemonStatus.UNKNOWN, message);
    }
}
