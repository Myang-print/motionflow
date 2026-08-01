# Release Readiness

Assessment date: 2026-08-01

Candidate: MotionFlow `0.2.1`

## Decision

The `0.2.1` source and unsigned local distribution are **engineering-complete release candidates**: the scoped effects, controls, resilience rules, direction-aware gradient trail, documentation, packaging, automated compatibility gates, and EDT-safe live startup fix are implemented.

JetBrains Marketplace publication is intentionally gated. Hosted CI has passed, but publication still requires repository secrets for an author certificate and Marketplace token plus the remaining interactive accessibility/performance/unload observations. The project does not label those external gates as passed without evidence.

## Gate summary

| Gate | Result | Evidence |
| --- | --- | --- |
| Compile, tests, structure, package | Pass | [Automated validation](automated-validation.md) |
| Public-API and forbidden-mechanism scan | Pass | `SourceArchitectureTest` |
| Plugin Verifier, current and latest | Pass | [Compatibility](compatibility.md) |
| Sandbox plugin load | Pass | [Automated validation](automated-validation.md) |
| Branding and diagrams | Pass | SVG assets rendered headlessly and visually inspected |
| Diagnostics privacy | Pass | Sanitizer tests and fixed-schema review |
| Windows live effect/height smoke | Pass on `0.2.0` | User installed the local package and confirmed the trail ran at caret height; their visual feedback drove `0.2.1` |
| Exact `0.2.1` speed-line visual smoke | Pending owner observation | Install the exact ZIP above and exercise local plus Split Right/Down movement from every side |
| Full Windows visual/accessibility matrix | Pending | Follow the manual acceptance procedure below |
| Hosted Windows/macOS/Ubuntu CI | Earlier baseline passed; `0.2.1` pending push | [GitHub Actions run 30651018811](https://github.com/Myang-print/motionflow/actions/runs/30651018811) |
| Dynamic unload and 100-editor heap gate | Pending interactive qualification | [Performance evidence](performance.md) |
| Author signature and Marketplace upload | Pending owner credentials | `.github/workflows/release.yml` |

## Artifact protocol

The tag workflow requires `vX.Y.Z` to match `gradle.properties`, reruns all automated gates, optionally signs and publishes when the complete secret set exists, writes `SHA256SUMS`, and creates the GitHub release from the exact verified ZIP.

Required Marketplace secrets:

- `PUBLISH_TOKEN`
- `CERTIFICATE_CHAIN`
- `PRIVATE_KEY`
- `PRIVATE_KEY_PASSWORD`

The workflow fails on a partial secret set and never commits credentials.

## Manual acceptance procedure

1. Install the exact release ZIP into a clean IDE profile.
2. Exercise horizontal, vertical, and diagonal caret movement; switch between Split Right/Down panes in both directions; then exercise primary click, navigation jumps, editor changes, tool-window changes, and optional frame dimming.
3. Toggle globally during every active effect and verify immediate cleanup.
4. Repeat with Reduced Motion, Power Save Mode, high contrast, light/dark themes, and 100–200% display scale.
5. Disable/uninstall dynamically and inspect for overlays, timers, retained editors, and plugin exceptions.
6. Record p50/p95/p99 frame preparation, controlled typing latency, and retained heap against the disabled baseline.
