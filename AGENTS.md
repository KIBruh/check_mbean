# AGENTS.md - check_mbean Development Guide

This file provides guidance for AI agents working on the check_mbean project.

## Project Overview

`check_mbean` is a Naemon/Icinga plugin developed in Java 21 to monitor Managed Beans (MBeans) exposed via Java Management Extensions (JMX).

## Build Commands

### Prerequisites
- Java 21
- Maven 3.8+ or Gradle 8+

### Build
```bash
# Maven
mvn clean package

# Gradle
./gradlew build
```

### Run Single Test
```bash
# Maven
mvn test -Dtest=TestClassName#testMethodName

# Gradle
./gradlew test --tests "com.example.TestClassName.testMethodName"
```

### Lint
```bash
# Maven (checkstyle)
mvn checkstyle:check

# Gradle (checkstyle)
./gradlew checkstyleMain checkstyleTest
```

### Full Build with Tests
```bash
mvn verify
# or
./gradlew check
```

## Code Style Guidelines

### General
- Java 21 source level
- Use modern Java features (records, switch expressions, pattern matching)
- No trailing whitespace
- 4-space indentation (no tabs)
- Max line length: 120 characters

### Imports
- Use fully qualified class names where appropriate to avoid ambiguity
- Group imports: static, java., javax., external., internal.
- No wildcard imports except for static imports

### Naming Conventions
- Classes: PascalCase (e.g., `CheckMBean`, `JmxClient`)
- Methods/variables: camelCase (e.g., `getAttribute`, `jmxUrl`)
- Constants: UPPER_SNAKE_CASE (e.g., `DEFAULT_TIMEOUT`)
- Packages: lowercase (e.g., `de.rbfh`)

### Types
- Use primitives where performance matters
- Use `List<T>` over arrays for collections
- Use `Optional<T>` for nullable return values
- Avoid raw types (use `List<String>` not `List`)

### Error Handling
- Use checked exceptions for recoverable errors
- Use unchecked exceptions for programming errors
- Never swallow exceptions without logging
- Always provide meaningful error messages
- Throw specific exceptions (e.g., `IllegalArgumentException` not `RuntimeException`)

### Logging
- Use a logger (SLF4J)
- Log at appropriate levels (ERROR for failures, DEBUG for development info)
- Never log sensitive data (passwords, JMX credentials)

### Documentation
- Javadoc for public APIs
- Include `@param`, `@return`, `@throws` tags
- Keep implementation comments brief; prefer self-documenting code

### Testing
- Use JUnit 5 (Jupiter)
- Test naming: `MethodName_StateUnderTest_ExpectedBehavior`
- One assertion per test when possible
- Use @ParameterizedTest for multiple input scenarios

## Architecture

### Core Components
- **`CheckMBean`**: Main entry point, orchestrates connection and execution
- **`CliParser`**: Command-line argument parsing (Apache Commons CLI, long-options only)
- **`JmxClient`**: JMX connection lifecycle and attribute reading
- **`Checker` interface**: Common interface for monitoring modes
  - `GaugeChecker`: Numeric comparisons
  - `RateChecker`: Counter-based rate calculation
  - `StringChecker`: Regex-based string matching
- **`Range` & `Threshold`**: Naemon threshold parsing and evaluation
- **`NaemonOutput`**: Output formatting (status messages, performance data)

### Output Requirements
- All numeric values must use plain decimal notation (no scientific notation)
- Example: `104012344` not `1.04E8`
- Follow Naemon plugin performance data format

### Threshold Specification
- Format: `[@]start:end`
- `@`: invert range (alert if inside)
- `~`: infinity
- Default: alert if outside range

### Rate Mode
- Uses state file for historical measurements
- Calculates rate per minute
- Handles counter resets
- Two-point measurement when historical data unavailable
- Purge measurements older than 3 * mean-rate-interval

## Existing Documentation

See `.agents/` directory for detailed implementation notes:
- `.agents/overview.md` - Project overview
- `.agents/gauge.md` - Gauge mode implementation
- `.agents/rate.md` - Rate mode implementation
- `.agents/string.md` - String mode implementation
