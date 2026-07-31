# Automated Validation Evidence

Date: 2026-08-01

Host: Windows 11, x86_64

Toolchain: JetBrains Runtime 21, Gradle 9.5.0, Kotlin 2.2.20

## Release command

```powershell
.\gradlew.bat check verifyPluginProjectConfiguration verifyPluginStructure buildPlugin --no-daemon --no-configuration-cache --console=plain
```

Result: **passed** — 14 suites, 38 tests, 0 failures, 0 errors, 0 skipped. The suite covers deterministic easing and timelines, scheduler start/stop, zero-duration fallback, global and per-surface capacity, callback isolation, adaptive degradation, settings normalization, motion policy, editor-event classification, contrast, diagnostics privacy, plugin identity, forbidden architecture mechanisms, documentation links, requirement traceability, release-version consistency, and brand assets.

Packaged artifact:

- file: `MotionFlow-0.1.0.zip`
- size: `199348` bytes
- SHA-256: `CD3700D5204886093A4D0F8AC6D7E2AB44B274E5EC7A8F9B336DC003C649D92F`

## Plugin Verifier

```powershell
.\gradlew.bat verifyPlugin --no-daemon --no-configuration-cache --console=plain
```

Result: **passed** against both resolved IDEs:

| IDE | Build | Verdict | Dynamic-plugin eligibility |
| --- | --- | --- | --- |
| IntelliJ IDEA Ultimate 2025.3.6 | `IU-253.33813.25` | Compatible | Probably enabled/disabled without restart |
| IntelliJ IDEA Ultimate 2026.2 | `IU-262.9437.65` | Compatible | Probably enabled/disabled without restart |

Plugin Verifier reported no binary incompatibility, internal API, removed API, or invalid plugin descriptor findings attributable to MotionFlow. Distribution-layout warnings emitted while scanning the IDE installations are upstream IDE-distribution warnings, not MotionFlow compatibility findings.

## Sandbox startup

`runIde` launched a clean IntelliJ IDEA 2025.3.6 sandbox with `io.github.myangprint.motionflow` listed as a required plugin. The sandbox log records MotionFlow as a loaded custom plugin and contains no MotionFlow `PluginException` or error entry for that launch.

The Windows desktop was locked during the evidence run, so interactive screenshot-based effect inspection could not be completed in this run. This limitation is recorded rather than converted into a false pass.

## Static architecture checks

`SourceArchitectureTest` scans stable source for:

- IntelliJ implementation-package imports;
- internal glass-pane helpers;
- reflection and JDK internals;
- network APIs;
- process-wide AWT hooks;
- Java agents or instrumentation;
- third-party runtime implementation dependencies;
- more than one Swing timer implementation.

All checks passed.
