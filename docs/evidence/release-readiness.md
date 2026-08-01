# Release Readiness

Assessment date: 2026-08-01

Candidate: MotionFlow `0.2.3`

## Decision

The `0.2.3` source and unsigned local distribution are **engineering-complete release candidates**: projected all-angle trail geometry, bounded multi-caret state, visibility/coverage controls, continuous exact-origin motion, resilience rules, documentation, packaging, and local automated compatibility gates are implemented.

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
| Windows live continuous/origin smoke | Pass for `0.2.2` with refinement findings | User judged 0.2.2 better, marked the PR ready, and supplied vertical/multi-caret/visibility evidence |
| Exact `0.2.3` projected multi-caret visual smoke | Pending owner observation | Install the exact ZIP above; verify vertical alignment, Alt-click carets, all visibility levels, and Split Right/Down |
| Full Windows visual/accessibility matrix | Pending | Follow the manual acceptance procedure below |
| Hosted Windows/macOS/Ubuntu CI | Previous 0.2.2 head passed; `0.2.3` pending push | [GitHub Actions run 30686443994](https://github.com/Myang-print/motionflow/actions/runs/30686443994) |
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
2. Exercise horizontal, vertical, and diagonal movement with one caret and with 2–8 Alt-click carets; switch coverage and every visibility level; switch between Split Right/Down panes in both directions; then exercise primary click, navigation jumps, editor changes, tool-window changes, and optional frame dimming.
3. Toggle globally during every active effect and verify immediate cleanup.
4. Repeat with Reduced Motion, Power Save Mode, high contrast, light/dark themes, and 100–200% display scale.
5. Disable/uninstall dynamically and inspect for overlays, timers, retained editors, and plugin exceptions.
6. Record p50/p95/p99 frame preparation, controlled typing latency, and retained heap against the disabled baseline.
