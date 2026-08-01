# Automated Validation Evidence

Date: 2026-08-01

Host: Windows 11, x86_64

Toolchain: JetBrains Runtime 21, Gradle 9.5.0, Kotlin 2.2.20

## Release command

```powershell
.\gradlew.bat check verifyPluginProjectConfiguration verifyPluginStructure buildPlugin --no-daemon --no-configuration-cache --console=plain
```

Result: **passed** — 15 suites, 41 tests, 0 failures, 0 errors, 0 skipped. The suite covers deterministic easing and timelines, scheduler start/stop, zero-duration fallback, global and per-surface capacity, callback isolation, adaptive degradation, settings normalization, motion policy, editor-event classification, EDT-safe startup and project-expiration behavior, contrast, diagnostics privacy, plugin identity, forbidden architecture mechanisms, documentation links, requirement traceability, release-version consistency, and brand assets.

Packaged artifact:

- file: `MotionFlow-0.1.1.zip`
- size: `201045` bytes
- SHA-256: `77547D9FEE88EBCC00EF99E1FC253AB866AE1725A28D5BEDFD7FCAE11D73BCF1`

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

`runIde` launched a clean IntelliJ IDEA 2025.3.6 sandbox with `io.github.myangprint.motionflow` listed as a required plugin. The sandbox log records MotionFlow `0.1.1` as a loaded custom plugin and contains no MotionFlow startup-thread exception or plugin error for that launch.

The user then installed `0.1.1` into IntelliJ IDEA 2025.3.1 and confirmed that the live caret trail was visible. This proves live editor effects started; it is not a substitute for the remaining full visual, accessibility, unload, and resource matrix.

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

## Hosted validation

[GitHub Actions run 30651018811](https://github.com/Myang-print/motionflow/actions/runs/30651018811) passed from commit `7427c3d`:

- Ubuntu package, structure, and test gate: 3m17s;
- macOS test and configuration gate: 4m38s;
- Windows test and configuration gate: 14m43s;
- two-baseline Plugin Verifier gate: 7m42s.

The workflow artifact is retained by GitHub Actions as `motionflow-plugin`; local artifact hashes above remain the reproducible evidence for the exact file stored in this workspace.
