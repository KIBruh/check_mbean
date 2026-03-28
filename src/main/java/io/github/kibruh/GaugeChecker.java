package io.github.kibruh;

import java.io.IOException;
import java.util.Optional;
import java.util.logging.Level;
import java.util.logging.Logger;

public class GaugeChecker implements Checker {
    private static final Logger logger = Logger.getLogger(GaugeChecker.class.getName());

    private final CheckerConfig config;

    public GaugeChecker(CheckerConfig config) {
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

            Object value = valueOpt.get();

            if (!(value instanceof Number number)) {
                throw new IllegalArgumentException("Attribute value is not numeric: " + value.getClass().getName());
            }

            double divisor = config.divisor();
            double doubleValue = number.doubleValue() / divisor;

            NaemonStatus status = NaemonStatus.OK;
            if (config.criticalThreshold() != null && config.criticalThreshold().isViolated(doubleValue)) {
                status = NaemonStatus.CRITICAL;
            } else if (config.warningThreshold() != null && config.warningThreshold().isViolated(doubleValue)) {
                status = NaemonStatus.WARNING;
            }

            String attributeLabel = config.attribute() + (config.path() != null ? "." + config.path() : "");
            String formattedValue = NaemonOutput.formatNumberForHuman(doubleValue);
            String uom = config.uom() != null ? " " + config.uom() : "";

            NaemonOutput output = new NaemonOutput(status, attributeLabel + " is " + formattedValue + uom);
            output.addPerfData(attributeLabel, doubleValue, config.uom(), config.warningThreshold(), config.criticalThreshold());

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
