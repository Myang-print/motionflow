# Requirement Traceability Matrix

Status legend:

- `Verified`: implementation exists and the named automated or recorded evidence passed.
- `Implemented`: implementation exists; a PRD-prescribed interactive, cross-product, latency, heap, or publication gate remains.
- `Deferred`: intentionally outside the `0.1.0` claim boundary.

## Core and control

| Requirement | Concrete implementation/evidence | Status |
| --- | --- | --- |
| MF-F-001 | `MotionPolicyService`; disposable runtime layers in `EditorEffectsManager` and `ProjectEffectsService`; `MotionEngine.cancelOwner` | Implemented |
| MF-F-002 | `MotionStateNormalizer.defaults`; `MotionStateNormalizerTest` | Verified |
| MF-F-003 | `MotionSettingsService` persistent application state and synchronous live topic | Implemented |
| MF-F-004 | `ToggleMotionAction`; bundled action text and notification | Implemented |
| MF-F-005 | `OpenSettingsAction`; searchable configurable ID | Implemented |
| MF-F-006 | `DiagnosticsService`, `DiagnosticsSanitizer`; `DiagnosticsSanitizerTest` | Verified |
| MF-F-007 | One manager/service instance and one disposable runtime layer per scope; source review | Implemented |
| MF-F-008 | `MotionStateNormalizer`; clamping test; per-field warning deduplication | Verified |

## Motion engine

| Requirement | Concrete implementation/evidence | Status |
| --- | --- | --- |
| MF-E-001 | `SystemMonotonicClock`; deterministic nanosecond timeline tests | Verified |
| MF-E-002 | Application `MotionEngine`; exactly-one-timer architecture test | Verified |
| MF-E-003 | Active-only timer; zero-duration and last-completion EDT integration tests | Verified |
| MF-E-004 | Per-component rectangle union in `requestRepaint`; one flush per tick | Implemented |
| MF-E-005 | `ScalarTimeline` state/cancel tests; owner cancellation and isolated callbacks | Verified |
| MF-E-006 | `ScalarTimeline.retarget`; continuity test | Verified |
| MF-E-007 | Preset cubic Bézier solver; endpoint/reference/fuzz tests | Verified |
| MF-E-008 | Package-boundary and forbidden-API architecture tests; JDK `JLayeredPane` ADR | Verified |
| MF-E-009 | Elapsed-time sampling and late-frame counter; timeline tests | Verified |
| MF-E-010 | `MotionMetrics`, 512-sample ring buffer; counter/percentile tests | Verified |

## Editor effects

| Requirement | Concrete implementation/evidence | Status |
| --- | --- | --- |
| MF-ED-001 | `EditorEffectsSession`; projected vertical-caret footprint in `CaretFlowGeometry`; horizontal/vertical/diagonal thickness tests | Verified |
| MF-ED-002 | Public add/move/remove caret callbacks; `CaretTrailAdmission`; independent keyed state capped at eight; admission and architecture tests | Verified |
| MF-ED-003 | Post-event listeners only; no handler replacement/interception architecture scan | Verified |
| MF-ED-004 | Configured limit clamped to 2–12; `ArrayDeque` age/size expiry | Implemented |
| MF-ED-005 | Main-editor capability filter, showing/disposed guards, disposable session | Implemented |
| MF-ED-006 | Build-261 native-caret-era policy cap; 253/262 Plugin Verifier cells | Implemented |
| MF-ED-007 | Editing-area primary-release ripple model and renderer | Implemented |
| MF-ED-008 | Drag/right-click/popup/stale-click classifier tests; Reduced Motion policy test | Verified |
| MF-ED-009 | Configurable visual-line jump classifier and pulse renderer | Verified |
| MF-ED-010 | Below-threshold negative classifier test | Verified |
| MF-ED-011 | Logical component coordinates plus `SwingUtilities.convertPoint/convertRectangle` | Implemented |
| MF-ED-012 | Editor release disposes session/overlay/timelines; interactive churn gate pending | Implemented |
| MF-ED-013 | `CrossEditorCaretFlow` shared-root overlay, component-local focus lifecycle, geometry and architecture tests | Implemented |
| MF-ED-014 | `CaretFlowGeometry.trailRibbon`, projected footprint, `CaretTrailPainter`, rendered gradient/core/vertical-width and all-angle geometry tests | Verified |
| MF-ED-015 | Per-caret `CaretTrailSampleBuffer.startAt`, keyed frame-sampled local motion, cross-editor primary flow, focus re-anchor guards, origin replacement/interpolation tests | Verified |
| MF-ED-016 | Schema-v4 coverage/visibility selectors, profile defaults, immutable policy mapping, hard painter caps, settings/policy/geometry tests | Verified |

## Workbench effects

| Requirement | Concrete implementation/evidence | Status |
| --- | --- | --- |
| MF-WB-001 | `FileEditorManagerListener` selected-editor edge cue | Implemented |
| MF-WB-002 | Component-level edge fallback; no tab traversal | Verified |
| MF-WB-003 | `ToolWindowManagerListener` focus outline | Implemented |
| MF-WB-004 | Overlay-only painting; forbidden implementation/internal API scan | Verified |
| MF-WB-005 | Window-local focus listener and dim overlay | Implemented |
| MF-WB-006 | Owned/modal window exclusion, presentation/high-contrast policy | Implemented |
| MF-WB-007 | Dim off by default; Reduced Motion and Power Save cancellation | Verified |
| MF-WB-008 | Project service ownership and per-project runtime disposable | Implemented |

## Capability and resilience

| Requirement | Concrete implementation/evidence | Status |
| --- | --- | --- |
| MF-C-001 | `CapabilityRegistry` and explicit `CapabilityState` taxonomy | Implemented |
| MF-C-002 | Fail-closed runtime/editor checks and no-op attachment path | Implemented |
| MF-C-003 | Per-effect `EffectCircuitBreaker`; retry/isolation tests; engine callback-isolation test | Verified |
| MF-C-004 | Bounded deduplicated diagnostic error categories; no hot-path logging | Verified |
| MF-C-005 | Headless/unit-test policy test and startup capability gate | Verified |
| MF-C-006 | Disposable manager/project/session/overlay ownership; Verifier dynamic eligibility; manual unload pending | Implemented |

## Accessibility

| Requirement | Concrete implementation/evidence | Status |
| --- | --- | --- |
| MF-A-001 | Top-level Reduced Motion checkbox in searchable settings | Implemented |
| MF-A-002 | Spatial cancellation and essential-cue 80 ms cap; policy test | Verified |
| MF-A-003 | One-shot finite timelines only; no loop/repeat API | Verified |
| MF-A-004 | Every effect duplicates state already available in native UI; visual audit pending | Implemented |
| MF-A-005 | Theme-derived accent selection and WCAG contrast test | Verified |
| MF-A-006 | High-contrast solid cue policy; ripple/dim disabled | Implemented |
| MF-A-007 | Labels, accessible names/descriptions, keyboard-native Swing controls; searchable-options build | Verified |
| MF-A-008 | Isolated production-engine preview with explicit Play/Pause and no auto-loop | Implemented |
| MF-A-009 | Explicit user setting; no automatic OS-detection claim in UI/docs | Verified |

## Performance and resources

| Requirement | Concrete implementation/evidence | Status |
| --- | --- | --- |
| MF-P-001 | Active-only timer and idle integration tests | Verified |
| MF-P-002 | Bounded frame preparation and local p50 metric; reference-machine gate pending | Implemented |
| MF-P-003 | Bounded p95/p99 metrics and degradation; reference-machine gate pending | Implemented |
| MF-P-004 | Post-event observation and bounded callbacks; controlled typing benchmark pending | Implemented |
| MF-P-005 | Disposable editor sessions; 100-editor retained-heap gate pending | Implemented |
| MF-P-006 | Exactly-one-timer and 512-sample ring-buffer tests | Verified |
| MF-P-007 | 64 global/8 per-surface admission tests, including essential saturation | Verified |
| MF-P-008 | 120-sample entry and 600-sample recovery hysteresis tests | Verified |
| MF-P-009 | All Power Save policies and essential-only duration test | Verified |
| MF-P-010 | No frame/paint logging; source review | Verified |

## Privacy, security, and release

| Requirement | Concrete implementation/evidence | Status |
| --- | --- | --- |
| MF-S-001 | Network API and third-party runtime dependency scans; privacy contract | Verified |
| MF-S-002 | Event metadata/geometry only; no document/VFS/PSI access in adapters | Verified |
| MF-S-003 | Fixed diagnostic schema and path/identity/secret rejection tests | Verified |
| MF-S-004 | No scripts, CSS, remote assets, reflection, agents, or instrumentation; architecture scan | Verified |
| MF-S-005 | Plugin settings only; no IDE/JVM/registry mutation in source | Verified |
| MF-S-006 | Forbidden-API scan, structure/configuration gates, Plugin Verifier 253/262 | Verified |
| MF-S-007 | No third-party runtime dependency test; Apache-2.0 and notices review | Verified |
| MF-S-008 | Tag/version gate, optional secret-backed signing/publishing, checksums, GitHub release workflow | Implemented |

## Evidence index

- [Automated validation](evidence/automated-validation.md)
- [Compatibility](evidence/compatibility.md)
- [Performance and resource evidence](evidence/performance.md)
- [Release readiness](evidence/release-readiness.md)

`Implemented` rows are deliberately not promoted to `Verified` until their specified interactive or external evidence exists.
