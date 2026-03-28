# String Mode Implementation

The `string` mode is used for monitoring non-numeric JMX attributes, such as
status strings, version information, or configuration flags.

## Implementation Details: `StringChecker`

The `StringChecker` class performs the monitoring logic:

1. **Attribute Retrieval**: It fetches the attribute value as a `String` (using
   `String.valueOf()` for non-string objects).
2. **Regex Matching**:
   - In this mode, `--warning` and `--critical` arguments are treated as
     **regular expressions**, not numeric ranges.
   - The plugin uses `java.util.regex.Pattern` to check if the current value
     *contains* a match for the provided regex.
3. **Negation Support**:
   - If the regex starts with `!` or `~`, the match is negated.
   - `--critical ~error` or `--critical !error` triggers CRITICAL if the value
     does NOT match the regex.
4. **Status Evaluation**:
   - If the `--critical` regex matches (or doesn't match, if negated), the
     status is `CRITICAL`.
   - If only the `--warning` regex matches (or doesn't match, if negated), the
     status is `WARNING`.
   - If neither regex matches (or both match when they shouldn't), the status is
     `OK`.
5. **Performance Data**: Only execution time is included (no value in perf data).

## Example

```bash
# Alert if status contains ERROR
check_mbean --url localhost:9999 --object-name com.example:type=Server \
  --attribute State --mode string --critical "ERROR"

# Alert if status does NOT contain OK
check_mbean --url localhost:9999 --object-name com.example:type=Server \
  --attribute State --mode string --critical "~OK"

# Warning if contains WARNING, critical if contains ERROR
check_mbean --url localhost:9999 --object-name com.example:type=Server \
  --attribute State --mode string --warning "WARNING" --critical "ERROR"
```

Output:
```
CRITICAL - Value is 'ERROR: Service failed' | 'time'=0.015s;;;
OK - Value is 'All good' | 'time'=0.012s;;;
```
