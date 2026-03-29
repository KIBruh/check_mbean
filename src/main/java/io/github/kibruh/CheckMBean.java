package io.github.kibruh;

import org.apache.commons.cli.ParseException;
import java.util.logging.ConsoleHandler;
import java.util.logging.Level;
import java.util.logging.Logger;

public class CheckMBean {
    private static final Logger logger = Logger.getLogger(CheckMBean.class.getName());

    public static void main(String[] args) {
        configureLogging();

        try {
            CliParser parser = new CliParser(args);

            if (parser.hasHelp()) {
                parser.printHelp();
                System.exit(NaemonStatus.UNKNOWN.getExitCode());
            }

            parser.validateRequired();

            JmxClient jmxClient = new DefaultJmxClient(
                    parser.getJmxUrl(),
                    parser.getUsername(),
                    parser.getPassword(),
                    parser.getTimeout()
            );

            jmxClient.connect();

            try {
                if ("fetch".equalsIgnoreCase(parser.getMode())) {
                    String value = fetchValue(parser, jmxClient);
                    System.out.println(value);
                } else {
                    Checker checker = createChecker(parser, jmxClient);
                    long startTime = System.nanoTime();
                    NaemonOutput output = checker.check();
                    double execTimeSeconds = (System.nanoTime() - startTime) / 1_000_000_000.0;
                    output.addExecTime(execTimeSeconds);
                    System.out.println(output.build());
                    System.exit(output.getStatus().getExitCode());
                }
            } finally {
                jmxClient.close();
            }

        } catch (ParseException e) {
            System.err.println("Error parsing arguments: " + e.getMessage());
            System.exit(NaemonStatus.UNKNOWN.getExitCode());
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Unexpected error", e);
            System.err.println("Error: " + e.getMessage());
            System.exit(NaemonStatus.UNKNOWN.getExitCode());
        }
    }

    private static Checker createChecker(CliParser parser, JmxClient jmxClient) {
        String mode = parser.getMode().toLowerCase();
        boolean isStringMode = "string".equals(mode);

        Threshold warningThreshold = null;
        Threshold criticalThreshold = null;

        if (parser.getWarningThreshold() != null) {
            warningThreshold = Threshold.parse(parser.getWarningThreshold(), NaemonStatus.WARNING, isStringMode);
        }
        if (parser.getCriticalThreshold() != null) {
            criticalThreshold = Threshold.parse(parser.getCriticalThreshold(), NaemonStatus.CRITICAL, isStringMode);
        }

        CheckerConfig config = new CheckerConfig(
                jmxClient,
                parser.getJmxUrl(),
                parser.getObjectName(),
                parser.getAttribute(),
                parser.getPath(),
                parser.getStatefile(),
                parser.getMeanRateInterval(),
                parser.getMinRateInterval(),
                parser.getRateWindowMultiplier(),
                warningThreshold,
                criticalThreshold,
                parser.getUom(),
                parser.getDivisor()
        );

        return switch (mode) {
            case "gauge" -> new GaugeChecker(config);
            case "rate" -> new RateChecker(config);
            case "string" -> new StringChecker(config);
            default -> throw new IllegalArgumentException("Unknown mode: " + mode);
        };
    }

    private static void configureLogging() {
        String logLevel = System.getProperty("io.github.kibruh.log.level", "SEVERE");
        Level level = Level.parse(logLevel.toUpperCase());

        Logger rootLogger = Logger.getLogger("");
        for (java.util.logging.Handler handler : rootLogger.getHandlers()) {
            if (handler instanceof ConsoleHandler) {
                handler.setLevel(level);
            }
        }
    }

    private static String fetchValue(CliParser parser, JmxClient jmxClient) {
        try {
            var objectName = new javax.management.ObjectName(parser.getObjectName());
            var valueOpt = jmxClient.getAttribute(objectName, parser.getAttribute(), parser.getPath());
            if (valueOpt.isEmpty()) {
                throw new IllegalStateException("Could not retrieve attribute: " + parser.getAttribute());
            }
            Object value = valueOpt.get();
            double divisor = parser.getDivisor();
            if (divisor != 1.0 && value instanceof Number number) {
                return NaemonOutput.formatNumber(number.doubleValue() / divisor);
            }
            return String.valueOf(value);
        } catch (Exception e) {
            throw new RuntimeException("Failed to fetch value: " + e.getMessage(), e);
        }
    }
}
