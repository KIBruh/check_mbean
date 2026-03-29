# GEMINI.md - check_mbean Project Context

This file serves as the primary instructional context for Gemini CLI interactions within the `check_mbean` project.

## Project Overview

`check_mbean` is a specialized Naemon/Icinga-compatible monitoring plugin written in Java 21. It allows for monitoring Managed Beans (MBeans) via Java Management Extensions (JMX). The project is designed for high performance and low overhead, with built-in support for GraalVM Native Image.

### Core Technologies
- **Language:** Java 21 (utilizing modern features like records, switch expressions, and pattern matching).
- **Build System:** Maven.
- **JMX Client:** Standard Java JMX API.
- **CLI Parsing:** Apache Commons CLI.
- **Testing:** JUnit 5 (Jupiter).
- **Native Compilation:** GraalVM Native Image for standalone binaries.

### Architecture
- **`CheckMBean`**: Entry point and orchestrator.
- **Monitoring Modes (`Checker` interface)**:
    - **`gauge`**: Monitors numeric values against thresholds.
    - **`rate`**: Calculates per-minute rates from counter attributes using stateful history.
    - **`string`**: Performs regex-based matching on string attributes.
    - **`fetch`**: Simple raw value retrieval (non-Nagios mode).
- **Threshold Engine**: Unified `Threshold` interface with support for numeric ranges (`NumericThreshold`) and regex patterns (`StringThreshold`).
- **JMX Layer**: Abstracted through `JmxClient` for testability.
- **Formatting**: Centralized `FormatUtils` for Naemon-compliant numeric output.

## Building and Running

### Prerequisites
- Java 21+
- Maven 3.8+

### Build Commands
```bash
# Build fat JAR with all dependencies
mvn clean package

# Run unit tests
mvn test

# Run full verification (including checkstyle)
mvn verify

# Build GraalVM Native Image (requires GraalVM)
mvn -Pnative package
```

### Running the Plugin
```bash
# Using JAR
java -jar target/check_mbean-1.0.0-SNAPSHOT.jar --url <URL> --object-name <NAME> --attribute <ATTR> [options]

# Using Native Binary (if built)
./target/check_mbean --url <URL> --object-name <NAME> --attribute <ATTR> [options]
```

## Development Conventions

### Coding Style
- **Java 21 Standards:** Use records for data holders and switch expressions where applicable.
- **Formatting:** 4-space indentation, 120-character maximum line length.
- **Logging:** Use `java.util.logging`. Log level is configurable via `-Dio.github.kibruh.log.level`.
- **Linting:** Strict Checkstyle rules are enforced (see `checkstyle.xml`).

### Testing Practices
- **JUnit 5:** Use for all tests.
- **Naming:** Follow `MethodName_StateUnderTest_ExpectedBehavior`.
- **Mocking:** Prefer manual test doubles or simple implementations over complex mocking frameworks when possible.

### Contribution Guidelines
- Always update or add tests for new features or bug fixes.
- Ensure `mvn verify` passes before committing.
- Do not use wildcard imports (except for static imports).

## Key Files & Directories
- `AGENTS.md`: High-level development guide for AI agents.
- `.agents/`: Detailed implementation notes for specific monitoring modes.
- `pom.xml`: Project dependencies and build configuration.
- `src/main/resources/native-image/`: GraalVM reachability metadata.
- `help.txt`: CLI help message template.

## Documentation References
- [Naemon Plugin API](https://www.naemon.org/documentation/usersguide/pluginapi.html)
- [Nagios Threshold Syntax](https://nagios-plugins.org/doc/guidelines.html#THRESHOLDFORMAT)
