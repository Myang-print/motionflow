# MotionFlow Agent Instructions

These instructions apply to the entire repository and supplement the user's global instructions.

## Source of truth

Read before implementation:

1. `docs/PRD.md`
2. `docs/implementation/implement.md`
3. the relevant file in `docs/implementation/details/`
4. `docs/traceability-matrix.md`
5. applicable ADRs in `docs/adr/`

If code, plan, and PRD conflict, stop and resolve the conflict explicitly. Do not silently broaden product scope.

## Architecture invariants

- Motion observes platform state; it never delays, blocks, reorders, or alters it.
- Stable code uses supported IntelliJ Platform and JDK APIs only.
- No reflection, internal/implementation APIs, Java agents, bytecode instrumentation, UI delegate/layout replacement, arbitrary scripts/CSS, or IDE-file modification.
- Mutable animation state is EDT-confined and bounded.
- There is one application frame scheduler, active only while timelines exist.
- Painters consume immutable snapshots and do not query PSI/VFS/indexes/services or log on hot paths.
- Every listener, painter, timer, and timeline has the shortest correct `Disposable` owner.
- Unsupported surfaces fail closed and expose a sanitized capability reason.
- Reduced Motion, Power Save Mode, global disable, dynamic unload, and owner disposal are mandatory test cases for every effect.

## Implementation workflow

- Use Kotlin unless a platform API requires Java.
- Keep `core` independent of editor/workbench/settings UI packages.
- Reference PRD IDs in tests and PR descriptions.
- Update the traceability matrix when adding implementation or evidence.
- Run at minimum `gradlew.bat check`, `verifyPluginProjectConfiguration`, and `verifyPluginStructure` on Windows.
- Run `verifyPlugin` before release/compatibility claims.
- Preserve unrelated work and stage explicit paths only.
- Do not commit build outputs, IDE state, sandbox data, profiler recordings, or secrets.

## Stop conditions

Stop and document a fallback/defer decision if a feature requires an unsupported API, input interception, platform component re-parenting, an idle polling loop, unbounded state, or misses the PRD performance budget.

