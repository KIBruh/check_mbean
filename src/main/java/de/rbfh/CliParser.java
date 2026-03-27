package de.rbfh;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Objects;

public class CliParser {
    private static final Logger logger = LoggerFactory.getLogger(CliParser.class);

    private final Options options;
    private final CommandLine cmd;
    private final String[] args;

    public CliParser(String[] args) throws ParseException {
        this.args = Objects.requireNonNull(args, "args cannot be null");
        this.options = createOptions();
        this.cmd = new DefaultParser().parse(options, args);
    }

    private Options createOptions() {
        Options opts = new Options();

        opts.addOption(Option.builder("h")
                .longOpt("help")
                .desc("Print this help message")
                .build());

        opts.addOption(Option.builder()
                .longOpt("url")
                .hasArg()
                .argName("URL")
                .desc("JMX Service URL (e.g., service:jmx:rmi:///jndi/rmi://host:port/jmxrmi)")
                .build());

        opts.addOption(Option.builder()
                .longOpt("object-name")
                .hasArg()
                .argName("NAME")
                .desc("MBean object name (e.g., java.lang:type=Memory)")
                .build());

        opts.addOption(Option.builder()
                .longOpt("attribute")
                .hasArg()
                .argName("NAME")
                .desc("MBean attribute name (e.g., HeapMemoryUsage)")
                .build());

        opts.addOption(Option.builder()
                .longOpt("path")
                .hasArg()
                .argName("PATH")
                .desc("Path to nested attribute in CompositeData (e.g., used)")
                .build());

        opts.addOption(Option.builder()
                .longOpt("mode")
                .hasArg()
                .argName("MODE")
                .desc("Monitoring mode: gauge, rate, string (default: gauge)")
                .build());

        opts.addOption(Option.builder()
                .longOpt("warning")
                .hasArg()
                .argName("THRESHOLD")
                .desc("Warning threshold (range or regex depending on mode)")
                .build());

        opts.addOption(Option.builder()
                .longOpt("critical")
                .hasArg()
                .argName("THRESHOLD")
                .desc("Critical threshold (range or regex depending on mode)")
                .build());

        opts.addOption(Option.builder()
                .longOpt("username")
                .hasArg()
                .argName("NAME")
                .desc("JMX authentication username")
                .build());

        opts.addOption(Option.builder()
                .longOpt("password")
                .hasArg()
                .argName("PASSWORD")
                .desc("JMX authentication password")
                .build());

        opts.addOption(Option.builder()
                .longOpt("timeout")
                .hasArg()
                .argName("SECONDS")
                .desc("Connection timeout in seconds (default: 60)")
                .build());

        opts.addOption(Option.builder()
                .longOpt("statefile")
                .hasArg()
                .argName("FILE")
                .desc("Path to state file for rate mode")
                .build());

        opts.addOption(Option.builder()
                .longOpt("mean-rate-interval")
                .hasArg()
                .argName("SECONDS")
                .desc("Mean rate interval in seconds for rate mode (default: 300)")
                .build());

        opts.addOption(Option.builder()
                .longOpt("min-rate-interval")
                .hasArg()
                .argName("SECONDS")
                .desc("Minimum interval between measurements for rate mode (default: 60)")
                .build());

        return opts;
    }

    public void printHelp() {
        HelpFormatter formatter = new HelpFormatter();
        formatter.printHelp("check_mbean", options);

        System.out.println();
        System.out.println("Examples:");
        System.out.println("  # Gauge mode - monitor heap memory usage");
        System.out.println("  check_mbean --url service:jmx:rmi:///jndi/rmi://host:9999/jmxrmi \\");
        System.out.println("    --object-name java.lang:type=Memory --attribute HeapMemoryUsage \\");
        System.out.println("    --path used --warning 100000000 --critical 200000000");
        System.out.println();
        System.out.println("  # Rate mode - monitor request counter rate");
        System.out.println("  check_mbean --url service:jmx:rmi:///jndi/rmi://host:9999/jmxrmi \\");
        System.out.println("    --object-name com.example:type=Requests --attribute TotalCount \\");
        System.out.println("    --mode rate --warning 1000 --critical 2000");
        System.out.println();
        System.out.println("  # String mode - check server status");
        System.out.println("  check_mbean --url service:jmx:rmi:///jndi/rmi://host:9999/jmxrmi \\");
        System.out.println("    --object-name com.example:type=Server --attribute State \\");
        System.out.println("    --mode string --critical \"^DOWN$\"");
    }

    public boolean hasHelp() {
        return cmd.hasOption("help");
    }

    public String getJmxUrl() {
        return cmd.getOptionValue("url");
    }

    public String getObjectName() {
        return cmd.getOptionValue("object-name");
    }

    public String getAttribute() {
        return cmd.getOptionValue("attribute");
    }

    public String getPath() {
        return cmd.getOptionValue("path");
    }

    public String getMode() {
        return cmd.getOptionValue("mode", "gauge");
    }

    public String getWarningThreshold() {
        return cmd.getOptionValue("warning");
    }

    public String getCriticalThreshold() {
        return cmd.getOptionValue("critical");
    }

    public String getUsername() {
        return cmd.getOptionValue("username");
    }

    public String getPassword() {
        return cmd.getOptionValue("password");
    }

    public int getTimeout() {
        return Integer.parseInt(cmd.getOptionValue("timeout", "60"));
    }

    public String getStatefile() {
        return cmd.getOptionValue("statefile");
    }

    public int getMeanRateInterval() {
        return Integer.parseInt(cmd.getOptionValue("mean-rate-interval", "300"));
    }

    public int getMinRateInterval() {
        return Integer.parseInt(cmd.getOptionValue("min-rate-interval", "60"));
    }

    public String[] getArgs() {
        return args;
    }

    public void validateRequired() {
        if (getJmxUrl() == null) {
            throw new IllegalStateException("Missing required option: --url");
        }
        if (getObjectName() == null) {
            throw new IllegalStateException("Missing required option: --object-name");
        }
        if (getAttribute() == null) {
            throw new IllegalStateException("Missing required option: --attribute");
        }
    }
}
