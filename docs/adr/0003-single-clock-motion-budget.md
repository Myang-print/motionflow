# ADR 0003: Single Active-Only Clock and Hard Motion Budgets

- Status: Accepted
- Date: 2026-07-31

## Context

Swing UI work runs on the EDT. Independent timers, polling loops, unbounded effect queues, or frame-count-based interpolation can add latency and remain active while the user is idle. A visual plugin loses its value if it makes typing or navigation less responsive.

## Decision

MotionFlow will use one application-scoped frame scheduler that runs only while at least one timeline is active. Timelines advance from monotonic elapsed time, late frames are dropped, repaint regions are coalesced, and state/concurrency are capped.

The PRD performance budgets are release gates. Sustained over-budget cost triggers adaptive simplification and then pauses nonessential effects. Budgets cannot be raised merely to make a regression pass.

## Consequences

Positive:

- zero intended idle loop/callback cost;
- consistent timing and cancellation across effects;
- centralized metrics and degradation;
- rapid interactions retarget rather than queue.

Negative:

- engine implementation is foundational work before visible features;
- all effects must fit the shared request/timeline/painter model;
- a scheduler defect could affect multiple effects, requiring strong isolation tests.

## Alternatives rejected

- **One timer per effect/surface:** duplicates EDT wakeups and complicates cleanup.
- **Permanent 60 Hz loop:** unacceptable idle cost.
- **Frame-count progress:** slows or stretches motion when EDT frames are delayed.
- **Never degrade visual quality:** prioritizes decoration over IDE responsiveness.

## Reconsideration trigger

Only measured evidence that the chosen Swing timer cannot satisfy correctness/performance may justify a scheduler change. The one-active-clock, elapsed-time, bounded-state, and zero-idle-work invariants remain unless a superseding ADR changes the PRD.

