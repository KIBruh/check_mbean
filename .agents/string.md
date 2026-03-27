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
3. **Status Evaluation**:
   - If the `--critical` regex matches, the status is `CRITICAL`.
   - If only the `--warning` regex matches, the status is `WARNING`.
   - If neither regex matches (or no thresholds are provided), the status is
     `OK`.
4. **Formatting**: The status message includes the attribute's actual value in
   single quotes for clarity.

## Example

If you monitor a `Status` attribute and set `--critical "^ERROR"`, any value
starting with "ERROR" will trigger a critical alert.
