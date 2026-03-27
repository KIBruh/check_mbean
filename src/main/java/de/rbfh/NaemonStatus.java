package de.rbfh;

public enum NaemonStatus {
    OK(0, "OK"),
    WARNING(1, "WARNING"),
    CRITICAL(2, "CRITICAL"),
    UNKNOWN(3, "UNKNOWN");

    private final int exitCode;
    private final String label;

    NaemonStatus(int exitCode, String label) {
        this.exitCode = exitCode;
        this.label = label;
    }

    public int getExitCode() {
        return exitCode;
    }

    public String getLabel() {
        return label;
    }

    public static NaemonStatus fromString(String status) {
        if (status == null) {
            return UNKNOWN;
        }
        return switch (status.toUpperCase()) {
            case "OK" -> OK;
            case "WARNING" -> WARNING;
            case "CRITICAL" -> CRITICAL;
            default -> UNKNOWN;
        };
    }
}
