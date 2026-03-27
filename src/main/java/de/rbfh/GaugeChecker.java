package de.rbfh;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.Optional;

public class GaugeChecker implements Checker {
    private static final Logger logger = LoggerFactory.getLogger(GaugeChecker.class);

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

            double doubleValue = number.doubleValue();

            NaemonStatus status = NaemonStatus.OK;
            if (config.criticalThreshold() != null && config.criticalThreshold().isViolated(doubleValue)) {
                status = NaemonStatus.CRITICAL;
            } else if (config.warningThreshold() != null && config.warningThreshold().isViolated(doubleValue)) {
                status = NaemonStatus.WARNING;
            }

            String attributeLabel = config.attribute() + (config.path() != null ? "." + config.path() : "");
            String formattedValue = NaemonOutput.formatNumber(number);

            NaemonOutput output = new NaemonOutput(status, attributeLabel + " is " + formattedValue);
            output.addPerfData(attributeLabel, number, "", config.warningThreshold(), config.criticalThreshold());

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
