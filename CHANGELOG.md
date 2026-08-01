# Changelog

All notable changes to MotionFlow will be documented here. The format follows [Keep a Changelog](https://keepachangelog.com/en/1.1.0/), and releases use semantic versioning.

## [Unreleased]

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

[Unreleased]: https://github.com/Myang-print/motionflow/compare/v0.1.1...HEAD
[0.1.1]: https://github.com/Myang-print/motionflow/compare/v0.1.0...v0.1.1
[0.1.0]: https://github.com/Myang-print/motionflow/releases/tag/v0.1.0
