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
3. **Threshold Comparison**: The value is compared against the parsed warning
   and critical threshold ranges:
   - **Critical check first**: If a critical threshold is provided and the
     value is outside its range (or inside if `@` is used), the status becomes
     `CRITICAL`.
   - **Warning check second**: If the status is not `CRITICAL` and a warning
     threshold is provided, the value is checked against it. If it fails, the
     status becomes `WARNING`.
   - **Default**: If neither threshold is breached, the status remains `OK`.
4. **Formatting**: The value is converted to a plain decimal string using
   `NaemonOutput.formatNumber`, ensuring large numbers do not appear in
   scientific notation (e.g., `104012344` instead of `1.04E8`).
5. **Performance Data**: The numeric value is included in the Naemon output as
   performance data, also formatted to avoid scientific notation, and includes
   any relevant thresholds.

## Example

If `--warning 80` and `--critical 90` are set, and the current value is `85`,
the `GaugeChecker` will return a `WARNING` status.
