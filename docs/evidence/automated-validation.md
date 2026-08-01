# Automated Validation Evidence

Date: 2026-08-01

Host: Windows 11, x86_64

Toolchain: JetBrains Runtime 21, Gradle 9.5.0, Kotlin 2.2.20

## Release command

```powershell
.\gradlew.bat check verifyPluginProjectConfiguration verifyPluginStructure buildPlugin --no-daemon --no-configuration-cache --console=plain
```

Result: **passed** — 18 suites, 57 tests, 0 failures, 0 errors, 0 skipped. The suite covers deterministic easing and timelines, scheduler start/stop, zero-duration fallback, global and per-surface capacity, callback isolation, adaptive degradation, schema-v3 style migration, motion policy, editor-event classification, EDT-safe startup and project-expiration behavior, exact previous-caret origin replacement, bounded frame sampling, travel/fade phases, all cardinal/diagonal ribbon geometry, rendered gradient/core visibility, focus re-anchor architecture, contrast, diagnostics privacy, plugin identity, forbidden mechanisms, documentation integrity, release-version consistency, and brand assets.

Packaged artifact:

- file: `MotionFlow-0.2.2.zip`
- size: `243421` bytes
- SHA-256: `572994B30D31F159F58CC6E1583392F4EF9524B23D49525C3A97C578A55445E4`

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

`runIde` previously launched a clean IntelliJ IDEA 2025.3.6 sandbox with `io.github.myangprint.motionflow` listed as a required plugin. The sandbox log recorded MotionFlow `0.1.1` as a loaded custom plugin and contained no MotionFlow startup-thread exception or plugin error for that launch.

The user installed the local `0.2.0` and `0.2.1` packages into IntelliJ IDEA. They confirmed the live caret-height path and then identified the stiff endpoint motion, low visibility, and stale/competing origin defects addressed by `0.2.2`. The exact 0.2.2 silky appearance still requires installation of the packaged ZIP and is not represented as an already completed manual observation.

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

[GitHub Actions run 30683585001](https://github.com/Myang-print/motionflow/actions/runs/30683585001) passed for the preceding 0.2.1 branch head `e2498fd`:

- Ubuntu package, structure, and test gate: 3m30s;
- macOS test and configuration gate: 1m26s;
- Windows test and configuration gate: 9m45s;
- two-baseline Plugin Verifier gate: 7m30s.

That run proves the branch workflow before the 0.2.2 refinement. Local gates and the artifact hash above are the exact evidence for 0.2.2; hosted 0.2.2 validation reruns after push.
