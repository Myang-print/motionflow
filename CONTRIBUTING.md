# Contributing to MotionFlow

MotionFlow welcomes focused, evidence-backed contributions. Because the plugin runs on the IDE UI thread, visual correctness alone is not sufficient; lifecycle, accessibility, compatibility, and performance are part of every effect.

## Start here

Read in this order:

1. [PRD](docs/PRD.md)
2. [Implementation plan](docs/implementation/implement.md)
3. the relevant [detailed design](docs/implementation/details/)
4. [traceability matrix](docs/traceability-matrix.md)
5. applicable [ADRs](docs/adr/)

## Set up

Use JDK 21 and the checked-in Gradle Wrapper.

```powershell
.\gradlew.bat check
.\gradlew.bat verifyPluginProjectConfiguration
.\gradlew.bat verifyPluginStructure
.\gradlew.bat runIde
```

The first build downloads the target IntelliJ Platform. `verifyPlugin` may download additional IDEs.

## Non-negotiable boundaries

- Use documented IntelliJ Platform/JDK APIs for stable code.
- Do not use reflection, internal/implementation APIs, Java agents, bytecode instrumentation, UI delegate replacement, or IDE-file patching.
- Observe platform events; do not consume, delay, replay, or reorder input/state transitions.
- Keep UI work bounded on the EDT; never read PSI, indexes, VFS content, files, or network data from animation callbacks.
- Tie every listener, painter, timer, and timeline to the shortest correct `Disposable`.
- Keep one active-only scheduler and no periodic idle work.
- Every effect must define Reduced Motion, Power Save Mode, failure, and unsupported-capability behavior.
- Never add telemetry/network behavior without an approved PRD/privacy change and explicit user consent.

## Change workflow

1. Link the issue and PRD requirement IDs.
2. Update the relevant detailed design if the implementation differs.
3. Add deterministic tests before or with implementation.
4. Add lifecycle/failure tests and performance evidence where relevant.
5. Update `docs/traceability-matrix.md` with concrete test/evidence links.
6. Run local checks and inspect only intentional changes.
7. Stage files explicitly; do not use `git add .` in a mixed worktree.

## Code expectations

- Kotlin is the default implementation language.
- Keep core math/state independent from editor/workbench packages.
- Prefer immutable painter snapshots and bounded collections.
- Avoid allocation and logging in frame/paint/caret hot paths.
- Use resource bundles for user-visible text.
- Comments explain invariants, lifecycle, or non-obvious platform behavior; do not narrate syntax.
- Do not suppress internal-API or compatibility inspections to make a check pass.

## Definition of done

Use the ten-point definition in the [implementation plan](docs/implementation/implement.md#6-definition-of-done-for-any-effect). A feature is not done until traceability and release evidence exist.

## Reporting issues

Include the copied MotionFlow diagnostics, exact IDE/product version, OS, theme, display scale, effect/profile, expected behavior, and reproduction steps. Diagnostics are designed not to contain file paths or project content, but review text before posting.

For security issues, follow [SECURITY.md](SECURITY.md) rather than opening a public bug report.

