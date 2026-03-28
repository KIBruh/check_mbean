package de.rbfh;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.logging.Level;
import java.util.logging.Logger;

public class RateChecker implements Checker {
    private static final Logger logger = Logger.getLogger(RateChecker.class.getName());

    private final CheckerConfig config;
    private final String statefile;

    public RateChecker(CheckerConfig config) {
        this.config = config;
        this.statefile = config.statefile() != null ? config.statefile() : defaultStatefile();
    }

    private String defaultStatefile() {
        String tempDir = System.getProperty("java.io.tmpdir");
        String hash = generateStatefileHash();
        String safeObjectName = config.objectName().replace(":", "_").replace(",", "_").replace("=", "_").replace(".", "_");
        String safeAttribute = config.attribute().replace(".", "_");
        return tempDir + "/check_mbean_" + hash + "_" + safeObjectName + "_" + safeAttribute + ".state";
    }

    private String generateStatefileHash() {
        String data = config.jmxUrl() + config.objectName() + config.attribute() + (config.path() != null ? config.path() : "");
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-224");
            byte[] hash = digest.digest(data.getBytes(StandardCharsets.UTF_8));
            StringBuilder hex = new StringBuilder();
            for (byte b : hash) {
                hex.append(String.format("%02x", b));
            }
            return hex.substring(0, 10);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-224 not available", e);
        }
    }

    @Override
    public NaemonOutput check() {
        try {
            Optional<Object> valueOpt = config.jmxClient().getAttribute(
                config.objectNameAsObjectName(), config.attribute(), config.path());

            if (valueOpt.isEmpty()) {
                return NaemonOutput.unknown("Could not retrieve attribute: " + config.attribute());
            }

            Object value = valueOpt.get();

            if (!(value instanceof Number number)) {
                throw new IllegalArgumentException("Attribute value is not numeric: " + value.getClass().getName());
            }

            double currentValue = number.doubleValue();
            long currentTime = System.currentTimeMillis();

            List<Measurement> measurements = loadMeasurements();
            boolean ignoreState = shouldIgnoreState(measurements, currentValue);
            
            if (ignoreState) {
                measurements.clear();
            }

            double rate = 0;
            long rateWindowSeconds = 0;

            if (!ignoreState && hasValidState(measurements, currentTime)) {
                Measurement rateMeasurement = findRateMeasurement(measurements, currentTime);
                if (rateMeasurement != null) {
                    rate = calculateRate(rateMeasurement.value, currentValue,
                                         rateMeasurement.timestamp, currentTime);
                    rateWindowSeconds = (currentTime - rateMeasurement.timestamp) / 1000;
                    logger.log(Level.FINE, "Using historical measurement: rate={0}, window={1}s", new Object[]{rate, rateWindowSeconds});
                } else {
                    ignoreState = true;
                }
            }

            if (ignoreState || rateWindowSeconds == 0) {
                logger.log(Level.FINE, "State ignored, performing two-point measurement");
                saveMeasurement(currentValue, currentTime);
                measurements.add(new Measurement(currentTime, currentValue));

                if (measurements.size() < 2) {
                    logger.log(Level.INFO, "Collecting initial measurements, waiting " + config.minRateInterval() + "s for second measurement...");
                    try {
                        Thread.sleep(config.minRateInterval() * 1000L);
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        return NaemonOutput.unknown("Measurement interrupted");
                    }

                    Optional<Object> retryValueOpt = config.jmxClient().getAttribute(
                        config.objectNameAsObjectName(), config.attribute(), config.path());
                    if (retryValueOpt.isEmpty()) {
                        return NaemonOutput.unknown("Could not retrieve attribute on retry");
                    }

                    Object retryValue = retryValueOpt.get();
                    if (!(retryValue instanceof Number retryNumber)) {
                        return NaemonOutput.unknown("Attribute is not numeric");
                    }

                    double retryValueDbl = retryNumber.doubleValue();
                    long retryTime = System.currentTimeMillis();

                    measurements.add(new Measurement(retryTime, retryValueDbl));
                }

                Measurement first = measurements.get(measurements.size() - 2);
                Measurement second = measurements.get(measurements.size() - 1);
                rate = calculateRate(first.value, second.value, first.timestamp, second.timestamp);
                rateWindowSeconds = (second.timestamp - first.timestamp) / 1000;
            }

            double divisor = config.divisor();
            if (divisor != 1.0) {
                rate = rate / divisor;
            }

            saveMeasurements(measurements, currentValue, currentTime);

            NaemonStatus status = NaemonStatus.OK;
            if (config.criticalThreshold() != null && config.criticalThreshold().isViolated(rate)) {
                status = NaemonStatus.CRITICAL;
            } else if (config.warningThreshold() != null && config.warningThreshold().isViolated(rate)) {
                status = NaemonStatus.WARNING;
            }

            String attributeLabel = config.attribute() + (config.path() != null ? "." + config.path() : "");
            String formattedRate = NaemonOutput.formatNumberForHuman(rate);
            String uom = config.uom();
            String unitSuffix = uom != null ? " " + uom : "/min";

            NaemonOutput output = new NaemonOutput(status,
                attributeLabel + " has a rate of " + formattedRate + unitSuffix + " (rate window " + rateWindowSeconds + "s)");

            output.addPerfData(attributeLabel, (long) currentValue, "c", null, null);
            output.addPerfData(attributeLabel + ".rate", rate, config.uom(), config.warningThreshold(), config.criticalThreshold());

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

    private boolean shouldIgnoreState(List<Measurement> measurements, double currentValue) {
        if (measurements.isEmpty()) {
            return true;
        }

        Measurement last = measurements.get(measurements.size() - 1);
        if (currentValue < last.value) {
            logger.log(Level.INFO, "Counter reset detected (current=" + currentValue + ", last=" + last.value + "), ignoring state");
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
        return ageSeconds <= 2 * config.meanRateInterval();
    }

    private Measurement findRateMeasurement(List<Measurement> measurements, long currentTime) {
        long targetTime = currentTime - (config.meanRateInterval() * 1000L);

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
                        logger.log(Level.WARNING, "Invalid measurement line: " + line);
                    }
                }
            }
        } catch (IOException e) {
            logger.log(Level.WARNING, "Could not load state file: " + e.getMessage());
        }

        return measurements;
    }

    private void saveMeasurements(List<Measurement> measurements, double currentValue, long currentTime) {
        long purgeThreshold = currentTime - (3L * config.meanRateInterval() * 1000L);

        measurements.removeIf(m -> m.timestamp < purgeThreshold);
        measurements.add(new Measurement(currentTime, currentValue));

        Path path = Paths.get(statefile);
        try (BufferedWriter writer = Files.newBufferedWriter(path)) {
            for (Measurement m : measurements) {
                writer.write(m.timestamp + " " + m.value);
                writer.newLine();
            }
        } catch (IOException e) {
            logger.log(Level.WARNING, "Could not save state file: " + e.getMessage());
        }
    }

    private void saveMeasurement(double value, long timestamp) {
        Path path = Paths.get(statefile);
        try (BufferedWriter writer = Files.newBufferedWriter(path, java.nio.file.StandardOpenOption.CREATE,
                                                               java.nio.file.StandardOpenOption.APPEND)) {
            writer.write(timestamp + " " + value);
            writer.newLine();
        } catch (IOException e) {
            logger.log(Level.WARNING, "Could not save measurement: " + e.getMessage());
        }
    }

    private record Measurement(long timestamp, double value) {
    }
}
