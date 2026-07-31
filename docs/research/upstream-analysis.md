# VSCode Animations Source Analysis

## 1. Analyzed source

- Repository: <https://github.com/BrandonKirbyson/VSCode-Animations>
- Analyzed commit: `cb4282a` (`2.0.7`, 2025-01-03)
- Local research checkout: `D:\_GitHub\10_Cloned\VSCode-Animations`
- License: MIT
- Analysis date: 2026-07-31

This analysis extracts product lessons and risks. MotionFlow does not copy upstream source or assets.

## 2. How the reference extension works

The VS Code extension has two cooperating contexts:

1. the normal extension host reads settings, composes CSS, installs a script path through one of several custom-injection extensions, and sends updated settings/CSS;
2. the injected workbench script runs inside the Electron DOM, adds a `<style>` element, observes DOM mutations, reacts to focus, and draws a canvas caret trail.

Notable mechanisms:

- `src/css.ts` selects compiled CSS files by setting and replaces duration markers;
- `src/install.ts` coordinates Custom CSS and JS Loader, Custom UI Style, or APC Customize UI++;
- `src/custom/updateHandler.ts` initializes injected handlers and live updates;
- `src/custom/handlers/tabsHandler.ts` uses `MutationObserver` and CSS classes for tab entrance/movement;
- `src/custom/handlers/focusHandler.ts` applies opacity to selected DOM elements on window blur/focus;
- `src/custom/bonus/cursorAnimation.ts` appends a canvas, polls cursor DOM elements, and runs a `requestAnimationFrame` trail loop;
- SCSS implements command-palette, tab, list-item, scroll, and layout transitions.

## 3. Feature inventory

| Upstream feature | Upstream mechanism | MotionFlow disposition |
| --- | --- | --- |
| Smooth layout mode | CSS transitions on DOM `top/left/width/height` | Do not port; Swing layout is platform-owned |
| Command palette | CSS animation on `.quick-input-widget` | Defer; no supported global lifecycle hook identified |
| Tabs | DOM mutation observer + CSS classes | Replace with supported selected-editor context cue; research tab geometry separately |
| Active list item | CSS transform on focused rows | Consider later only with supported component events |
| Scrolling item entrances | CSS animation on Monaco rows | Do not port initially; virtualized Swing lists make global animation risky |
| Focus dimming | DOM opacity updates | Implement optional input-transparent frame overlay, off by default |
| Cursor trail | DOM cursor polling + canvas | Implement event-driven editor painter; no polling |
| Live configuration | Regenerated CSS sent through DOM messenger | Implement immutable policy snapshots and live platform settings |
| Custom CSS | User edits injected stylesheet | Explicit non-goal; arbitrary code/UI mutation is unsafe |

## 4. Strengths worth preserving

- Effects are individually configurable.
- Durations can be tuned globally and per feature.
- Live updates reduce restart friction.
- The extension makes animation categories understandable through visual examples.
- It offers an immediate master enable/disable control.
- The cursor trail demonstrates strong demand for spatial continuity.

## 5. Risks not to reproduce

### Injection dependency

The extension requires another extension to inject JavaScript/CSS and warns that custom injection can cause a VS Code installation-corrupt message. MotionFlow must install like a normal Marketplace plugin and never modify IDE files.

### DOM selector fragility

CSS class names and exact DOM structures are implementation details. MotionFlow must use documented event and painting APIs and degrade when a surface is unavailable.

### Polling and perpetual loops

The reference cursor handler polls cursor elements every 500 ms and maintains a `requestAnimationFrame` loop while active. MotionFlow uses caret events and runs its single scheduler only while a timeline exists.

### Incomplete cleanup risk

DOM observers, resize observers, intervals, and frame loops require explicit teardown. MotionFlow ties every listener/painter/timeline to a platform `Disposable` and tests dynamic unload.

### Broad transitions

Animating all elements with inline geometry can create layout lag or visual conflict. MotionFlow renders bounded cues without altering platform geometry.

### Limited tests

The upstream test suite contains a sample assertion rather than behavior coverage. MotionFlow defines deterministic, platform, UI, leak, performance, compatibility, and accessibility gates before feature code.

## 6. Product differentiation

MotionFlow aims to be better through engineering qualities rather than a higher animation count:

- native supported APIs and normal installation;
- event-driven effects with zero idle loop;
- one consistent motion engine and policy;
- explicit Reduced Motion and Power Save behavior;
- compatibility-aware coexistence with native smooth caret movement;
- strict performance and retained-heap budgets;
- local privacy-preserving diagnostics;
- fail-closed capability adapters;
- traceable requirements and release evidence.

## 7. Attribution decision

The upstream MIT license permits reuse with notice, but this project intentionally uses clean, independent Kotlin/Swing code. `THIRD_PARTY_NOTICES.md` still credits VSCode Animations as inspiration and links the original repository. If any upstream code is later adapted, its copyright and MIT notice must accompany the adapted portion and receive an explicit license review.

