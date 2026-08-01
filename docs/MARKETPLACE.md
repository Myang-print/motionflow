# JetBrains Marketplace Listing

## Short description

Smooth, native visual feedback for IntelliJ Platform IDEs—without delaying input or patching the IDE.

## Feature summary

MotionFlow adds restrained visual orientation cues for caret movement, primary clicks, large navigation jumps, editor changes, tool-window focus, and optional frame deactivation. It includes four profiles, per-effect controls, Reduced Motion, Power Save behavior, high-contrast simplification, and redacted local diagnostics.

MotionFlow is implemented with supported IntelliJ event APIs and standard Swing overlays. It does not inject CSS or JavaScript, patch IDE files, replace input handlers, read document text, send telemetry, or make network requests.

## Suggested media order

1. `docs/assets/motionflow-hero.png`
2. `docs/assets/effects.svg`
3. `docs/assets/architecture.svg`
4. A clean-IDE MotionFlow settings screenshot captured during the final interactive acceptance pass

## Publishing contract

Only the tag workflow publishes. It verifies tag/version equality, runs tests and Plugin Verifier, signs when the complete certificate secret set is available, produces SHA-256 checksums, and creates the GitHub release. Marketplace compatibility claims must match [compatibility evidence](evidence/compatibility.md).
