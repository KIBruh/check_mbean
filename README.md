# check_mbean

`check_mbean` is a high-performance Naemon/Icinga-compatible monitoring plugin written in Java 21. It allows for seamless monitoring of Managed Beans (MBeans) via JMX (Java Management Extensions), offering specialized modes for gauges, counters (rates), and string matching.

Built for modern infrastructure, it supports **GraalVM Native Image**, providing near-instant startup times and minimal memory footprint, making it ideal for high-frequency monitoring environments.

## Features

- **Standardized Output**: Fully compliant with Naemon/Nagios plugin API, including performance data.
- **Multiple Monitoring Modes**:
  - `gauge`: Numeric value comparison against standard threshold ranges.
  - `rate`: Stateful per-minute rate calculation for counters (handles resets automatically).
  - `string`: Regex-based pattern matching (supports negation).
  - `fetch`: Raw attribute retrieval for debugging or custom scripting.
- **Composite Data Support**: Drill down into complex JMX attributes using dot-notation (e.g., `HeapMemoryUsage.used`).
- **GraalVM Native Image**: Can be compiled to a standalone binary for zero-dependency deployment and ultra-fast execution.
- **Modern Java**: Leverages Java 21 features for robust and maintainable logic.

## Usage

```bash
check_mbean --url <JMX_URL> --object-name <NAME> --attribute <ATTR> [options]
```

### Examples

**Monitor Heap Memory (Gauge):**
```bash
# Alert if used heap memory exceeds 1GB/2GB
check_mbean --url localhost:9999 --object-name java.lang:type=Memory \
  --attribute HeapMemoryUsage --path used --warning 1073741824 --critical 2147483648 --uom B
```

**Monitor Process CPU Usage (Rate):**
```bash
# Calculate CPU time in seconds per minute (divisor converts nanos to seconds)
check_mbean --url localhost:9999 --object-name java.lang:type=OperatingSystem \
  --attribute ProcessCpuTime --mode rate --divisor 1000000000 --warning 45 --critical 55
```

**Check JVM Vendor (String):**
```bash
# Alert if the JVM vendor is NOT Oracle or Eclipse Foundation
check_mbean --url localhost:9999 --object-name java.lang:type=Runtime \
  --attribute VmVendor --mode string --critical "!Oracle|Eclipse"
```

## Installation

### Prerequisites
- Java 21 or higher
- Maven 3.8+ (for building)

### Build from Source
```bash
# Build the Shaded JAR
mvn clean package

# Build the GraalVM Native Binary (requires GraalVM installed)
mvn -Pnative package
```

The resulting artifacts will be in the `target/` directory.

## Configuration Options

| Option | Description | Default |
|--------|-------------|---------|
| `--url` | JMX Service URL or `host:port` shorthand | (Required) |
| `--object-name` | MBean ObjectName | (Required) |
| `--attribute` | MBean Attribute name | (Required) |
| `--path` | Nested path for CompositeData (e.g., `used`) | |
| `--mode` | `gauge`, `rate`, `string`, `fetch` | `gauge` |
| `--warning` | Warning threshold (range or regex) | |
| `--critical` | Critical threshold (range or regex) | |
| `--divisor` | Divide fetched value (e.g., 1024 for KB) | `1` |
| `--uom` | Unit of measure for performance data | |
| `--statefile` | Path to state file (Rate mode only) | `/tmp/*.state` |
| `--mean-rate-interval`| Mean rate window in seconds | `300` |
| `--min-rate-interval` | Minimum time between measurements | `60` |
| `--rate-window-multiplier`| Multiplier for state validity | `3` |
| `--username` | JMX authentication username | |
| `--password` | JMX authentication password | |
| `--timeout` | Connection timeout in seconds | `60` |

## License

This project is licensed under the MIT License - see the LICENSE file for details.
