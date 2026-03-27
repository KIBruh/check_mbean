package de.rbfh;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.Optional;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

public class StringChecker implements Checker {
    private static final Logger logger = LoggerFactory.getLogger(StringChecker.class);

    private final CheckerConfig config;

    public StringChecker(CheckerConfig config) {
        this.config = config;
    }

    @Override
    public NaemonOutput check() {
        try {
            Optional<Object> valueOpt = config.jmxClient().getAttribute(
                config.objectNameAsObjectName(), config.attribute(), config.path());

            if (valueOpt.isEmpty()) {
                return NaemonOutput.unknown("Could not retrieve attribute: " + config.attribute() 
                    + (config.path() != null ? "." + config.path() : ""));
            }

            String value = String.valueOf(valueOpt.get());

            boolean criticalMatch = false;
            boolean warningMatch = false;

            if (config.criticalRegex() != null && !config.criticalRegex().isEmpty()) {
                try {
                    criticalMatch = Pattern.compile(config.criticalRegex()).matcher(value).find();
                } catch (PatternSyntaxException e) {
                    return NaemonOutput.unknown("Invalid critical regex: " + e.getMessage());
                }
            }

            if (!criticalMatch && config.warningRegex() != null && !config.warningRegex().isEmpty()) {
                try {
                    warningMatch = Pattern.compile(config.warningRegex()).matcher(value).find();
                } catch (PatternSyntaxException e) {
                    return NaemonOutput.unknown("Invalid warning regex: " + e.getMessage());
                }
            }

            NaemonStatus status;
            if (criticalMatch) {
                status = NaemonStatus.CRITICAL;
            } else if (warningMatch) {
                status = NaemonStatus.WARNING;
            } else {
                status = NaemonStatus.OK;
            }

            String attributeLabel = config.attribute() + (config.path() != null ? "." + config.path() : "");
            NaemonOutput output = new NaemonOutput(status, attributeLabel + " is '" + value + "'");
            output.addPerfData(attributeLabel, value, "");

            return output;

        } catch (IllegalArgumentException e) {
            logger.error("Invalid argument: {}", e.getMessage());
            return NaemonOutput.unknown(e.getMessage());
        } catch (IOException e) {
            logger.error("I/O error: {}", e.getMessage());
            return NaemonOutput.unknown("Connection error: " + e.getMessage());
        } catch (Exception e) {
            logger.error("Unexpected error: {}", e.getMessage(), e);
            return NaemonOutput.unknown("Unexpected error: " + e.getMessage());
        }
    }
}
