# IntelliJ Platform Feasibility Assessment

## 1. Conclusion

A useful, Marketplace-appropriate animation plugin is feasible, but only as a set of supported event-driven overlays. A literal port of DOM/CSS behavior is not feasible or desirable.

The stable product promise should be outcome-based—visual continuity and focus cues—not implementation-shaped promises such as “animate every tab and popup.”

## 2. Feasibility tiers

| Effect | Feasibility | Supported path | Decision |
| --- | --- | --- | --- |
| Caret trail | High | caret events, editor coordinates, disposable painter | MVP |
| Click ripple | High | editor mouse events, disposable painter | MVP |
| Navigation/active-line pulse | High | caret events, visual-line delta, painter | MVP |
| Selected-editor context cue | High | `FileEditorManagerListener`, selected component painter | MVP |
| Tool-window focus cue | Medium-high | `ToolWindowManagerListener`, component painter | MVP |
| Inactive-frame dim overlay | Medium | window focus + glass-pane painter | MVP, off by default |
| True moving editor-tab indicator | Medium/unknown | public tabs API must be verified | Time-boxed spike, fallback required |
| Tree/list selection glide | Medium/unknown | component-specific supported listeners needed | `0.2.x` candidate |
| Global popup/Search Everywhere reveal | Low | no supported global lifecycle hook identified | Deferred |
| Smooth tool-window/splitter geometry | Low/risky | would interfere with layout ownership | Rejected for stable scope |
| Arbitrary custom animation scripting | Technically possible, unsafe | would expand security/API surface | Rejected |

## 3. Platform mechanisms

### Editor events

The SDK exposes editor and caret event APIs, including disposable listener registration. This supports event-driven caret and click effects without polling or input interception.

### Selected editors

`FileEditorManagerListener` reports file open/close/selection changes. It supports a destination-context cue even if editor-tab geometry remains unavailable.

### Tool windows

`ToolWindowManagerListener` reports registration/show/state changes. A cue can attach to the tool-window component without animating its bounds.

### Painting overlays

`IdeGlassPaneUtil.installPainter` provides a public helper that registers a `Painter` under a `Disposable` in current platform source. Every usage still requires Plugin Verifier across the baseline matrix.

### Settings and services

Platform services, `PersistentStateComponent`, and Kotlin UI DSL support application settings and live policy updates. Declarative listeners reduce startup cost and are preferred when available.

## 4. Threading implications

Swing UI callbacks and painting run on the EDT. Expensive work on the EDT freezes the IDE. MotionFlow therefore:

- confines a small mutable engine to EDT;
- performs no PSI/VFS/index work;
- uses one timer only while active;
- computes by elapsed monotonic time and drops missed visual frames;
- coalesces repaint regions;
- bounds all histories and concurrent effects;
- establishes tail-latency and retained-heap gates.

## 5. Lifecycle implications

The platform disposer model is a strong fit for overlays. An editor-local disposable can own listeners, painter, and timelines. Project services can own tool-window/frame effects. Application services own the scheduler and settings.

Dynamic unload is feasible only if every registration follows that tree. Global hooks, static component references, and perpetual timers would threaten unload and are excluded.

## 6. Compatibility implications

- Platform 2024.2+ requires IntelliJ Platform Gradle Plugin 2.x; MotionFlow uses 2.x.
- The minimum is 2025.3/build 253 with Java 21; default builds use the latest 2025.3 patch rather than the initial 2025.3.1 release.
- Platform 2026.2+ uses Java 25, so latest-target CI must account for its runtime.
- Plugin Verifier is required for binary/internal-API checks.
- UI logic should be frontend-local in Split Mode; modular packaging remains a later decision because the format is experimental.
- 2026.1 native smooth caret movement changes the best default but not the feasibility of a non-intercepting trail.

## 7. Marketplace implications

Current approval criteria reject internal API usage, significant performance degradation, security/privacy problems, and disruptive interference with product behavior. They also constrain name/logo/description. “MotionFlow” avoids prohibited product/vendor terms and the scaffold includes an original 40×40 SVG.

The public-API/no-geometry-mutation architecture is therefore not merely style; it is part of product approval feasibility.

## 8. Research spikes before implementation commitment

1. Verify `IdeGlassPaneUtil.installPainter` on minimum/current/latest targets.
2. Prototype caret coordinate/painter lifecycle in standard and diff editors.
3. Determine native smooth-caret coexistence behavior on 2026.1+ without reading internal settings.
4. Time-box public editor-tab geometry research and document fallback.
5. Verify frame-local focus events and overlay exclusion around modal dialogs.
6. Run a classic plugin in Split Mode to decide whether remote support is disabled or a modular migration is required.

Each spike produces a small reproducer, verifier result, and decision. A spike is not shipped by default.
