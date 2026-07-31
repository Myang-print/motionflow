# Workbench Effects Details

## 1. Scope

Workbench effects provide orientation when the selected editor, active tool window, or active IDE frame changes. They render non-interactive cues after state changes and never animate platform-owned component geometry.

## 2. Selected-editor context cue

### 2.1 Event source

Use project-level `FileEditorManagerListener.selectionChanged`. A declarative project listener is preferred for lazy creation. The listener remains stateless and forwards to the project service.

### 2.2 Stable implementation

The required stable effect is an editor-context cue, not a promise to move a tab.

On selection change:

1. resolve the newly selected `FileEditor` component through supported APIs;
2. reject disposed, non-showing, or unsupported components;
3. attach/use a painter on that selected content surface;
4. animate a short edge accent or soft outline for 140–220 ms;
5. retarget or cancel if selection changes again.

This makes destination context visible without needing the editor-tab component's rectangle.

### 2.3 Tab-geometry research spike

Time-box a spike to determine whether a supported `JBTabs`/`JBEditorTabs` API exposes the selected editor-tab bounds across the target versions and products.

Acceptance for promotion:

- symbol is public and not internal/experimental/scheduled for removal;
- no traversal by class-name string or implementation class;
- no component re-parenting or layout/UI delegate changes;
- correct under pinned/preview tabs, split editors, hidden tabs, theme and scale changes;
- Plugin Verifier passes the full matrix.

If any criterion fails, close the spike and retain the editor-edge cue. Reflection is not a fallback.

## 3. Tool-window focus cue

### 3.1 Event source and ownership

Use declaratively registered `ToolWindowManagerListener` at project scope. The listener forwards activation/visibility state to `ProjectEffectsService`, which resolves the public `ToolWindow` component and owns project-lifetime painters.

### 3.2 Rendering

The first stable cue is a short, theme-derived border/edge highlight attached to the activated tool-window component. It does not move, resize, fade, or delay the tool window itself.

Balanced default:

- 160 ms opacity/edge-width cue;
- one active tool-window timeline per project frame;
- latest activation retargets the cue;
- high contrast uses a solid 1–2 logical px accent;
- Reduced Motion uses ≤80 ms opacity or no cue.

### 3.3 Edge cases

- tool window registered after project open;
- unavailable or hidden window;
- detached/floating window;
- window component not yet showing at callback time;
- rapid activation through keyboard shortcuts;
- project close during callback;
- multiple project frames and cloned windows.

A missing component is a normal no-op recorded as a capability reason, not an error.

## 4. Inactive-frame dimming

### 4.1 Product behavior

This effect is off by default. When enabled, losing focus paints a translucent overlay over eligible content. Regaining focus removes it immediately or through a short fade based on policy.

The effect must not call `Window.setOpacity`, because that can behave differently across operating systems and affects native decorations and child windows. A Swing painter provides a narrower, reversible behavior.

### 4.2 Event source

Prefer project frame/window lifecycle APIs that expose focus changes with project ownership. If only AWT focus events are available, register the narrowest window-local listener on the resolved frame and remove it through the project/frame disposable. Do not install a toolkit-wide `AWTEventListener` for MVP.

### 4.3 Exclusions

Do not dim when:

- Reduced Motion or Power Save policy forbids it;
- high contrast is active;
- a modal dialog owned by the frame is active;
- the target is not a standard project frame;
- presentation mode or another platform mode makes the result ambiguous;
- a remote/client environment has not passed a dedicated test;
- capability checks cannot guarantee an input-transparent overlay.

### 4.4 Recovery invariant

Global disable, settings reset, project close, frame dispose, plugin unload, or any effect exception clears the dim overlay immediately. A session must never be left visually dimmed because an exit animation was cancelled.

## 5. Painter strategy

Use the plugin-owned `SurfaceOverlay`, which adds an input-transparent `JComponent` to the target root pane's standard Swing `JLayeredPane`. The target remains in place and retains its layout, border, and UI delegate. The overlay tracks target hierarchy/bounds and is removed by the shortest owner disposable.

`IdeGlassPaneUtil.installPainter` is not used because the 2025.3.6 SDK marks it internal. If a standard layered pane is unavailable, mark the capability unsupported and keep the plugin functional without that effect.

Painter implementations copy `Graphics2D`, clip to the target, draw from immutable state, dispose the copy, and never intercept input.

## 6. Multiple windows and projects

Application settings and engine are shared, but visual owners are not.

Use keys that include a non-persisted project/frame owner token so that:

- activating a tool window in project A does not cancel project B's cue;
- only one frame is treated as active by its own focus events;
- project disposal cancels all its effect keys in one operation;
- diagnostics aggregate counts without storing project identities.

## 7. Unsupported effects

The following remain explicitly excluded from stable implementation:

- interpolating tool-window or splitter bounds;
- animating arbitrary Swing component layout changes;
- globally detecting and animating all IDE popups;
- snapshotting and crossfading entire project frames;
- modifying tab components to preserve closed tabs during exit animation;
- installing custom look-and-feel/UI delegates.

These behaviors either interfere with platform layout/input or lack a supported lifecycle API.

## 8. Test plan

### Automated/platform tests

- selection change to text and non-text editors;
- rapid A→B→C editor selection retarget;
- editor component disposed mid-cue;
- tool-window activation/show/hide/unregister;
- project close and dynamic unload;
- missing/non-showing component capability no-op;
- global disable and Reduced Motion during a cue;
- dim overlay recovery after injected exception;
- independent state for two projects.

### Manual/UI tests

- split editors, preview tabs, diff editors;
- docked, floating, and detached tool windows;
- modal and non-modal dialogs;
- alt-tab and focus switching across multiple project frames;
- Windows/macOS/Linux frame decorations;
- light/dark/high-contrast themes and mixed DPI;
- remote frontend after the feature enters compatibility scope.
