# Changelog

All notable changes to MotionFlow will be documented here. The format follows [Keep a Changelog](https://keepachangelog.com/en/1.1.0/), and releases use semantic versioning.

## [Unreleased]

## [0.2.3] - 2026-08-01

### Added

- Added independent trails for all admitted carets, with primary-first admission and a hard eight-caret editor cap.
- Added **Caret coverage** choices for All carets or Primary only.
- Added **Trail visibility** choices: Soft, Balanced, Clear, and Bright; Clear is the stronger Balanced-profile default.

### Changed

- Projected the vertical native-caret footprint onto the motion normal, retaining full caret-height emphasis horizontally while producing a narrow aligned stem vertically.
- Increased the local trail base peak from `0.66` to `0.72`, with schema-v4 visibility scaling and a hard `0.96` painter cap.
- Made Subtle use Primary only + Soft and Expressive use All carets + Bright.

### Fixed

- Removed the full-height orthogonal crossbar seen during up/down caret movement.
- Added public caret add/remove lifecycle handling so Alt-click secondary carets receive independent exact-origin trails and release their state immediately when removed.

## [0.2.2] - 2026-08-01

### Added

- Added **Silky native trail**, a frame-sampled afterimage with a smooth center core and gradual post-arrival fade.

### Changed

- Increased the standard-theme trail's bounded peak visibility while preserving theme-derived color and source readability.
- Made the silky trail the schema-v3 default; schema-v2 speed-line defaults migrate automatically, while Speed lines and Rounded ribbon remain explicit options.
- Updated split-pane flow to build its tail from actual animated-head samples rather than a rigid two-point wedge.

### Fixed

- Reset each local trail to the immediately previous caret instead of retaining a stale origin from an older movement.
- Suppressed destination-editor local trails during split-pane focus changes and re-anchored after one EDT turn, leaving one exact source-to-destination cue.

## [0.2.1] - 2026-08-01

### Added

- Added a persisted caret-trail style selector. `Speed lines (gradient)` is the default; `Rounded ribbon` preserves the former appearance as an option.

### Changed

- Replaced the default uniform capsule with a direction-aware tapered polygon whose color and alpha strengthen from the transparent origin to the full caret-height leading edge.
- Made local and cross-pane trail geometry robust for movement from every cardinal side and diagonal quadrant.
- Replaced the rounded cross-pane proxy caret with a sharp-edged caret cue and removed a redundant repaint request.

## [0.2.0] - 2026-08-01

### Added

- Added a distance-aware caret-flow cue when focus moves between visible Split Right or Split Down editor panes sharing one root pane.

### Changed

- Changed the standard caret trail from a thin line to a lower-alpha ribbon whose thickness follows the current logical caret height.
- Kept the High Contrast trail as a thinner solid indicator to preserve source readability.

## [0.1.1] - 2026-08-01

### Fixed

- Fixed editor and workbench effects failing to start because IntelliJ may invoke post-startup activities on a background thread while MotionFlow's UI services are EDT-confined.
- Made the deferred startup callback expire safely if its project is disposed before the EDT turn runs.

## [0.1.0] - 2026-08-01

### Added

- Initialized a Kotlin/Gradle IntelliJ Platform plugin project targeting build 253+ and the latest 2025.3 maintenance release by default.
- Added the MotionFlow product requirements, implementation plan, subsystem details, research, architecture decisions, and requirement traceability matrix.
- Added repository, CI, privacy, security, and contribution foundations.
- Implemented the shared monotonic motion engine with one active-only Swing timer, elapsed-time sampling, retargeting, repaint coalescing, bounded capacity, metrics, and adaptive degradation.
- Added Subtle, Balanced, Expressive, and Custom profiles; application persistence; Reduced Motion; Power Save behavior; live policy updates; and an isolated one-shot settings preview.
- Added bounded primary-caret trail, click ripple, large-navigation pulse, selected-editor cue, tool-window cue, and optional inactive-frame dimming.
- Added input-transparent standard Swing layered overlays with disposable lifecycle ownership and no internal IntelliJ APIs.
- Added global toggle, settings, and redacted diagnostics actions with localized UI copy.
- Added failure isolation, high-contrast simplification, native smooth-caret-era coexistence, privacy sanitization, and architecture guards.
- Added product branding, diagrams, user documentation, troubleshooting guidance, and release evidence templates.

[Unreleased]: https://github.com/Myang-print/motionflow/compare/v0.2.3...HEAD
[0.2.3]: https://github.com/Myang-print/motionflow/compare/v0.2.2...v0.2.3
[0.2.2]: https://github.com/Myang-print/motionflow/compare/v0.2.1...v0.2.2
[0.2.1]: https://github.com/Myang-print/motionflow/compare/v0.2.0...v0.2.1
[0.2.0]: https://github.com/Myang-print/motionflow/compare/v0.1.1...v0.2.0
[0.1.1]: https://github.com/Myang-print/motionflow/compare/v0.1.0...v0.1.1
[0.1.0]: https://github.com/Myang-print/motionflow/releases/tag/v0.1.0
