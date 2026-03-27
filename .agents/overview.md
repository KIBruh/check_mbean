# Project Overview: check_mbean

`check_mbean` is a specialized Naemon/Icinga plugin developed in Java 21 to
monitor Managed Beans (MBeans) exposed via Java Management Extensions (JMX).
It provides a robust, standalone solution for tracking health and performance
metrics of JVM-based applications.

## Key Features

- **JMX Remote Connectivity**: Securely connects to remote JVMs using standard
  JMX Service URLs.
- **Multiple Monitoring Modes**: Supports `gauge` (numeric comparisons),
  `rate` (counter-based rate calculation), and `string` (regex-based matching).
- **Naemon Standard Thresholds**: Fully implements the Naemon threshold range
  specification, including inversion (`@`) and infinity (`~`).
- **Composite Data Support**: Allows drilling into complex `CompositeData`
  structures using a dot-separated path (e.g., `HeapMemoryUsage.used`).
- **Stateful Rate Monitoring**: Automatically handles counter resets and
  performs two-point measurements when historical data is unavailable.
- **Native Image Support**: Can be compiled into a standalone native binary
  using GraalVM for near-instant startup and minimal memory footprint.
- **Human-Readable Output**: Ensures all numeric values (both in status messages
  and performance data) are formatted using plain decimal notation, avoiding
  confusing scientific notation (e.g., `104012344` instead of `1.04E8`).

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
- **`Range` & `Threshold`**: Encapsulate the complex Naemon threshold parsing
  and evaluation logic.
- **`NaemonOutput` & `NaemonStatus`**: Standardize the formatting of status
  messages and performance data, including a specialized formatter to prevent
  scientific notation in numeric outputs.

## Documentation

Detailed implementation notes for each mode can be found in the following files:

- [Gauge Mode](gauge.md)
- [Rate Mode](rate.md)
- [String Mode](string.md)
