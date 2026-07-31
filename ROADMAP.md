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

- [x] Monotonic clock and easing primitives.
- [x] Timeline state machine and continuous retargeting.
- [x] One active-only frame scheduler and repaint coalescing.
- [x] Immutable policy, profiles, Reduced Motion, and Power Save Mode.
- [x] Bounded metrics and adaptive degradation.
- [x] Deterministic and scheduler/capacity tests.

## Then — M2–M3 Product alpha

- [x] Versioned settings service and accessible settings UI.
- [x] Capability registry and editor lifecycle manager.
- [x] Caret trail.
- [x] Click ripple.
- [x] Active-line navigation pulse.
- [ ] Editor churn/leak and typing-latency qualification.

## Later — M4–M5 Workbench beta

- [x] Selected-editor context cue.
- [x] Tool-window focus cue.
- [x] Optional inactive-frame dim overlay.
- [x] Toggle/settings/diagnostics actions.
- [x] Theme-aware contrast, accessibility policy, and failure isolation.

## Release — M6–M7

- [x] Plugin Verifier against the 2025.3 target and latest resolved IDE.
- [x] Windows sandbox startup and clean plugin-load log.
- [x] Cross-platform hosted test workflow and tag-gated release automation.
- [x] Marketplace metadata, branding, checksums, and signing/publishing integration.
- [ ] Product/OS/theme/scale interactive matrix.
- [ ] Dynamic unload, retained-heap, and performance gates.
- [ ] Closed beta with at least ten representative users.
- [ ] Signed private-channel release candidate.
- [x] Marketplace-ready `0.1.0` source candidate.

## Research-only candidates

These are not commitments and may be rejected:

- public-API moving tab indicator;
- safe tree/list selection cues;
- remote-development modular packaging;
- popup reveal effects if a supported lifecycle API appears.

Animated layout geometry, arbitrary custom scripts/CSS, reflection, and IDE-file patching are not roadmap items.
