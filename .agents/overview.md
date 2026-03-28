# Project Overview: check_mbean

`check_mbean` is a specialized Naemon/Icinga plugin developed in Java 21 to
monitor Managed Beans (MBeans) exposed via Java Management Extensions (JMX).
It provides a robust, standalone solution for tracking health and performance
metrics of JVM-based applications.

## Key Features

- **JMX Remote Connectivity**: Securely connects to remote JVMs using standard
  JMX Service URLs. Supports both full URLs and shorthand `host:port` syntax.
- **Multiple Monitoring Modes**: Supports:
  - `gauge`: numeric value monitoring with thresholds
  - `rate`: counter-based rate calculation with stateful history
  - `string`: regex-based pattern matching with negation support
  - `fetch`: raw value retrieval (not a Naemon plugin)
- **Naemon Standard Thresholds**: Fully implements the Naemon threshold range
  specification, including inversion (`@`) and infinity (`~`).
- **Composite Data Support**: Allows drilling into complex `CompositeData`
  structures using a dot-separated path (e.g., `HeapMemoryUsage.used`).
- **Stateful Rate Monitoring**: Automatically handles counter resets and
  performs two-point measurements when historical data is unavailable.
- **Native Image Support**: Can be compiled into a standalone native binary
  using GraalVM for near-instant startup and minimal memory footprint.
- **Human-Readable Output**: All numeric values in status messages use max 3
  decimal places. Performance data uses full precision.
- **Performance Data Customization**: Supports `--uom` (unit of measure) and
  `--divisor` options for transforming values.
- **Configurable Logging**: Uses java.util.logging with configurable level via
  `-Dio.github.kibruh.log.level` system property (default: SEVERE).

## Core Architecture

The plugin follows a modular design for high maintainability:

- **`CheckMBean`**: The main entry point that orchestrates connection,
  execution, and Naemon-compliant output.
- **`CliParser`**: Handles command-line argument parsing using Apache Commons
  CLI, ensuring strict long-option-only parameters.
- **`JmxClient` & `DefaultJmxClient`**: Manage the JMX connection lifecycle and
  attribute reading logic.
- **`Checker` Interface**: A common interface for all monitoring modes
  (`GaugeChecker`, `RateChecker`, `StringChecker`).
- **`CheckerConfig`**: Record containing configuration shared across all checkers.
- **`Range` & `Threshold`**: Encapsulate the complex Naemon threshold parsing
  and evaluation logic (as Java records).
- **`NaemonOutput` & `NaemonStatus`**: Standardize the formatting of status
  messages and performance data.

## CLI Options

| Option | Description |
|--------|-------------|
| `--url` | JMX Service URL or shorthand `host:port` |
| `--object-name` | MBean object name (e.g., `java.lang:type=Memory`) |
| `--attribute` | MBean attribute name |
| `--path` | Path into CompositeData (optional) |
| `--mode` | Monitoring mode: `gauge`, `rate`, `string`, `fetch` (default: `gauge`) |
| `--uom` | Unit of measure for performance data (e.g., `B`, `KB`, `MB/s`) |
| `--divisor` | Divide value before threshold check and output |
| `--warning` | Warning threshold (range or regex depending on mode) |
| `--critical` | Critical threshold (range or regex depending on mode) |
| `--statefile` | Path to state file for rate mode |
| `--mean-rate-interval` | Mean rate interval in seconds (default: 300) |
| `--min-rate-interval` | Minimum interval between measurements (default: 60) |
| `--username` | JMX authentication username |
| `--password` | JMX authentication password |
| `--timeout` | Connection timeout in seconds (default: 60) |
| `--help` | Print help message |

## Documentation

Detailed implementation notes for each mode can be found in the following files:

- [Gauge Mode](gauge.md)
- [Rate Mode](rate.md)
- [String Mode](string.md)
