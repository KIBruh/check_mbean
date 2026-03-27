package de.rbfh;

import org.apache.commons.cli.ParseException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CheckMBean {
    private static final Logger logger = LoggerFactory.getLogger(CheckMBean.class);

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
            logger.error("Unexpected error", e);
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

        return switch (mode) {
            case "gauge" -> new GaugeChecker(
                    jmxClient,
                    parser.getObjectName(),
                    parser.getAttribute(),
                    parser.getPath(),
                    warningThreshold,
                    criticalThreshold
            );
            case "rate" -> new RateChecker(
                    jmxClient,
                    parser.getObjectName(),
                    parser.getAttribute(),
                    parser.getPath(),
                    parser.getStatefile(),
                    parser.getMeanRateInterval(),
                    parser.getMinRateInterval(),
                    warningThreshold,
                    criticalThreshold
            );
            case "string" -> new StringChecker(
                    jmxClient,
                    parser.getObjectName(),
                    parser.getAttribute(),
                    parser.getPath(),
                    parser.getWarningThreshold(),
                    parser.getCriticalThreshold()
            );
            default -> throw new IllegalArgumentException("Unknown mode: " + mode);
        };
    }
}
