# Rate Mode Implementation

The `rate` mode is used for monitoring JMX attributes that are counters (e.g.,
total requests, number of errors). It calculates the rate of change over time,
typically expressed in "units per minute."

## Implementation Details: `RateChecker`

The `RateChecker` implements complex logic based on the `ratemode.txt`
specification:

1. **State Management**:
   - It uses a state file (either auto-generated in `/tmp` or specified via
     `--statefile`) to store timestamped measurements.
   - Measurements are stored as `timestamp value` pairs.
   - Old data is purged: only measurements newer than `3 * mean-rate-interval`
     are kept.
2. **Rate Calculation**:
   - **Counter Reset Detection**: If the current counter value is smaller than
     the last stored measurement, the state is ignored, and a new two-point
     measurement is triggered.
   - **Two-Point Measurement**: If no valid state is available, the plugin makes
     two measurements separated by at least `--min-rate-interval` seconds. This
     ensures an accurate initial rate.
   - **Historical Data Usage**: If valid state exists, the plugin finds the
     measurement closest to `now - mean-rate-interval`, provided it's not older
     than `2 * mean-rate-interval`.
3. **Per-Minute Calculation**:
   - The rate is always calculated per minute:
     `rate = (deltaValue / deltaTimeSeconds) * 60`.
4. **Performance Data**:
   - Includes both the current absolute counter value (`c` unit) and the
     calculated rate (`/min` unit).
   - All numeric values are formatted to avoid scientific notation (e.g.,
     `104012344` instead of `1.04E8`).
   - Thresholds are applied to the *calculated rate*, not the absolute counter.

## Key Logic

- If state file is used, rate window is shown in the output (e.g.,
  `rate window 305s`).
- If state file is ignored, the plugin pauses for `--min-rate-interval` seconds
  to get a valid rate.
