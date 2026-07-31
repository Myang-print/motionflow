# Requirement Traceability Matrix

Status legend: `Planned`, `Implemented`, `Verified`, `Deferred`. M0 establishes the plan; runtime requirements remain `Planned` until code and evidence exist.

| Requirement | Implementation owner | Planned verification | Status |
| --- | --- | --- | --- |
| MF-F-001 | M1 policy, M2 lifecycle | Disable-during-every-effect integration test; timer/painter cleanup | Planned |
| MF-F-002 | M1 policy, M2 settings | Profile golden table and settings UI test | Planned |
| MF-F-003 | M2 settings | Persistence round trip; Apply live update; restart smoke | Planned |
| MF-F-004 | M5 actions | Find Action/no-project/toggle state tests | Planned |
| MF-F-005 | M5 actions | Settings navigation UI test | Planned |
| MF-F-006 | M5 diagnostics | Snapshot golden and privacy rejection tests | Planned |
| MF-F-007 | M1 engine, M2 lifecycle | Repeated enable/disable registration-count test | Planned |
| MF-F-008 | M2 settings | Invalid-state clamp/migration/log-once tests | Planned |
| MF-E-001 | M1 time | Fake-clock and monotonic adapter contract tests | Planned |
| MF-E-002 | M1 scheduler | Multi-project/surface one-timer invariant test | Planned |
| MF-E-003 | M1 scheduler | Idle/start/last-finish lifecycle test | Planned |
| MF-E-004 | M1 repaint coordinator | Rectangle union and request-count test | Planned |
| MF-E-005 | M1 timeline | State transition and disposal race tests | Planned |
| MF-E-006 | M1 timeline | Mid-flight retarget continuity test | Planned |
| MF-E-007 | M1 easing | Endpoint/reference/fuzz tests | Planned |
| MF-E-008 | M1 architecture | Core dependency/forbidden API test; painter code review | Planned |
| MF-E-009 | M1 scheduler | Late-tick elapsed-time/frame-drop test | Planned |
| MF-E-010 | M1/M5 diagnostics | Counter accuracy and bounded-buffer tests | Planned |
| MF-ED-001 | M3 caret | Caret move painter UI test | Planned |
| MF-ED-002 | M3 caret | Multi-caret primary-only test | Planned |
| MF-ED-003 | M3 architecture | No handler replacement scan; typing latency test | Planned |
| MF-ED-004 | M3 caret | Point count/age bound and stress test | Planned |
| MF-ED-005 | M2/M3 lifecycle | Hidden/disposed/unsupported editor tests | Planned |
| MF-ED-006 | M2 capability, M3 caret | 2025.3 and 2026.1+ coexistence matrix | Planned |
| MF-ED-007 | M3 ripple | Primary-click position/render test | Planned |
| MF-ED-008 | M3 ripple | Drag/right-click/reduced-motion negative tests | Planned |
| MF-ED-009 | M3 navigation | Large visual-line jump test | Planned |
| MF-ED-010 | M3 navigation | One-character typing negative test | Planned |
| MF-ED-011 | M3 UI helpers | 100–200% and mixed-DPI coordinate tests | Planned |
| MF-ED-012 | M2/M3 lifecycle | 100-editor churn and release-mid-effect tests | Planned |
| MF-WB-001 | M4 editor context | Selection change cue integration test | Planned |
| MF-WB-002 | M4 capability | Unsupported tab geometry fallback test | Planned |
| MF-WB-003 | M4 tool window | Activation cue UI test | Planned |
| MF-WB-004 | M4 architecture | No geometry mutation test/code scan | Planned |
| MF-WB-005 | M4 frame | Focus loss/gain overlay test | Planned |
| MF-WB-006 | M4 frame | Modal/high-contrast/session exclusion matrix | Planned |
| MF-WB-007 | M4 policy | Default-off/reduced/power behavior tests | Planned |
| MF-WB-008 | M4 ownership | Two-project independent-state test | Planned |
| MF-C-001 | M2 capability | Capability declaration completeness test | Planned |
| MF-C-002 | M2/M5 capability | Unsupported API/surface no-op test | Planned |
| MF-C-003 | M5 resilience | Two-failure circuit-breaker isolation test | Planned |
| MF-C-004 | M5 diagnostics | Repeated error rate-limit test | Planned |
| MF-C-005 | M2 lifecycle | Headless/unit-test no-listener/no-timer test | Planned |
| MF-C-006 | M0/M2 lifecycle | Dynamic unload/reload and classloader leak test | Planned |
| MF-A-001 | M2 settings | Top-level Reduced Motion discoverability test | Planned |
| MF-A-002 | M1/M2 policy | Spatial cancellation and ≤80 ms fallback tests | Planned |
| MF-A-003 | All effects | Spec/static check for duration/repetition; manual audit | Planned |
| MF-A-004 | Product/UI | Accessibility review of every cue | Planned |
| MF-A-005 | M5 theme | Contrast calculation/theme matrix | Planned |
| MF-A-006 | M1/M5 policy | High-contrast simplified-policy test | Planned |
| MF-A-007 | M2 settings | Accessible names and keyboard focus UI audit | Planned |
| MF-A-008 | M2 preview | Play-once/pause/no-idle-loop test | Planned |
| MF-A-009 | M2 capability/copy | UI copy inspection; no false system claim | Planned |
| MF-P-001 | M1 scheduler | Five-minute idle timer/callback profile | Planned |
| MF-P-002 | M1/M6 performance | Controlled active-frame benchmark median | Planned |
| MF-P-003 | M1/M6 performance | Controlled p95/p99 benchmark | Planned |
| MF-P-004 | M3/M6 performance | Alternating typing-latency baseline | Planned |
| MF-P-005 | M2/M3 lifecycle | Heap/leak analysis after 100 editors | Planned |
| MF-P-006 | M1 diagnostics | Scheduler and ring-cap invariant tests | Planned |
| MF-P-007 | M1 admission | Global/per-surface cap tests | Planned |
| MF-P-008 | M1 degradation | Threshold/hysteresis and sustained-load tests | Planned |
| MF-P-009 | M1/M2 policy | Power Save enter/leave policy matrix | Planned |
| MF-P-010 | M1/M5 logging | Hot-path log instrumentation/static test | Planned |
| MF-S-001 | Architecture/release | Network dependency/code scan and runtime observation | Planned |
| MF-S-002 | All adapters | Forbidden content access test/code review | Planned |
| MF-S-003 | M5 diagnostics | Privacy golden/property tests | Planned |
| MF-S-004 | Architecture | Script/CSS/remote asset/dependency scan | Planned |
| MF-S-005 | Architecture | Filesystem/config mutation review and sandbox diff | Planned |
| MF-S-006 | M0/M6 quality | Forbidden API scan, DevKit, Plugin Verifier | Planned |
| MF-S-007 | M0/M6 dependencies | Dependency/license/vulnerability review | Planned |
| MF-S-008 | M7 release | CI provenance, signing, tag, SHA-256 comparison | Planned |

## Milestone evidence index

| Milestone | Required evidence location |
| --- | --- |
| M0 | CI logs, build outputs, documentation review |
| M1 | Unit/platform test reports and engine benchmark summary |
| M2 | Settings migration fixtures and editor lifecycle leak report |
| M3 | Editor UI artifacts, latency/allocation report |
| M4 | Multi-project/window UI artifacts and unload test |
| M5 | Accessibility checklist and diagnostics privacy results |
| M6 | Compatibility matrix, Plugin Verifier, performance report, beta summary |
| M7 | Release tag, signed artifact, SHA-256, Marketplace smoke results |

When implementation starts, replace generic test descriptions with concrete test class/method or manual-procedure links. A status may become `Verified` only when the named evidence exists.

