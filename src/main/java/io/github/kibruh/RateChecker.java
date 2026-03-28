package io.github.kibruh;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Stateful rate checker for JMX counter attributes.
 * Implements the logic specified in ratemode.txt.
 */
public class RateChecker implements Checker {
    private static final Logger logger = Logger.getLogger(RateChecker.class.getName());

    private final CheckerConfig config;
    private final Path statePath;

    public RateChecker(CheckerConfig config) {
        this.config = Objects.requireNonNull(config);
        String path = config.statefile() != null ? config.statefile() : generateDefaultStatePath();
        this.statePath = Paths.get(path);
    }

    @Override
    public NaemonOutput check() {
        try {
            final long now = System.currentTimeMillis();
            final double currentValue = fetchValue();

            List<Measurement> measurements = loadMeasurements();
            
            // Rule: Ignore state if counter reset (current < last)
            if (!measurements.isEmpty() && currentValue < measurements.getLast().value()) {
                logger.info("Counter reset detected (current: %s, last: %s). Resetting state."
                        .formatted(currentValue, measurements.getLast().value()));
                measurements.clear();
            }

            // Rule: Ignore state if last measurement is too old
            if (!measurements.isEmpty()) {
                long lastAgeSeconds = (now - measurements.getLast().timestamp()) / 1000;
                if (lastAgeSeconds > (long) config.rateWindowMultiplier() * config.meanRateInterval()) {
                    logger.log(Level.FINE, "State expired (last seen %ds ago). Resetting.".formatted(lastAgeSeconds));
                    measurements.clear();
                }
            }

            double rate;
            long windowSeconds;

            if (measurements.isEmpty()) {
                // Rule: If state is ignored, make two measurements --min-rate-interval seconds apart
                logger.info("Initial measurement phase. Waiting %ds for second point...".formatted(config.minRateInterval()));
                Measurement m1 = new Measurement(now, currentValue);
                
                try {
                    Thread.sleep(config.minRateInterval() * 1000L);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    return NaemonOutput.unknown("Interrupted during initial measurement wait");
                }

                long secondNow = System.currentTimeMillis();
                double secondValue = fetchValue();
                Measurement m2 = new Measurement(secondNow, secondValue);
                
                measurements.add(m1);
                measurements.add(m2);
                
                rate = calculateRate(m1, m2);
                windowSeconds = (secondNow - now) / 1000;
            } else {
                // Rule: Rate is calculated with the measurement nearest to (now - mean-rate-interval)
                long targetTime = now - (config.meanRateInterval() * 1000L);
                Measurement nearest = measurements.stream()
                        .min(Comparator.comparingLong(m -> Math.abs(m.timestamp() - targetTime)))
                        .orElseThrow();

                rate = calculateRate(nearest, new Measurement(now, currentValue));
                windowSeconds = (now - nearest.timestamp()) / 1000;
                
                measurements.add(new Measurement(now, currentValue));
            }

            // Apply divisor if specified
            if (config.divisor() != 1.0) {
                rate /= config.divisor();
            }

            saveMeasurements(measurements, now);

            return formatOutput(rate, windowSeconds, currentValue);

        } catch (Exception e) {
            logger.log(Level.SEVERE, "Rate check failed: " + e.getMessage(), e);
            return NaemonOutput.unknown("Rate calculation error: " + e.getMessage());
        }
    }

    private double fetchValue() throws IOException {
        return config.jmxClient()
                .getAttribute(config.objectNameAsObjectName(), config.attribute(), config.path())
                .filter(v -> v instanceof Number)
                .map(v -> ((Number) v).doubleValue())
                .orElseThrow(() -> new IOException("Attribute '%s' is missing or not numeric".formatted(config.attribute())));
    }

    private double calculateRate(Measurement m1, Measurement m2) {
        double deltaVal = m2.value() - m1.value();
        double deltaSec = (m2.timestamp() - m1.timestamp()) / 1000.0;
        return (deltaSec > 0) ? (deltaVal / deltaSec) * 60.0 : 0.0;
    }

    private List<Measurement> loadMeasurements() {
        if (!Files.exists(statePath)) return new ArrayList<>();
        try (var lines = Files.lines(statePath)) {
            return lines.map(String::trim)
                    .filter(l -> !l.isEmpty())
                    .map(l -> l.split("\\s+"))
                    .filter(p -> p.length == 2)
                    .map(p -> new Measurement(Long.parseLong(p[0]), Double.parseDouble(p[1])))
                    .collect(ArrayList::new, ArrayList::add, ArrayList::addAll);
        } catch (Exception e) {
            logger.warning("Could not read state file %s: %s".formatted(statePath, e.getMessage()));
            return new ArrayList<>();
        }
    }

    private void saveMeasurements(List<Measurement> measurements, long now) {
        // Rule: save all measurements ... newer than three times --mean-rate-interval
        // Rule: statefile must not contain any data older than mean-rate-interval * (multiplier + 2)
        long hardLimit = now - ((long) config.meanRateInterval() * (config.rateWindowMultiplier() + 2) * 1000L);
        
        List<String> lines = measurements.stream()
                .filter(m -> m.timestamp() >= hardLimit)
                .map(m -> "%d %s".formatted(m.timestamp(), NaemonOutput.formatNumber(m.value())))
                .toList();

        try {
            Files.write(statePath, lines, StandardCharsets.UTF_8);
        } catch (IOException e) {
            logger.warning("Could not write state file %s: %s".formatted(statePath, e.getMessage()));
        }
    }

    private NaemonOutput formatOutput(double rate, long window, double rawValue) {
        NaemonStatus status = NaemonStatus.OK;
        if (config.criticalThreshold() != null && config.criticalThreshold().isViolated(rate)) {
            status = NaemonStatus.CRITICAL;
        } else if (config.warningThreshold() != null && config.warningThreshold().isViolated(rate)) {
            status = NaemonStatus.WARNING;
        }

        String attr = config.attribute() + (config.path() != null ? "." + config.path() : "");
        String uom = config.uom() != null ? config.uom() : "/min";
        String message = "%s rate is %s %s (window %ds)".formatted(attr, NaemonOutput.formatNumberForHuman(rate), uom, window);

        return new NaemonOutput(status, message)
                .addPerfData(attr, rawValue, "c")
                .addPerfData(attr + ".rate", rate, config.uom(), config.warningThreshold(), config.criticalThreshold())
                .addPerfData("check_rate_window", window, "s");
    }

    private String generateDefaultStatePath() {
        String key = config.jmxUrl() + config.objectName() + config.attribute() + Objects.toString(config.path(), "");
        try {
            byte[] hash = MessageDigest.getInstance("SHA-256").digest(key.getBytes(StandardCharsets.UTF_8));
            StringBuilder hex = new StringBuilder();
            for (int i = 0; i < 8; i++) hex.append("%02x".formatted(hash[i]));
            return System.getProperty("java.io.tmpdir") + "/check_mbean_" + hex + ".state";
        } catch (NoSuchAlgorithmException e) {
            return System.getProperty("java.io.tmpdir") + "/check_mbean_fallback.state";
        }
    }

    private record Measurement(long timestamp, double value) {}
}
