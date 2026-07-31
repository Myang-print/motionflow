# Testing and Release Details

## 1. Quality strategy

Motion quality requires more than screenshot correctness. Testing must cover deterministic interpolation, platform lifecycle, event non-interference, cleanup, tail latency, compatibility, accessibility, packaging, and real visual judgment.

## 2. Test suites

### 2.1 Pure unit tests

Run without a full IDE where possible:

- clock and easing math;
- timeline state machine and retargeting;
- policy precedence and profiles;
- settings validation/migration;
- bounded ring buffers and percentiles;
- admission/circuit-breaker/degradation logic;
- diagnostics sanitization.

Naming: `*Test.kt`. Tests use a fake monotonic clock and manual scheduler; no real sleeping.

### 2.2 Platform tests

Use IntelliJ Platform test framework for:

- service creation and disposal;
- settings persistence integration;
- editor session attach/release;
- public event/listener behavior;
- action availability and no-project behavior;
- plugin descriptor/resource bundle checks.

### 2.3 UI integration tests

Use a supported UI testing approach only for behavior that requires a showing component:

- settings Apply/Cancel/Reset and keyboard order;
- painter installation/removal;
- editor coordinate mapping under scrolling/folding/zoom;
- tool-window and multi-frame behavior;
- dynamic install/uninstall where automation is stable.

UI tests assert state/lifecycle and capture artifacts on failure. Pixel-perfect equality is avoided across OSs; geometry and tolerance-based samples are acceptable.

### 2.4 Architecture tests

- forbidden imports/packages/reflection;
- `core` does not depend on `editor`, `workbench`, or settings UI;
- no dependency introduces a second Kotlin standard library;
- no process-wide polling/timer except the approved scheduler;
- no hard-coded user-visible strings;
- all effect registries are bounded.

### 2.5 Leak and unload tests

- 100 editor create/release cycles;
- 20 project open/close cycles where feasible;
- trigger every effect before disposal;
- inspect weak references after GC/test framework leak assertions;
- install, uninstall without restart, reinstall, and verify no duplicate callbacks;
- run Plugin DevKit dynamic plugin inspection.

## 3. Requirement test naming

Tests and manual procedures cite requirement IDs in comments, display names, or traceability metadata, for example:

```kotlin
@Test
fun `MF-E-006 retarget starts from current interpolated value`() { ... }
```

One test may cover multiple requirements, but every MVP requirement must map to at least one verification item in `docs/traceability-matrix.md`.

## 4. Local commands

```powershell
.\gradlew.bat check
.\gradlew.bat verifyPluginProjectConfiguration
.\gradlew.bat verifyPluginStructure
.\gradlew.bat buildPlugin
.\gradlew.bat verifyPlugin
.\gradlew.bat runIde
```

`verifyPlugin` may download multiple IDE distributions and is expected to be slower. A successful `buildPlugin` does not replace Plugin Verifier or manual UI qualification.

## 5. Continuous integration

Pull requests run:

1. Gradle wrapper validation;
2. `check`;
3. project configuration and plugin structure verification;
4. plugin distribution build;
5. Plugin Verifier against configured current/latest IDEs;
6. artifact upload for the plugin ZIP and relevant reports.

Scheduled or release-candidate CI may run broader product/version matrices. Secrets are unavailable to pull requests and publishing never occurs from a pull-request workflow.

## 6. Compatibility matrix

### Mandatory before `0.1.0`

| OS | Product | Version | Theme/scale | Scope |
| --- | --- | --- | --- | --- |
| Windows 11 | IntelliJ IDEA | 2025.3.x | Dark 100%, 150% | Full |
| Windows 11 | IntelliJ IDEA | Latest stable | Light/High Contrast, mixed DPI | Full |
| macOS current | IntelliJ IDEA | Latest stable | Light/Dark, Retina | Full |
| Ubuntu latest LTS | IntelliJ IDEA | Latest stable | Dark 100%, 200% | Full |
| One primary OS | PyCharm | Latest stable | Default | Smoke + verifier |
| One primary OS | WebStorm | Latest stable | Default | Smoke + verifier |
| One primary OS | GoLand | Latest stable | Default | Smoke + verifier |

### Conditional claims

- Rider: verify frontend behavior and product dependency before claiming.
- Android Studio: use its current stable distribution and manual test before listing.
- Remote development: run separate frontend/backend sessions before claiming.
- Additional products: Plugin Verifier plus a minimal editor/tool-window smoke test.

Record every cell with artifact checksum, date, result, and linked issue for waivers. A waiver cannot cover data loss, IDE freeze, input interference, internal API use, or leak.

## 7. Visual acceptance script

For each mandatory full cell:

1. open a representative code file and enable Balanced;
2. exercise small and large caret moves;
3. type continuously and verify no visible input lag;
4. click, drag-select, use multiple carets, fold, soft-wrap, and scroll;
5. switch editors rapidly and use split editors;
6. show/activate/hide tool windows;
7. switch theme and display scale if supported;
8. enable Reduced Motion during active effects;
9. enter/leave Power Save Mode;
10. globally disable during every effect type;
11. close editors/projects and unload the plugin;
12. copy diagnostics and inspect for private data.

## 8. Security and dependency checks

- review all direct and transitive runtime dependencies;
- prefer zero runtime third-party dependencies in MVP;
- verify Gradle Wrapper checksum/validation in CI;
- use Dependabot for Gradle and GitHub Actions updates;
- review automated dependency PRs; never auto-merge release tooling blindly;
- build from tagged source in CI;
- sign Marketplace artifact only in protected release context;
- compare release ZIP checksum before final promotion.

## 9. Release process

### 9.1 Release candidate

1. freeze scope and update version/changelog;
2. run local mandatory Windows full cell;
3. run CI check/build/structure/verifier;
4. complete remaining OS/product matrix;
5. inspect plugin ZIP contents and size;
6. install exact ZIP in clean profiles;
7. run dynamic unload and diagnostics privacy checks;
8. create signed release candidate in a private Marketplace channel;
9. repeat smoke test from downloaded signed artifact.

### 9.2 Public release

1. confirm Marketplace metadata, screenshots, source, license, and privacy statement;
2. tag immutable source commit;
3. publish signed artifact and checksum;
4. verify Marketplace installation on at least two products;
5. publish release notes containing only shipped behavior;
6. monitor issues for 72 hours and prepare rollback/unlist response for critical regressions.

### 9.3 Rollback criteria

Immediately withdraw or supersede a release for:

- data loss or input corruption;
- reproducible IDE freeze/crash attributable to MotionFlow;
- persistent visual overlay after disable/uninstall;
- sensitive data in diagnostics or network activity;
- severe typing latency regression;
- dynamic unload/classloader leak affecting update/uninstall;
- Marketplace or license violation.

## 10. Evidence archive

For every release retain:

- source commit and tag;
- CI run links;
- unsigned/signed artifact SHA-256;
- Plugin Verifier reports;
- compatibility matrix;
- performance summary and environment;
- accessibility checklist;
- known limitations and approved waivers;
- Marketplace artifact/version identifier.

