package de.rbfh;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.management.ObjectName;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class RateChecker implements Checker {
    private static final Logger logger = LoggerFactory.getLogger(RateChecker.class);

    private final JmxClient jmxClient;
    private final String objectName;
    private final String attribute;
    private final String path;
    private final String statefile;
    private final int meanRateInterval;
    private final int minRateInterval;
    private final Threshold warningThreshold;
    private final Threshold criticalThreshold;

    public RateChecker(JmxClient jmxClient, String objectName, String attribute,
                       String path, String statefile, int meanRateInterval,
                       int minRateInterval, Threshold warningThreshold,
                       Threshold criticalThreshold) {
        this.jmxClient = jmxClient;
        this.objectName = objectName;
        this.attribute = attribute;
        this.path = path;
        this.statefile = statefile != null ? statefile : defaultStatefile();
        this.meanRateInterval = meanRateInterval;
        this.minRateInterval = minRateInterval;
        this.warningThreshold = warningThreshold;
        this.criticalThreshold = criticalThreshold;
    }

    private String defaultStatefile() {
        String tempDir = System.getProperty("java.io.tmpdir");
        String safeObjectName = objectName.replace(":", "_").replace(",", "_").replace("=", "_");
        String safeAttribute = attribute.replace(".", "_");
        return tempDir + "/check_mbean_" + safeObjectName + "_" + safeAttribute + ".state";
    }

    @Override
    public NaemonOutput check() {
        try {
            ObjectName objectNameObj = new ObjectName(objectName);
            Optional<Object> valueOpt = jmxClient.getAttribute(objectNameObj, attribute, path);

            if (valueOpt.isEmpty()) {
                return NaemonOutput.unknown("Could not retrieve attribute: " + attribute);
            }

            Object value = valueOpt.get();

            if (!(value instanceof Number number)) {
                throw new IllegalArgumentException("Attribute value is not numeric: " + value.getClass().getName());
            }

            double currentValue = number.doubleValue();
            long currentTime = System.currentTimeMillis();

            List<Measurement> measurements = loadMeasurements();
            boolean ignoreState = shouldIgnoreState(measurements, currentValue);

            double rate = 0;
            long rateWindowSeconds = 0;

            if (!ignoreState && hasValidState(measurements, currentTime)) {
                Measurement rateMeasurement = findRateMeasurement(measurements, currentTime);
                if (rateMeasurement != null) {
                    rate = calculateRate(rateMeasurement.value, currentValue,
                                         rateMeasurement.timestamp, currentTime);
                    rateWindowSeconds = (currentTime - rateMeasurement.timestamp) / 1000;
                    logger.debug("Using historical measurement: rate={}, window={}s", rate, rateWindowSeconds);
                } else {
                    ignoreState = true;
                }
            }

            if (ignoreState || rateWindowSeconds == 0) {
                logger.debug("State ignored, performing two-point measurement");
                saveMeasurement(currentValue, currentTime);
                measurements.add(new Measurement(currentTime, currentValue));

                if (measurements.size() < 2) {
                    return NaemonOutput.unknown("Waiting for second measurement...");
                }

                Measurement first = measurements.get(measurements.size() - 2);
                Measurement second = measurements.get(measurements.size() - 1);
                rate = calculateRate(first.value, second.value, first.timestamp, second.timestamp);
                rateWindowSeconds = (second.timestamp - first.timestamp) / 1000;
            }

            saveMeasurements(measurements, currentValue, currentTime);

            NaemonStatus status = NaemonStatus.OK;
            if (criticalThreshold != null && criticalThreshold.isViolated(rate)) {
                status = NaemonStatus.CRITICAL;
            } else if (warningThreshold != null && warningThreshold.isViolated(rate)) {
                status = NaemonStatus.WARNING;
            }

            String attributeLabel = attribute + (path != null ? "." + path : "");
            String formattedRate = NaemonOutput.formatNumber(rate);

            NaemonOutput output = new NaemonOutput(status,
                attributeLabel + " has a rate of " + formattedRate + "/min (rate window " + rateWindowSeconds + "s)");

            output.addPerfData(attributeLabel, (long) currentValue, "c", null, null);
            output.addPerfData(attributeLabel + ".rate", rate, "/min", warningThreshold, criticalThreshold);

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

    private boolean shouldIgnoreState(List<Measurement> measurements, double currentValue) {
        if (measurements.isEmpty()) {
            return true;
        }

        Measurement last = measurements.get(measurements.size() - 1);
        if (currentValue < last.value) {
            logger.info("Counter reset detected (current={}, last={}), ignoring state", currentValue, last.value);
            return true;
        }

        return false;
    }

    private boolean hasValidState(List<Measurement> measurements, long currentTime) {
        if (measurements.isEmpty()) {
            return false;
        }

        Measurement last = measurements.get(measurements.size() - 1);
        long ageSeconds = (currentTime - last.timestamp) / 1000;
        return ageSeconds <= 2 * meanRateInterval;
    }

    private Measurement findRateMeasurement(List<Measurement> measurements, long currentTime) {
        long targetTime = currentTime - (meanRateInterval * 1000L);

        Measurement closest = null;
        long closestDiff = Long.MAX_VALUE;

        for (Measurement m : measurements) {
            long diff = Math.abs(m.timestamp - targetTime);
            if (diff < closestDiff) {
                closestDiff = diff;
                closest = m;
            }
        }

        return closest;
    }

    private double calculateRate(double oldValue, double newValue, long oldTime, long newTime) {
        long deltaTimeSeconds = (newTime - oldTime) / 1000;
        if (deltaTimeSeconds <= 0) {
            return 0;
        }

        double deltaValue = newValue - oldValue;
        return (deltaValue / deltaTimeSeconds) * 60;
    }

    private List<Measurement> loadMeasurements() {
        List<Measurement> measurements = new ArrayList<>();
        Path path = Paths.get(statefile);

        if (!Files.exists(path)) {
            return measurements;
        }

        try (BufferedReader reader = Files.newBufferedReader(path)) {
            String line;
            while ((line = reader.readLine()) != null) {
                String[] parts = line.trim().split("\\s+", 2);
                if (parts.length == 2) {
                    try {
                        long timestamp = Long.parseLong(parts[0]);
                        double value = Double.parseDouble(parts[1]);
                        measurements.add(new Measurement(timestamp, value));
                    } catch (NumberFormatException e) {
                        logger.warn("Invalid measurement line: {}", line);
                    }
                }
            }
        } catch (IOException e) {
            logger.warn("Could not load state file: {}", e.getMessage());
        }

        return measurements;
    }

    private void saveMeasurements(List<Measurement> measurements, double currentValue, long currentTime) {
        long purgeThreshold = currentTime - (3L * meanRateInterval * 1000L);

        measurements.removeIf(m -> m.timestamp < purgeThreshold);
        measurements.add(new Measurement(currentTime, currentValue));

        Path path = Paths.get(statefile);
        try (BufferedWriter writer = Files.newBufferedWriter(path)) {
            for (Measurement m : measurements) {
                writer.write(m.timestamp + " " + m.value);
                writer.newLine();
            }
        } catch (IOException e) {
            logger.warn("Could not save state file: {}", e.getMessage());
        }
    }

    private void saveMeasurement(double value, long timestamp) {
        Path path = Paths.get(statefile);
        try (BufferedWriter writer = Files.newBufferedWriter(path, java.nio.file.StandardOpenOption.CREATE,
                                                               java.nio.file.StandardOpenOption.APPEND)) {
            writer.write(timestamp + " " + value);
            writer.newLine();
        } catch (IOException e) {
            logger.warn("Could not save measurement: {}", e.getMessage());
        }
    }

    private record Measurement(long timestamp, double value) {
    }
}
