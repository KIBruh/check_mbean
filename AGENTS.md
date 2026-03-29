# AGENTS.md - check_mbean Development Guide

This file provides guidance for AI agents working on the check_mbean project.

## Project Overview

`check_mbean` is a Naemon/Icinga plugin developed in Java 21 to monitor Managed Beans (MBeans) exposed via Java Management Extensions (JMX).

## Build & Test Commands

This project uses Maven 3.8+ and Java 21.

### Primary Build (includes Checkstyle & Tests)
```bash
mvn verify
```

### Development Build (fast)
```bash
mvn clean package -DskipTests
```

### Run Specific Test
```bash
# Run a single test class
mvn test -Dtest=CheckerTest

# Run a single test method
mvn test -Dtest=CheckerTest#testGaugeChecker
```

### Linting
```bash
mvn checkstyle:check
```

## Code Style Guidelines

Adherence to these rules is enforced via Checkstyle (`checkstyle.xml`).

### General Formatting
- **Source Level:** Java 21 (Records, Switch Expressions, Pattern Matching)
- **Indentation:** 4 spaces (no tabs)
- **Line Length:** Maximum 120 characters
- **Braces:** Left curly on same line, right curly on its own line
- **Whitespace:** One statement per line; multiple variable declarations are forbidden

### Naming Conventions
- **Packages:** lowercase (e.g., `io.github.kibruh`), regex: `^[a-z]+(\.[a-z][a-z0-9]*)*$`
- **Classes/Interfaces:** PascalCase (e.g., `CheckMBean`, `JmxClient`)
- **Methods:** camelCase, may include underscores in tests (e.g., `test_Condition_Expected`)
- **Variables/Members:** camelCase, at least 2 characters (regex: `^[a-z][a-z0-9][a-zA-Z0-9]*$`)
- **Constants:** UPPER_SNAKE_CASE (e.g., `DEFAULT_TIMEOUT`)

### Imports
- **No Wildcards:** Do not use star imports (e.g., `import java.util.*`)
- **Grouping:** static first, then java.*, then others (alphabetic order within groups)

### Types & Error Handling
- **Records:** Use for immutable data (e.g., `CheckerConfig`, `Range`, `NumericThreshold`, `StringThreshold`)
- **Optional:** Use `Optional<T>` for return types that may be empty; avoid returning nulls
- **Exceptions:** Use checked exceptions for recoverable errors; unchecked for programming errors
- **Logging:** Use `java.util.logging`. Log level defaults to `SEVERE`

### Testing
- **Framework:** JUnit 5 (Jupiter)
- **Convention:** One assertion per test when possible; use `@ParameterizedTest` for scenarios
- **Naming:** `MethodName_State_ExpectedResult` (e.g., `evaluate_ValueOutsideRange_ReturnsCritical`)

## Architecture & Logic

### Core Components
- **`CheckMBean`**: Entry point. Parses CLI and initiates the check
- **`CliParser`**: Handles Apache Commons CLI (long-options only)
- **`JmxClient`**: Manages MBeanServerConnection lifecycle
- **`Checker` Interface**: Implemented by `GaugeChecker`, `RateChecker`, and `StringChecker`
- **`Threshold` Interface**: Unified threshold evaluation, implemented by `NumericThreshold` (range-based) and `StringThreshold` (regex-based)
- **`NaemonOutput`**: Formats status messages and performance data

### Performance Data & Numeric Formatting
- **Naemon Protocol:** Must follow `label=value[uom];[warn];[crit];[min];[max]`
- **Full Precision:** Use plain decimal notation for performance data (no scientific notation like `1.04E8`)
- **Human Readable:** Limit to 3 decimal places in the status message (e.g., `10.123`)

### Rate Calculation Logic
- **Statefulness:** Uses a state file to store historical measurements
- **Calculation:** `(currentValue - previousValue) / timeDeltaMinutes`
- **Resets:** If `currentValue < previousValue`, the counter is assumed to have reset; clear state and return UNKNOWN or calculate from zero if appropriate
- **Window:** State is valid if `age <= multiplier * interval`. Purge data older than `(multiplier + 2) * interval`

## Documentation

See the `.agents/` directory for implementation details:
- `.agents/gauge.md` - Numeric comparisons and UOMs
- `.agents/rate.md` - Stateful rate calculation and counter resets
- `.agents/string.md` - Regex matching and negation support
- `.agents/overview.md` - High-level system design
