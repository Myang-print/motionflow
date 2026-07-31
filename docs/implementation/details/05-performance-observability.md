# Performance and Observability Details

## 1. Performance model

MotionFlow executes on the EDT, so small average cost is not enough; tail latency and lifecycle behavior matter. The product therefore budgets update cost, paint preparation, allocation, idle activity, retained heap, and typing latency.

The performance objective is not to guarantee 60 rendered frames under all IDE loads. The objective is to preserve platform responsiveness, advance by elapsed time, and skip visual detail when necessary.

## 2. Budgets

| Metric | Target | Hard release gate |
| --- | ---: | ---: |
| Idle timers | 0 | 0 |
| Idle periodic callbacks | 0 | 0 |
| Median active frame preparation | ≤0.50 ms | ≤0.75 ms |
| p95 active frame preparation | ≤1.50 ms | ≤2.00 ms |
| p99 active frame preparation | ≤3.00 ms | investigate/fix |
| Added p95 typing latency | ≤2 ms | no significant regression |
| Retained heap after 100-editor churn | ≤8 MiB over baseline | no retained disposed editors |
| Global timelines | ≤64 | 64 |
| Timelines per surface | ≤8 | 8 |
| Diagnostics samples | ≤512 | 512 |

`Frame preparation` covers timeline sampling, immutable model publication, dirty-region calculation, and repaint submission. A separate UI profiling run measures actual paint cost because Swing may call painters outside the scheduler callback.

## 3. Measurement protocol

### 3.1 Controlled environment

Record:

- hardware model, CPU, RAM, GPU;
- OS/build, IDE/product/build, JBR;
- display topology and scaling;
- project fixture hash;
- enabled plugins and Power Save Mode;
- MotionFlow commit, artifact checksum, profile;
- warm-up duration and run count.

Compare plugin-disabled and plugin-enabled runs in alternating order to reduce thermal and background-process bias. Use at least five measured runs after warm-up and report median plus tail percentiles, not a single best run.

### 3.2 Interaction scenarios

1. idle for five minutes after indexing finishes;
2. type 1,000 characters through a reproducible input script;
3. repeat caret navigation and 100 large jumps;
4. click and drag-select in the editor;
5. switch among 30 files at keyboard-repeat pace;
6. activate tool windows 100 times;
7. open/close 100 editors;
8. move project frame between different-DPI monitors;
9. run maximum Expressive profile stress;
10. enter/leave Power Save and Reduced Motion during activity.

### 3.3 Tools

- built-in IDE Activity Monitor/thread dumps for UI freezes;
- Java Flight Recorder or Async Profiler where supported;
- heap dump/reference analysis for disposed editors/projects;
- engine's local bounded metrics for internal frame costs;
- UI test timing for reproducible interaction, treated as supporting rather than sole evidence.

Raw results go under a dated local/CI artifact directory excluded from Git unless a summarized benchmark document is intentionally reviewed.

## 4. Allocation controls

Hot-path rules:

- no stream/sequence construction per frame;
- no unbounded lists/deques/maps;
- no color parsing or service lookup in paint;
- no logging string construction unless debug mode is already known active;
- reuse small mutable builders behind the EDT boundary, then publish compact immutable snapshots;
- cap trail points and concurrent ripple models;
- copy only the state needed by the painter.

Object pooling is not a default. Introduce it only after a profile shows allocation pressure and tests can prove correct reset/lifecycle behavior.

## 5. Frame-cost metrics

Use `System.nanoTime()` around the engine tick and optionally around MotionFlow painter bodies in diagnostics builds. Store duration values in a fixed circular `LongArray(512)`.

Percentiles are calculated from a copy outside the tick or on user request. Diagnostics include sample count so small-sample percentiles are not misleading.

Counters:

- active and peak timelines;
- submitted, retargeted, completed, cancelled, rejected timelines;
- repaint requests before/after coalescing;
- late frames and dropped visual frames;
- effect circuit-breaker openings;
- adaptive degradation transitions;
- capability no-op counts by category.

No counter key contains project/editor/file identity.

## 6. Adaptive degradation

State:

```text
NORMAL -> SIMPLIFIED -> NONESSENTIAL_PAUSED
```

Transitions require sustained evidence, not one slow EDT frame that may belong to the IDE.

- Enter SIMPLIFIED after 120 active samples with MotionFlow p95 >2 ms.
- Simplify trail count, alpha layers, stroke complexity, and simultaneous ripples.
- Enter NONESSENTIAL_PAUSED after another 120 over-budget samples.
- Essential opacity orientation cues may continue if individually cheap.
- Recovery requires 600 active samples below 1 ms p95 and no new circuit-breaker event.
- The user can reset session degradation through diagnostics, but it re-enters if thresholds recur.

Diagnostics clearly labels this as an inference from MotionFlow's own measured callback time, not proof that MotionFlow caused every slow IDE frame.

## 7. Diagnostics snapshot schema

Illustrative plain text:

```text
MotionFlow diagnostics
pluginVersion=0.1.0
ideBuild=IU-253.x
os=Windows 11 amd64
profile=BALANCED
reducedMotion=false
powerSaveBehavior=DISABLE_ALL
capabilities=caretTrail:SUPPORTED,frameDim:DISABLED_BY_SETTING
timelines.active=0
timelines.peak=5
frame.samples=512
frame.p50Ms=0.21
frame.p95Ms=0.73
frame.p99Ms=1.11
frames.dropped=2
degradation=NORMAL
errors=none
```

Sanitization tests reject:

- path separators followed by plausible local paths;
- home/user directory values;
- project names, file names/extensions, document fragments;
- host names, user names, environment variables;
- raw exception messages and stack traces.

## 8. Logging

Levels:

- `INFO`: plugin lifecycle and one-time capability summary only when useful;
- `WARN`: normalized invalid state and an opened effect circuit breaker, rate-limited;
- `DEBUG`: explicit diagnostics mode, policy changes, aggregate engine counters;
- no per-frame/per-caret/per-mouse logs.

Logs use categorical reason codes. Exceptions are logged once when needed for issue diagnosis, but copied diagnostics contain only sanitized categories.

## 9. Performance regression workflow

1. reproduce with the same artifact/settings/project and plugin-disabled baseline;
2. determine whether cost is scheduler, adapter, painter, allocation, or lifecycle;
3. add the smallest automated regression scenario possible;
4. fix root cause or disable the affected capability;
5. rerun the entire affected scenario, not only a microbenchmark;
6. update benchmark evidence and traceability;
7. never raise a budget merely to make a regression pass without an approved PRD change.

