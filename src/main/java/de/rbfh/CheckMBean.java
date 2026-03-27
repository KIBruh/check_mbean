package de.rbfh;

import org.apache.commons.cli.ParseException;
import java.util.logging.Level;
import java.util.logging.Logger;

public class CheckMBean {
    private static final Logger logger = Logger.getLogger(CheckMBean.class.getName());

    public static void main(String[] args) {
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
                Checker checker = createChecker(parser, jmxClient);
                NaemonOutput output = checker.check();
                System.out.println(output.build());
                System.exit(output.getStatus().getExitCode());
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

        Threshold warningThreshold = null;
        Threshold criticalThreshold = null;

        if (parser.getWarningThreshold() != null) {
            warningThreshold = Threshold.parse(parser.getWarningThreshold(), NaemonStatus.WARNING);
        }
        if (parser.getCriticalThreshold() != null) {
            criticalThreshold = Threshold.parse(parser.getCriticalThreshold(), NaemonStatus.CRITICAL);
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
                warningThreshold,
                criticalThreshold,
                parser.getWarningThreshold(),
                parser.getCriticalThreshold()
        );

        return switch (mode) {
            case "gauge" -> new GaugeChecker(config);
            case "rate" -> new RateChecker(config);
            case "string" -> new StringChecker(config);
            default -> throw new IllegalArgumentException("Unknown mode: " + mode);
        };
    }
}
