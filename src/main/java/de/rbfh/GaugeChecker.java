package de.rbfh;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.management.ObjectName;
import java.io.IOException;
import java.util.Optional;

public class GaugeChecker implements Checker {
    private static final Logger logger = LoggerFactory.getLogger(GaugeChecker.class);

    private final JmxClient jmxClient;
    private final String objectName;
    private final String attribute;
    private final String path;
    private final Threshold warningThreshold;
    private final Threshold criticalThreshold;

    public GaugeChecker(JmxClient jmxClient, String objectName, String attribute,
                        String path, Threshold warningThreshold, Threshold criticalThreshold) {
        this.jmxClient = jmxClient;
        this.objectName = objectName;
        this.attribute = attribute;
        this.path = path;
        this.warningThreshold = warningThreshold;
        this.criticalThreshold = criticalThreshold;
    }

    @Override
    public NaemonOutput check() {
        try {
            ObjectName objectNameObj = new ObjectName(objectName);
            Optional<Object> valueOpt = jmxClient.getAttribute(objectNameObj, attribute, path);

            if (valueOpt.isEmpty()) {
                return NaemonOutput.unknown("Could not retrieve attribute: " + attribute + (path != null ? "." + path : ""));
            }

            Object value = valueOpt.get();

            if (!(value instanceof Number number)) {
                throw new IllegalArgumentException("Attribute value is not numeric: " + value.getClass().getName());
            }

            double doubleValue = number.doubleValue();

            NaemonStatus status = NaemonStatus.OK;
            if (criticalThreshold != null && criticalThreshold.isViolated(doubleValue)) {
                status = NaemonStatus.CRITICAL;
            } else if (warningThreshold != null && warningThreshold.isViolated(doubleValue)) {
                status = NaemonStatus.WARNING;
            }

            String attributeLabel = attribute + (path != null ? "." + path : "");
            String formattedValue = NaemonOutput.formatNumber(number);

            NaemonOutput output = new NaemonOutput(status, attributeLabel + " is " + formattedValue);
            output.addPerfData(attributeLabel, number, "", warningThreshold, criticalThreshold);

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
