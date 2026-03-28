# Rate Mode Implementation

The `rate` mode is used for monitoring JMX attributes that are counters (e.g.,
total requests, number of errors). It calculates the rate of change over time,
typically expressed in "units per minute."

## Implementation Details: `RateChecker`

The `RateChecker` implements complex logic for rate calculation:

1. **State Management**:
   - Uses a state file (either auto-generated in `/tmp` or specified via
     `--statefile`) to store timestamped measurements.
   - Auto-generated statefiles follow the pattern:
     `check_mbean_<hash>_<host>_<port>_<objectname>_<attribute>.state`
   - Measurements are stored as `timestamp value` pairs in plain decimal format.
   - All measurements within the time window are saved (not just two).
   - Old data is purged: measurements older than 
     `(rateWindowMultiplier + 2) * mean-rate-interval` are removed.
   - If the last measurement age exceeds `rateWindowMultiplier * mean-rate-interval`,
     the state is considered invalid and fresh measurements are taken.

2. **Rate Calculation**:
   - **Counter Reset Detection**: If the current counter value is smaller than
     the last stored measurement, the state is cleared and ignored.
   - **Two-Point Measurement**: If no valid state is available, the plugin makes
     two measurements separated by at least `--min-rate-interval` seconds. This
     ensures an accurate initial rate.
   - **Historical Data Usage**: If valid state exists, the plugin finds the
     measurement closest to `now - mean-rate-interval`, provided the rate window
     doesn't exceed `rateWindowMultiplier * mean-rate-interval`.
   - The statefile saves raw values (before divisor is applied), so changing
     `--divisor` does not invalidate the statefile.

3. **Per-Minute Calculation**:
   - The rate is always calculated per minute:
     `rate = (deltaValue / deltaTimeSeconds) * 60`.
   - If `--divisor` is specified, the rate is divided before threshold check
     and in output.

4. **Performance Data**:
   - Includes the current absolute counter value (`c` unit), calculated rate,
     rate window in seconds, and execution time.
   - Rate window is labeled as `check_rate_window` with `s` unit.
   - If `--uom` is specified, uses that unit instead of `/min` for the rate.
   - Human-readable output shows max 3 decimal places; performance data uses
     full precision.
   - Thresholds are applied to the *calculated rate*, not the absolute counter.

## Example

```bash
# Basic rate monitoring
check_mbean --url localhost:9999 --object-name com.example:type=Requests \
  --attribute TotalCount --mode rate --warning 1000 --critical 2000 \
  --min-rate-interval 5 --mean-rate-interval 15

# With unit and divisor
check_mbean --url localhost:9999 --object-name com.example:type=Requests \
  --attribute TotalCount --mode rate --uom MB/s --divisor 1048576 \
  --warning 10 --critical 20 --min-rate-interval 5 --mean-rate-interval 15

# With custom rate window multiplier
check_mbean --url localhost:9999 --object-name com.example:type=Requests \
  --attribute TotalCount --mode rate --rate-window-multiplier 2 \
  --warning 1000 --critical 2000
```

Output:
```
OK - Value has a rate of 1.5 MB/s (rate window 15s) | 'Value'=1234567c;;;; 'Value.rate'=1.5;0:10;0:20;;MB/s 'check_rate_window'=15s;;;; 'time'=0.015s;;;
```
