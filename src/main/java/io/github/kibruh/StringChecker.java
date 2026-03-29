package io.github.kibruh;

import java.io.IOException;
import java.util.Optional;
import java.util.logging.Level;
import java.util.logging.Logger;

public class StringChecker implements Checker {
    private static final Logger logger = Logger.getLogger(StringChecker.class.getName());

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

            NaemonStatus status = NaemonStatus.OK;
            if (config.criticalThreshold() != null && config.criticalThreshold().isViolated(value)) {
                status = NaemonStatus.CRITICAL;
            } else if (config.warningThreshold() != null && config.warningThreshold().isViolated(value)) {
                status = NaemonStatus.WARNING;
            }

            String attributeLabel = config.attribute() + (config.path() != null ? "." + config.path() : "");
            NaemonOutput output = new NaemonOutput(status, attributeLabel + " is '" + value + "'");

            return output;

        } catch (IllegalArgumentException e) {
            logger.log(Level.SEVERE, "Invalid argument: " + e.getMessage());
            return NaemonOutput.unknown(e.getMessage());
        } catch (IOException e) {
            logger.log(Level.SEVERE, "I/O error: " + e.getMessage());
            return NaemonOutput.unknown("Connection error: " + e.getMessage());
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Unexpected error: " + e.getMessage(), e);
            return NaemonOutput.unknown("Unexpected error: " + e.getMessage());
        }
    }
}
