# Gauge Mode Implementation

The `gauge` mode is the default monitoring mode for `check_mbean`. It is used for
monitoring numeric values that can fluctuate up and down, such as memory usage,
CPU load, or thread counts.

## Implementation Details: `GaugeChecker`

The `GaugeChecker` class implements the logic for this mode:

1. **Attribute Retrieval**: It uses the `JmxClient` to fetch the value of a
   specified JMX attribute. If a `--path` is provided, it drills into
   `CompositeData` structures to retrieve nested values.
2. **Type Validation**: The retrieved value must be an instance of
   `java.lang.Number`. If the value is not numeric, the checker throws an
   `IllegalArgumentException`, which is caught and reported as `UNKNOWN` status.
3. **Divisor Application**: If `--divisor` is specified, the value is divided
   before threshold comparison and in output.
4. **Threshold Comparison**: The (possibly divided) value is compared against the
   parsed warning and critical threshold ranges:
   - **Critical check first**: If a critical threshold is provided and the
     value is outside its range (or inside if `@` is used), the status becomes
     `CRITICAL`.
   - **Warning check second**: If the status is not `CRITICAL` and a warning
     threshold is provided, the value is checked against it. If it fails, the
     status becomes `WARNING`.
   - **Default**: If neither threshold is breached, the status remains `OK`.
5. **Formatting**:
   - Human-readable output: Uses `NaemonOutput.formatNumberForHuman` which shows
     max 3 decimal places (e.g., `123.456`).
   - Performance data: Uses full precision via `NaemonOutput.formatNumber`.
6. **Performance Data**: Includes the numeric value (with optional `--uom` unit),
   warning/critical thresholds, and execution time. The `--uom` option adds a
   unit string to both the message and performance data.

## Example

```bash
# Basic gauge monitoring
check_mbean --url localhost:9999 --object-name java.lang:type=Memory \
  --attribute HeapMemoryUsage --path used --warning 80 --critical 90

# With unit of measure and divisor (value in bytes, display in MB)
check_mbean --url localhost:9999 --object-name java.lang:type=Memory \
  --attribute HeapMemoryUsage --path used --uom MB --divisor 1048576 \
  --warning 80 --critical 90
```

Output:
```
OK - Value is 256 MB | 'Value'=256;0:80;0:90;;MB 'time'=0.015s;;;
```
