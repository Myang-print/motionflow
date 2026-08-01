# Automated Validation Evidence

Date: 2026-08-01

Host: Windows 11, x86_64

Toolchain: JetBrains Runtime 21, Gradle 9.5.0, Kotlin 2.2.20

## Release command

```powershell
.\gradlew.bat check verifyPluginProjectConfiguration verifyPluginStructure buildPlugin --no-daemon --no-configuration-cache --console=plain
```

Result: **passed** — 19 suites, 66 tests, 0 failures, 0 errors, 0 skipped. The suite covers deterministic easing and timelines, scheduler start/stop, zero-duration fallback, global and per-surface capacity, callback isolation, adaptive degradation, schema-v4 style/coverage/visibility migration, motion policy, editor-event classification, EDT-safe startup and project-expiration behavior, per-caret exact-origin replacement, bounded primary-first eight-caret admission, caret add/remove architecture, frame sampling, travel/fade phases, projected horizontal/vertical/diagonal geometry, rendered gradient/core/vertical-width visibility, focus re-anchor architecture, contrast, diagnostics privacy, plugin identity, forbidden mechanisms, documentation integrity, release-version consistency, and brand assets.

Packaged artifact:

- file: `MotionFlow-0.2.3.zip`
- size: `259385` bytes
- SHA-256: `D18329FDC66FA94E9DB6F59F914184A54BEB84F1F5C3BCF45A91E44FF4875C6C`

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

The user installed the local `0.2.0`, `0.2.1`, `0.2.2`, and `0.2.3` packages into IntelliJ IDEA. They confirmed that 0.2.2 was better and identified its vertical orthogonal-bar geometry, primary-only Alt-click behavior, and low-visibility limitations. After testing the 0.2.3 correction, they judged it better and explicitly approved it for publication. Broader accessibility, scaling, performance, and dynamic-unload cells remain separate qualification work and are not inferred from that visual acceptance.

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

[GitHub Actions run 30690180931](https://github.com/Myang-print/motionflow/actions/runs/30690180931) passed all four jobs for the exact 0.2.3 implementation commit `26f0424`:

- Ubuntu package, structure, and test gate: 3m57s;
- macOS test and configuration gate: 2m23s;
- Windows test and configuration gate: 13m12s;
- two-baseline Plugin Verifier gate: 12m59s.

This hosted matrix independently confirms the local 0.2.3 test, package, configuration, structure, and compatibility evidence above.
