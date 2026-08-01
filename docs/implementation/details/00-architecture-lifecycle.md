# Architecture and Lifecycle Details

## 1. Architectural objective

MotionFlow is an event-to-overlay pipeline. It observes a completed platform state change, converts it to immutable visual state, advances that state with one bounded scheduler, and requests Swing repaint. The plugin never becomes the owner of the IDE state being illustrated.

This boundary is the primary safety mechanism. An effect may fail or disappear without changing the selected file, caret position, tool-window state, mouse event, or underlying component geometry.

## 2. Layer boundaries

### 2.1 Core layer

The core layer has no dependency on editor, workbench, settings UI, VFS, PSI, or product modules.

Responsibilities:

- monotonic time;
- motion specifications and easing;
- timeline lifecycle and retargeting;
- one frame scheduler;
- repaint coalescing;
- policy calculation;
- capability state;
- bounded metrics and effect circuit breakers.

Allowed dependencies: Kotlin/JDK, stable `com.intellij.openapi` application/service/disposer APIs, Swing timer abstractions at the scheduler boundary.

### 2.2 Surface adapter layer

Adapters translate supported IntelliJ Platform events into effect requests. They own no scheduler and do not persist user settings.

Each adapter contains:

- a platform listener;
- coordinate/state extraction performed after the platform event;
- an eligibility predicate based on `MotionPolicy` and `CapabilitySnapshot`;
- an effect-specific model;
- a painter installed with a surface-local disposable.

### 2.3 Settings and actions layer

The settings service persists raw user intent. `MotionPolicyService` produces an immutable, normalized effective policy. Actions call services; they do not hold state.

### 2.4 Diagnostics layer

Diagnostics consume sanitized metrics and categorical failures. They cannot inspect editors, documents, file paths, projects, PSI, or VFS.

## 3. Service ownership

| Component | Scope | Lifetime | Why |
| --- | --- | --- | --- |
| `MotionSettingsService` | Application | Plugin/application | Visual preference is application-wide |
| `MotionPolicyService` | Application | Plugin/application | One effective policy per process |
| `MotionEngine` | Application | Plugin/application | One frame clock invariant |
| `CapabilityRegistry` | Application with project/session inputs | Plugin/application | Centralized fail-closed decisions |
| `DiagnosticsService` | Application | Plugin/application | Bounded cross-surface counters |
| `ProjectEffectsService` | Project | Project/plugin | Tool-window and frame/project wiring |
| `EditorEffectsSession` | Editor | Editor release/plugin | Shortest safe owner for editor listeners/painters |

Application/project services that own resources implement `Disposable` where needed. Extensions themselves do not implement lifecycle merely to unsubscribe; listeners use declarative registration where practical, and resources are registered under services or editor-local disposables.

## 4. Disposable tree

```text
plugin/application
├── MotionEngine
│   ├── frame timer (only while active)
│   └── application timelines
├── MotionPolicyService
├── ProjectEffectsService(project A)
│   ├── project message-bus connection
│   ├── tool-window painters
│   └── frame-focus painter
└── EditorEffectsSession(editor X)
    ├── caret listener
    ├── editor mouse listener
    ├── overlay painter
    └── editor-owned timelines
```

Rules:

1. A child never outlives its platform owner.
2. A callback checks owner disposal before dereferencing delayed state.
3. Timeline cancellation happens before painter disposal where ordering matters.
4. Engine maps use weak owner identity only as a secondary guard; correct disposal is mandatory.
5. No process-wide AWT hook is installed until a narrower supported event source is proven insufficient and separately reviewed.

## 5. Threading model

### 5.1 EDT-only operations

- receiving Swing/editor UI events that are documented on the EDT;
- timeline registry mutation;
- scheduler tick;
- painter state swap;
- `repaint()` request;
- component coordinate conversion;
- settings UI changes.

These operations must remain fast. They do not read PSI, indexes, VFS content, project models, files, or the network.

### 5.2 Background operations

The MVP has no required background computation. Percentile snapshots can be calculated from a copied 512-entry numeric buffer outside the frame callback if profiling shows a need. Any background result returned to the UI is immutable and applied only if its owner remains valid.

### 5.3 Concurrency invariant

The engine's mutable state is confined to the EDT. Public entry points assert or marshal pure UI work to the EDT. There is no explicit lock in animation code.

## 6. Event-to-render sequence

Example: caret movement.

```text
CaretListener receives new position
  -> adapter checks policy/capability/editor validity
  -> adapter maps caret visual position to logical component coordinates
  -> adapter submits EffectRequest(key, target, spec, payload)
  -> engine retargets or creates timeline
  -> engine starts its timer if registry was previously empty
  -> tick computes normalized progress using monotonic elapsed time
  -> painter model is atomically replaced on EDT
  -> repaint coordinator unions regions and calls repaint
  -> final tick removes timeline
  -> engine stops timer when registry becomes empty
```

## 7. Public API boundary

Expected stable APIs include:

- `EditorFactoryListener`, `EditorEventMulticaster`, `CaretListener`, `EditorMouseListener`;
- `FileEditorManagerListener`;
- `ToolWindowManagerListener`;
- JDK/Swing `JLayeredPane`, plugin-owned input-transparent `JComponent`s, and component/hierarchy listeners;
- application/project services, message bus, `Disposable`/`Disposer`;
- `PersistentStateComponent` and settings `Configurable` APIs;
- `PowerSaveMode` and look-and-feel/theme notifications if verified stable;
- JDK/Swing `Timer`, painting, transforms, and component coordinate helpers.

An API's package name alone is not proof of support. Every introduced IntelliJ symbol must pass Plugin DevKit inspections and Plugin Verifier for the declared matrix.

`IdeGlassPaneUtil.installPainter` is explicitly forbidden: implementation verification found `@ApiStatus.Internal` on the method in the 2025.3.6 SDK. `SurfaceOverlay` is the stable host and may use only standard JDK/Swing types.

Forbidden in stable source and dependencies:

- `com.intellij.*.impl` imports;
- symbols annotated `@ApiStatus.Internal`, `@ApiStatus.ScheduledForRemoval`, or unsupported experimental APIs without a recorded exception;
- reflection (`java.lang.reflect`, Kotlin reflection for platform access);
- `sun.*`, `jdk.internal.*`, Java agents, instrumentation, bytecode weaving;
- class-name string matching to identify internal Swing components;
- UI delegate/layout manager replacement;
- IDE installation or configuration patching.

An architecture test scans imports, constant strings, and dependencies for these patterns. Plugin Verifier remains the authoritative binary check.

## 8. Failure isolation

Each effect runs behind an `EffectCircuitBreaker`:

```text
CLOSED --first unexpected failure--> RETRY_ALLOWED
RETRY_ALLOWED --next invocation succeeds--> CLOSED
RETRY_ALLOWED --second failure--> OPEN_FOR_SESSION
```

Opening one breaker:

- cancels timelines for that effect;
- removes its painters/listeners if they are effect-specific;
- records a sanitized error category and IDE/plugin version;
- rate-limits a single warning log;
- leaves other effects and platform behavior unchanged.

Programmer errors detected in tests should not be hidden. The circuit breaker protects users at runtime, while tests inject failures and assert cleanup.

## 9. Dynamic unload

Before an MVP release, perform all of the following:

1. run the Plugin DevKit dynamic plugin inspection;
2. install the local ZIP into a clean sandbox;
3. trigger every effect in two projects and multiple editors;
4. uninstall or update without restart;
5. inspect logs for unload failure or leaked classloader;
6. verify overlays/timers/listeners no longer react;
7. reinstall and repeat to catch duplicate registration.

Any need for `require-restart="true"` is a release blocker and requires architectural review.

## 10. Split-mode posture

The initial artifact is a classic plugin because the modular plugin format remains experimental. Nonetheless:

- all immediate interaction and painting is modeled as frontend work;
- no effect requires backend project state;
- shared core code has no filesystem/PSI dependency;
- project services treat remote/frontend capability as explicit;
- remote support is not advertised until a real frontend/backend run passes.

A future modular migration should split `core`/DTOs into shared, surface/UI code into frontend, and leave the backend module empty unless a concrete feature requires it.

## 11. Architecture tests

- core package dependency test;
- forbidden import and reflection scan;
- one engine service instance across multiple projects;
- no UI installation in headless test mode;
- resource count returns to baseline after editor/project churn;
- dynamic unload/reload does not double listener callbacks;
- every painter is registered under a disposable with a shorter or equal owner lifetime.
