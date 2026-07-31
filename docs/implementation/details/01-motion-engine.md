# Motion Engine Details

## 1. Design targets

The engine must provide visually smooth interpolation without making correctness depend on a 60 Hz timer. It is a small deterministic state machine running on the EDT, with one active timer per application process and no timer while idle.

## 2. Core types

Illustrative interfaces; exact names may change during implementation.

```kotlin
fun interface MonotonicClock {
    fun nowNanos(): Long
}

fun interface Easing {
    fun transform(progress: Double): Double
}

data class MotionSpec(
    val durationMillis: Int,
    val easing: EasingId,
    val essential: Boolean,
    val reducedMotionFallback: ReducedMotionFallback,
)

data class EffectKey(
    val ownerId: Long,
    val type: EffectType,
    val channel: String,
)

interface Timeline {
    val key: EffectKey
    val owner: Disposable
    fun sample(nowNanos: Long): TimelineSample
    fun retarget(nowNanos: Long, target: EffectTarget)
    fun cancel(reason: CancelReason)
}
```

`ownerId` is an internal identity token, not a project/editor/file identifier and is never persisted or copied into diagnostics.

## 3. Time and progress

Use `System.nanoTime()` behind `MonotonicClock`. Wall-clock time is forbidden for animation progress because clock adjustments can move backward or jump.

For a nonzero duration:

```text
raw = (nowNanos - startNanos) / durationNanos
progress = clamp(raw, 0, 1)
eased = easing(progress)
```

For duration zero, the first sample is the target state and the timeline finishes immediately without starting the Swing timer.

Frame count is not part of progress. If the EDT is busy, the next sample advances to current elapsed time. The engine never executes missed intermediate frames.

## 4. Easing

Initial presets:

| ID | Use | Shape |
| --- | --- | --- |
| `LINEAR` | opacity diagnostics/tests | constant velocity |
| `STANDARD` | short balanced transitions | smooth ease-out |
| `EMPHASIZED_DECELERATE` | elements entering attention | quick start, soft arrival |
| `EMPHASIZED_ACCELERATE` | elements leaving | soft start, quick exit |

If cubic Bézier easing is used, solve `x(t) = progress` for `t`, then return `y(t)`. Use a bounded Newton iteration with binary-search fallback. Validate control points so x coordinates are in `[0,1]`; clamp floating-point output at the public boundary.

Tests include endpoints, deterministic reference samples, finite outputs, invalid control points, and 10,000 random progress values.

## 5. Timeline state machine

```text
IDLE --start--> RUNNING
RUNNING --sample p=1--> FINISHED
RUNNING --cancel/dispose--> CANCELLED
RUNNING --retarget--> RUNNING (new start=current sampled value)
FINISHED/CANCELLED --any command--> no-op except explicit new timeline creation
```

Cancellation is idempotent. Completion and cancellation callbacks execute at most once. A callback may submit/cancel another timeline; registry iteration therefore uses a stable snapshot or a two-phase mutation queue.

## 6. Retargeting

Retargeting prevents jumps during rapid user input.

At retarget time `tr`:

1. sample the current interpolated state from the old timeline at `tr`;
2. use that state as the new start;
3. replace the target and motion specification;
4. choose the new duration from distance-aware policy, capped by profile limits;
5. preserve the same effect key.

Never queue stale destinations. For a selected-editor indicator, repeated `A → B → C` input before the first animation finishes produces a continuous path from the current visual state toward C.

## 7. Scheduler

`MotionEngine` owns a `FrameScheduler` abstraction. Production uses a coalescing `javax.swing.Timer` with an initial delay of zero and nominal delay of 16 ms. Tests use a manual scheduler and fake clock.

Tick algorithm:

1. assert EDT;
2. capture `nowNanos` once;
3. drain pending cancellation/retarget commands;
4. sample timelines up to the global cap;
5. discard invalid/disposed owners;
6. publish immutable painter models;
7. accumulate dirty rectangles by target component;
8. remove finished/cancelled timelines;
9. issue coalesced repaint requests;
10. record elapsed engine cost;
11. stop the timer if no active timeline remains.

Do not call `paintImmediately`, `Toolkit.sync`, `invokeAndWait`, or sleep.

## 8. Repaint coordination

Each sample reports a previous and current logical bounding rectangle. The dirty region is their union plus antialias/shadow padding. Regions for the same component in a tick are unioned and clamped to component bounds.

If the union covers most of a small component, a component repaint is acceptable. For editor-sized components, prefer bounded repaint regions. Coordinates remain Swing logical pixels; `Graphics2D` owns device scaling.

Painter models are immutable snapshots so paint methods perform only arithmetic and draw calls. They do not allocate collections, resolve settings, query services, or transform platform state.

## 9. Capacity and degradation

Limits:

- 64 global timelines;
- 8 timelines per surface;
- 12 caret trail samples maximum in MVP;
- 512 frame-cost metric samples;
- one sanitized error record per category plus a five-entry recent ring.

Admission policy:

1. retarget an equivalent key when possible;
2. cancel the oldest nonessential timeline on the same surface;
3. cancel the oldest nonessential global timeline;
4. reject the new nonessential request and count it;
5. essential accessibility feedback may replace another essential effect only through an explicit priority rule.

Adaptive degradation uses hysteresis to prevent oscillation:

- enter level 1 after p95 > 2 ms over 120 active samples;
- level 1 reduces trail points, shadows, and gradient complexity;
- enter level 2 if another 120 samples remain over budget;
- level 2 pauses nonessential effects for the session;
- automatic recovery requires 600 under-budget active samples and never overrides explicit Reduced Motion/Power Save policy.

## 10. Policy application

The engine accepts an already resolved `MotionPolicy`; it does not read persisted state.

On policy change:

- master disabled: cancel everything and clear all painter models;
- Reduced Motion enabled: cancel spatial effects and optionally replace current feedback with a ≤80 ms fade;
- duration/intensity changed: new requests use new values; long-running existing effects may retarget once from current state;
- effect disabled: cancel only matching keys;
- Power Save Mode entered: apply selected power policy immediately.

## 11. Error behavior

One failing timeline is removed in the same tick. Its error is categorized by effect type and operation (`sample`, `publish`, `repaint`). The engine continues with other timelines. Repeated errors never produce repeated stack traces on the hot path.

Out-of-memory and fatal JVM errors are not swallowed.

## 12. Test plan

### Pure tests

- monotonic clock adapter contract;
- easing endpoints/reference values/fuzzing;
- progress at negative/zero/mid/end/late time;
- state transition table;
- callback at-most-once behavior;
- retarget continuity;
- cap and replacement policy;
- percentile and hysteresis calculations.

### EDT integration tests

- scheduler starts exactly once from idle;
- zero-duration timeline does not start timer;
- reentrant submissions do not corrupt iteration;
- last completion stops timer;
- owner disposal during tick removes state;
- repaint union and clipping are correct.

### Performance tests

- 1, 8, 32, and 64 simple timelines;
- caret model with maximum samples;
- rapid retarget at keyboard-repeat rates;
- allocation profile after warm-up;
- timer inactive and metrics stable while idle.

