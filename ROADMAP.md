# Roadmap

The PRD defines scope; this file is the short execution view. Dates are intentionally omitted until performance and API spikes reduce uncertainty.

## Now — M0 Foundation

- [x] Initialize Kotlin/Gradle IntelliJ Platform project.
- [x] Establish product name, identity, compatibility baseline, and license.
- [x] Analyze VSCode Animations and current platform constraints.
- [x] Write PRD, implementation plan, detailed designs, ADRs, and traceability matrix.
- [x] Add CI/contributor/security/privacy foundations.
- [x] Complete initial local build and structure/configuration verification.

## Next — M1 Core engine

- [ ] Monotonic clock and easing primitives.
- [ ] Timeline state machine and continuous retargeting.
- [ ] One active-only frame scheduler and repaint coalescing.
- [ ] Immutable policy, profiles, Reduced Motion, and Power Save Mode.
- [ ] Bounded metrics and adaptive degradation.
- [ ] Deterministic and performance tests.

## Then — M2–M3 Product alpha

- [ ] Versioned settings service and accessible settings UI.
- [ ] Capability registry and editor lifecycle manager.
- [ ] Caret trail.
- [ ] Click ripple.
- [ ] Active-line navigation pulse.
- [ ] Editor churn/leak and typing-latency qualification.

## Later — M4–M5 Workbench beta

- [ ] Selected-editor context cue.
- [ ] Tool-window focus cue.
- [ ] Optional inactive-frame dim overlay.
- [ ] Toggle/settings/diagnostics actions.
- [ ] Theme, DPI, accessibility, and failure-isolation polish.

## Release — M6–M7

- [ ] Full Plugin Verifier and product/OS/scale matrix.
- [ ] Dynamic unload, retained-heap, and performance gates.
- [ ] Closed beta with at least ten representative users.
- [ ] Signed private-channel release candidate.
- [ ] Marketplace-ready `0.1.0`.

## Research-only candidates

These are not commitments and may be rejected:

- public-API moving tab indicator;
- safe tree/list selection cues;
- remote-development modular packaging;
- popup reveal effects if a supported lifecycle API appears.

Animated layout geometry, arbitrary custom scripts/CSS, reflection, and IDE-file patching are not roadmap items.
