# MotionFlow Implementation Plan

| Field | Value |
| --- | --- |
| Based on | PRD 1.0 |
| Plan version | 1.0 |
| Status | Ready for execution |
| Planning horizon | `0.1.0` |
| Last updated | 2026-07-31 |

## 1. Purpose

This is the controlling implementation plan for MotionFlow. It converts the product requirements into ordered engineering work, verification evidence, and release gates. Detailed subsystem designs are linked from each workstream.

The plan is intentionally public-API-first. If implementation reveals that a stable requirement needs an internal API, that requirement is not silently implemented. The work stops at the affected task, the capability is downgraded or deferred, and the PRD/ADR/traceability records are updated.

## 2. System decomposition

```text
Platform events
    │
    ▼
Surface adapters ──► immutable effect requests ──► motion policy
    │                                               │
    │                                               ▼
    └──────────────────────────────────────────► motion engine
                                                    │
                                                    ▼
                                      disposable layered overlays
                                                    │
                                                    ▼
                                              Swing repaint

Settings ──► policy snapshots
Diagnostics ◄── engine metrics + capability registry + sanitized failures
```

Planned package boundaries:

```text
io.github.myangprint.motionflow
├── core
│   ├── animation       # Clock, timeline, easing, scheduler
│   ├── policy          # Profiles, reduced motion, power policy
│   ├── capability      # Runtime feature support and reasons
│   └── diagnostics     # Bounded local metrics and sanitized snapshots
├── settings            # Persistent state, configurable, preview
├── editor
│   ├── lifecycle       # Editor attach/detach
│   ├── caret           # Trail adapter and painter
│   ├── ripple          # Click ripple adapter and painter
│   └── navigation      # Active-line pulse
├── workbench
│   ├── editorcontext   # Selected-file transition cue
│   ├── toolwindow      # Tool-window focus cue
│   └── frame           # Optional inactive-frame dim overlay
├── actions             # Toggle, settings, diagnostics
└── ui                  # Theme/color/scale helpers
```

Dependencies point inward: surface packages depend on `core`; `core` never imports editor, workbench, settings UI, PSI, VFS, or product-specific APIs.

## 3. Work breakdown and order

### M0 — Foundation

Status: initialized in the first round.

#### M0.1 Repository scaffold

- Deliverables: Gradle Wrapper, Kotlin/JVM 21 build, IntelliJ Platform Gradle Plugin 2.x, latest 2025.3 patch target, plugin descriptor, icon, license, CI, contributor files.
- Requirements: G-01, G-06, MF-S-007.
- Verification:
  - `./gradlew check`
  - `./gradlew verifyPluginProjectConfiguration`
  - `./gradlew verifyPluginStructure`
  - clean Git status after an intentional commit
- Definition of done: a fresh clone builds without globally installed Gradle and contains no template placeholder identity.

#### M0.2 Product and architecture baseline

- Deliverables: PRD, implementation plan, details, upstream analysis, feasibility matrix, ADRs, traceability matrix.
- Verification: every MVP requirement appears in the traceability matrix and has a planned verification method.
- Definition of done: no unresolved contradiction across scope, platform constraints, implementation, and release claims.

### M1 — Core motion engine and policy

Dependency: M0.

#### M1.1 Time and easing primitives

- Implement `MonotonicClock`, `Easing`, `CubicBezier`, and immutable `MotionSpec`.
- Initial easing presets: linear, standard, emphasized-decelerate, emphasized-accelerate.
- Clamp duration to `0..2000 ms`; stable defaults remain `80..300 ms`.
- Requirements: MF-E-001, MF-E-007, MF-F-008.
- Tests: boundaries, monotonicity where required, deterministic samples, invalid input, zero duration.

#### M1.2 Timeline state machine

- Implement states `IDLE`, `RUNNING`, `FINISHED`, `CANCELLED`.
- Implement start, retarget-from-current, finish, cancel, owner disposal.
- Use latest-intent-wins replacement keys: `(surfaceOwner, effectType, channel)`.
- Requirements: MF-E-005, MF-E-006, MF-E-009.
- Tests: state transition table, cancellation during callback, retarget at boundary times, disposal race.

#### M1.3 Frame scheduler and repaint coordinator

- Implement one application service with a Swing `Timer` only while work is active.
- Tick target: 60 Hz; compute progress from elapsed monotonic time rather than tick count.
- Coalesce repaint regions by component; remove disposed/non-showing owners before render.
- Enforce 64 global and 8 per-surface timeline caps.
- Requirements: MF-E-002–005, MF-E-009, MF-P-001, MF-P-006–007.
- Tests: one-timer invariant, idle shutdown, replacement/cap behavior, repaint union, reentrant scheduling.

#### M1.4 Policy service

- Implement immutable `MotionPolicy` computed from persisted settings, profile, Reduced Motion, Power Save Mode, high contrast, and capability state.
- Expose a message-bus topic carrying old/new policy snapshots.
- Requirements: MF-F-001–003, MF-A-001–006, MF-P-009.
- Tests: precedence matrix and immediate cancellation behavior.

#### M1.5 Metrics and adaptive degradation

- Implement capped rolling samples and counters with no logging on the frame path.
- Implement particle/alpha simplification followed by nonessential pause.
- Requirements: MF-E-010, MF-P-002–010.
- Tests: percentile calculation, bounded memory, threshold hysteresis, reset.

M1 exit gate:

- deterministic unit suite passes;
- a scheduler lifecycle integration test proves no active timer after the last cancellation;
- synthetic 100-timeline stress test respects caps and contains no unbounded allocation;
- core imports pass the architectural dependency test.

Detailed design: [motion engine](details/01-motion-engine.md), [performance](details/05-performance-observability.md).

### M2 — Settings, lifecycle, and capability foundation

Dependency: M1. May run before visual effects so all later work is policy-driven.

#### M2.1 Persistent settings

- Implement application-level `PersistentStateComponent` as a light service.
- Use a versioned state schema and explicit migration functions.
- Normalize values on load; do not mutate persisted state from getters.
- Requirements: MF-F-002–003, MF-F-008.
- Tests: defaults, round trip, malformed XML/state, schema migrations.

#### M2.2 Settings UI and preview

- Implement `BoundConfigurable` or `Configurable` with Kotlin UI DSL 2.
- Build an isolated preview using the production engine and a preview-local disposable.
- Apply/cancel/reset semantics must match platform conventions.
- Requirements: Section 8, MF-A-001, MF-A-007–009.
- Tests: model tests; UI Robot or manual automation for keyboard order and Apply/Cancel.

#### M2.3 Capability registry

- Detect headless/test, high contrast, Power Save Mode, editor suitability, product/version, and remote frontend state using supported APIs.
- Record structured no-op reasons.
- Do not infer native caret settings through reflection; use platform/version and observable safe behavior only.
- Requirements: MF-C-001–006, MF-ED-006.
- Tests: capability truth table with injectable environment facade.

#### M2.4 Editor lifecycle manager

- Register `EditorFactoryListener` declaratively or through an application service with a parent disposable.
- Attach one `EditorEffectsSession` per eligible editor and store it using a safe owner mapping that cannot outlive the editor.
- Requirements: MF-ED-005, MF-ED-012, MF-C-006.
- Tests: open/release churn, duplicate attach prevention, dynamic unload.

M2 exit gate:

- settings round-trip and migration tests pass;
- global disable tears down preview and live sessions;
- 100-editor churn test has no retained disposed editor attributable to MotionFlow.

Detailed design: [settings and accessibility](details/04-settings-accessibility.md), [compatibility](details/07-compatibility-risk.md).

### M3 — Editor effects

Dependency: M1 and M2.

#### M3.1 Caret trail

- Listen through `EditorEventMulticaster.addCaretListener(listener, disposable)` or editor-local `CaretModel` with an editor session disposable.
- Convert the event caret's visual position to editor component coordinates after the platform move completes.
- Maintain independent origin, sample buffer, generation, immutable visual, and keyed timeline state for each admitted caret. Admit the primary first, cap the editor at eight animated carets, and dispose state on the public caret-removed event.
- Interpolate each visual trail head from that caret's immediately previous coordinate to the completed platform destination, sample once per shared engine frame, and render a bounded silky ribbon that fades after arrival. New movement resets only the same caret's stale history.
- Project the vertical caret footprint onto the motion normal so horizontal motion retains caret-height emphasis while vertical motion remains a narrow aligned streak.
- Expose All carets/Primary only coverage and Soft/Balanced/Clear/Bright visibility choices; default Balanced to All carets + Clear and retain hard alpha/width caps.
- Keep Speed lines and Rounded ribbon as explicit non-default styles and retain the thinner solid High Contrast fallback.
- Observe component-local editor focus and render split-pane transitions through a temporary shared-root overlay only when both editors remain showing in the same root pane.
- Initial maximum: 12 trail samples; Balanced default: 7.
- Coalesce multiple moves within one engine frame.
- Requirements: MF-ED-001–006, MF-ED-011–016.
- Tests: per-caret exact-origin replacement, independent bounded multi-caret admission/removal, per-frame interpolation/fade, projected caret-footprint/taper/gradient geometry, every cardinal and diagonal arrival vector, visibility selection, coordinate mapping, scrolling, folding, soft wrap, one-line editors, split-pane focus re-anchoring, cross-frame rejection, release during animation, native-caret policy.

#### M3.2 Click ripple

- Listen to editor mouse events without consuming them.
- Trigger only on primary-button `MOUSE_RELEASED` with drag distance below threshold.
- Clip to editor viewport; use a theme-derived accent and bounded alpha.
- Requirements: MF-ED-007–008.
- Tests: click/drag classification, right click, selection, reduced motion, scale factor.

#### M3.3 Active-line navigation pulse

- Track the previous primary-caret visual line.
- Trigger only when the absolute visual-line delta meets the threshold and the cause is not ordinary typing.
- Render a short horizontal alpha cue; never modify editor markup or text attributes.
- Requirements: MF-ED-009–010.
- Tests: keyboard jump, mouse jump, typing, document reflow, editor switch.

M3 exit gate:

- editor effects never wrap or replace platform action handlers;
- latency and allocation microbenchmarks meet budgets;
- mixed-DPI and theme manual tests pass;
- editor churn/leak test remains green.

Detailed design: [editor effects](details/02-editor-effects.md).

### M4 — Workbench effects

Dependency: M1 and M2. Can proceed in parallel with M3 after shared foundations exist.

#### M4.1 Editor-context indicator

- Observe `FileEditorManagerListener.selectionChanged`.
- First implementation paints a short edge/highlight cue on the selected editor component.
- Investigate public `JBTabs` geometry only through classes verified as supported. If not safe, retain the edge cue and close the tab-geometry spike.
- Requirements: MF-WB-001–002.
- Tests: split editors, preview tab, diff editor, non-text editor, editor close during transition.

#### M4.2 Tool-window focus cue

- Observe `ToolWindowManagerListener` declaratively.
- Attach painters to showing tool-window components with project-scoped disposables.
- Render after activation; do not animate component bounds.
- Requirements: MF-WB-003–004, MF-WB-008.
- Tests: show/hide/activate, multiple projects, unavailable tool window, project close.

#### M4.3 Inactive-frame dim overlay

- Off by default.
- Observe window focus without a process-wide AWT listener if a narrower frame lifecycle API suffices.
- Paint a translucent, input-transparent plugin component in the frame's standard Swing layered pane.
- Exclude dialogs/popups/high contrast/Power Save/reduced motion.
- Requirements: MF-WB-005–008.
- Tests: multi-frame focus, modal dialog, alt-tab, disable while dimmed, project dispose.

M4 exit gate:

- no platform component is re-parented, resized, or assigned a replacement UI delegate;
- multi-project and modal-dialog test matrix passes;
- all painters disappear on unload.

Detailed design: [workbench effects](details/03-workbench-effects.md).

### M5 — Actions, diagnostics, resilience, and polish

Dependency: M1–M4.

#### M5.1 User actions

- Implement toggle, open settings, and copy diagnostics actions.
- `update()` must be cheap and declare the correct action update thread.
- Requirements: MF-F-004–006.
- Tests: action state, no-project context, clipboard snapshot sanitization.

#### M5.2 Failure isolation

- Add per-effect circuit breakers with one retry and session disable.
- Add structured, rate-limited logging outside hot loops.
- Requirements: MF-C-002–004.
- Tests: injected painter failure, repeated event failure, unaffected effect continuity.

#### M5.3 Theme, scale, and accessibility polish

- Centralize theme color resolution and alpha constraints.
- Respond to look-and-feel changes by invalidating derived colors and repainting, not by rebuilding global services.
- Complete high-contrast, keyboard, screen-reader label, and preview-pause checks.
- Requirements: MF-A-003–008, MF-ED-011.

M5 exit gate:

- diagnostics contain no paths or workspace identifiers in golden tests;
- circuit-breaker tests pass;
- accessibility checklist is signed off.

### M6 — Verification and beta

Dependency: M1–M5.

#### M6.1 Automated verification

- Run unit, platform, integration, architecture, leak, and packaging tests.
- Run Plugin Verifier against current target and latest compatible releases.
- Run Qodana/Plugin DevKit inspections for internal APIs and dynamic unload.
- Requirements: Section 15.

#### M6.2 Performance qualification

- Build repeatable editor-navigation, typing, clicking, editor-churn, and tool-window scripts.
- Record disabled/plugin-on runs with identical conditions.
- Publish raw local results as CI/manual artifacts, not marketing claims.
- Requirements: MF-P-001–010.

#### M6.3 Compatibility matrix

- Mandatory: Windows + IntelliJ IDEA 2025.3 and latest stable; macOS latest stable; Ubuntu latest LTS.
- Representative products: IntelliJ IDEA, PyCharm, WebStorm, GoLand, Rider frontend where applicable.
- Themes/scales and remote-development cells follow the PRD.

#### M6.4 Closed beta

- Recruit at least 10 users representing primary personas.
- Use a fixed task script and short survey.
- Triage all P0/P1, performance, accessibility, and compatibility reports before release.

M6 exit gate:

- all PRD release gates pass;
- no unsupported compatibility claim remains in README/plugin.xml;
- beta success targets are met or the release is re-scoped explicitly.

### M7 — `0.1.0` release

Dependency: M6.

#### M7.1 Release preparation

- Freeze feature work; update changelog, screenshots, privacy statement, third-party notice, and Marketplace copy.
- Verify version range against current stable/EAP findings.
- Generate source tag and signed distribution from CI.

#### M7.2 Publication

- Upload to a private Marketplace channel first.
- Install the exact signed artifact into clean IDE profiles.
- Promote only after smoke tests and checksum comparison.
- Monitor issues manually during the first 72 hours.

## 4. Test strategy summary

| Layer | Purpose | Examples |
| --- | --- | --- |
| Pure unit | Deterministic core behavior | easing, timelines, policy precedence, percentiles |
| Platform unit | API and lifecycle integration | editor attach/release, services, settings state |
| UI integration | Real Swing behavior | painters, coordinates, settings, actions |
| Architecture | Enforce dependency/API rules | core import boundary, forbidden packages/reflection |
| Leak | Prove cleanup | editor/project churn, dynamic unload |
| Performance | Enforce budgets | frame cost, allocations, typing latency, idle timer |
| Compatibility | Cross-version/product confidence | Plugin Verifier and smoke matrix |
| Manual visual | Human motion quality | theme, DPI, multiple monitors, interruption behavior |

## 5. Branch and review policy

- Keep changes small enough to review by subsystem or vertical feature.
- Every feature PR links PRD requirement IDs and updates the traceability matrix.
- Stable feature PRs may not introduce suppressed internal-API warnings.
- Performance-sensitive PRs include before/after evidence from the same environment.
- Use explicit staging; do not mix generated artifacts or unrelated workspace changes.
- A feature is not complete because it looks correct in one IDE. Lifecycle, reduced motion, performance, and failure behavior are part of the feature.

## 6. Definition of done for any effect

An effect is done only when all statements are true:

1. Event source and painter use supported APIs.
2. The effect observes but does not intercept platform behavior.
3. It is controlled by immutable policy and responds to global disable.
4. It cancels on owner disposal and dynamic unload.
5. Reduced Motion and Power Save Mode behavior is defined and tested.
6. Theme, scale, clipping, rapid retarget, and interruption cases are tested.
7. Performance metrics are inside budget.
8. Diagnostics report support/disable state without private data.
9. Traceability and user-facing documentation are updated.
10. Plugin Verifier and relevant inspections pass.

## 7. Stop conditions

Stop and redesign rather than shipping if any of the following occurs:

- the only implementation path imports an internal or implementation-class API;
- an effect requires replacing a platform UI delegate or layout manager;
- input events must be consumed, delayed, or replayed;
- idle timers or global polling are required;
- disposed editor/project instances remain reachable;
- measured p95 cost exceeds the budget after adaptive degradation;
- a stable feature cannot fail closed on unsupported products;
- the effect creates a conflict with native IDE motion that cannot be detected or avoided safely.

## 8. Detailed implementation documents

- [Architecture and lifecycle](details/00-architecture-lifecycle.md)
- [Motion engine](details/01-motion-engine.md)
- [Editor effects](details/02-editor-effects.md)
- [Workbench effects](details/03-workbench-effects.md)
- [Settings and accessibility](details/04-settings-accessibility.md)
- [Performance and observability](details/05-performance-observability.md)
- [Testing and release](details/06-testing-release.md)
- [Compatibility and risk controls](details/07-compatibility-risk.md)
