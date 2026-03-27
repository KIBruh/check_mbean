package de.rbfh;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.management.ObjectName;
import java.io.IOException;
import java.util.Optional;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

public class StringChecker implements Checker {
    private static final Logger logger = LoggerFactory.getLogger(StringChecker.class);

    private final JmxClient jmxClient;
    private final String objectName;
    private final String attribute;
    private final String path;
    private final String warningRegex;
    private final String criticalRegex;

    public StringChecker(JmxClient jmxClient, String objectName, String attribute,
                        String path, String warningRegex, String criticalRegex) {
        this.jmxClient = jmxClient;
        this.objectName = objectName;
        this.attribute = attribute;
        this.path = path;
        this.warningRegex = warningRegex;
        this.criticalRegex = criticalRegex;
    }

    @Override
    public NaemonOutput check() {
        try {
            ObjectName objectNameObj = new ObjectName(objectName);
            Optional<Object> valueOpt = jmxClient.getAttribute(objectNameObj, attribute, path);

            if (valueOpt.isEmpty()) {
                return NaemonOutput.unknown("Could not retrieve attribute: " + attribute + (path != null ? "." + path : ""));
            }

            String value = String.valueOf(valueOpt.get());

            boolean criticalMatch = false;
            boolean warningMatch = false;

            if (criticalRegex != null && !criticalRegex.isEmpty()) {
                try {
                    criticalMatch = Pattern.compile(criticalRegex).matcher(value).find();
                } catch (PatternSyntaxException e) {
                    return NaemonOutput.unknown("Invalid critical regex: " + e.getMessage());
                }
            }

            if (!criticalMatch && warningRegex != null && !warningRegex.isEmpty()) {
                try {
                    warningMatch = Pattern.compile(warningRegex).matcher(value).find();
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

            String attributeLabel = attribute + (path != null ? "." + path : "");
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
