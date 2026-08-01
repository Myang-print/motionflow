# Performance and Resource Evidence

Date: 2026-08-01

## Enforced bounds

| Invariant | Bound | Evidence |
| --- | ---: | --- |
| Application schedulers | 1 | Source architecture test |
| Scheduler while idle | 0 active timers | Real EDT integration tests |
| Timelines | 64 global, 8 per surface | Admission tests, including all-essential saturation |
| Caret trail points | 12 maximum | Bounded deque implementation |
| Ripples per editor | 3 maximum | Bounded editor overlay model |
| Frame samples | 512 | Ring-buffer tests |
| Degradation entry | 120 over-budget samples | Deterministic hysteresis test |
| Degradation recovery | 600 healthy samples | Deterministic hysteresis test |

Timelines advance from monotonic elapsed time rather than frame count, so a late frame skips obsolete intermediate state instead of extending input feedback. Repaint requests for the same component are unioned once per scheduler tick.

## What is and is not measured

The engine records bounded p50/p95/p99 frame-preparation metrics locally and exposes them only through redacted diagnostics. Automated tests verify calculation, capacity, idle shutdown, and degradation mechanics.

No claim is made here that the PRD's machine-specific typing-latency or retained-heap thresholds have passed. Those gates require a warmed interactive IDE, a controlled project, repeated plugin-off/plugin-on trials, and heap inspection. They remain release-observation work rather than invented benchmark numbers.
