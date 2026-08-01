# MotionFlow Product Requirements Document

| Field | Value |
| --- | --- |
| Product | MotionFlow |
| Document version | 1.0 |
| Status | Approved baseline for implementation |
| Owner | Myang |
| Last updated | 2026-07-31 |
| Target first public release | `0.1.0` |
| Minimum platform | IntelliJ Platform 2025.3, build `253` |

## 1. Executive summary

MotionFlow is a native, configurable motion system for IntelliJ Platform IDEs. It makes state changes easier to follow and the interface feel more responsive through restrained editor and workbench effects. It is inspired by VSCode Animations, but it is an independent implementation designed for the IntelliJ Platform's Swing/JBR architecture.

The product must not inject code, patch IDE files, depend on internal APIs, re-parent platform components, or compromise input latency. Every effect is optional, disposable, theme-aware, and governed by a shared motion engine. Reduced motion, Power Save Mode, remote development, multi-monitor scaling, and older hardware are product requirements rather than afterthoughts.

The first release focuses on effects that can be implemented with supported platform APIs:

- a caret-height editor trail and a focus-flow cue between visible split editor panes, while complementing native smooth-caret behavior;
- a short editor click ripple and active-line pulse;
- a moving editor-context indicator when the selected file changes;
- tool-window focus cues and optional inactive-frame dimming;
- global enable/disable, motion profiles, live settings, reduced motion, and diagnostics.

Effects that require unsupported access to JetBrains-owned component internals—true editor-tab layout animation, arbitrary popup interception, or animated tool-window geometry—remain outside the stable feature set until a public API exists.

## 2. Problem and opportunity

### 2.1 User problem

Large IDE interfaces change state frequently: the caret jumps, editors switch, tool windows open, focus moves, and contextual surfaces appear. Many transitions are instantaneous. Users can lose visual context even when the underlying action is fast.

Existing options do not solve the full problem:

- IntelliJ Platform 2026.1 adds native smooth caret movement, but users on 2025.3 do not have it and the feature does not cover broader workbench transitions.
- Existing Marketplace plugins generally address one narrow effect, such as smooth scrolling or a caret trail.
- A DOM/CSS injection technique suitable for VS Code cannot be safely transferred to Swing-based IDEs.

### 2.2 Product opportunity

MotionFlow can provide a coherent motion language across supported IDE surfaces while being safer than ad hoc component patching. A shared engine enables consistent easing, cancellation, frame scheduling, accessibility, and performance measurement.

### 2.3 Governing product invariant

> Motion may clarify an IDE state transition, but it must never delay, block, reorder, or alter that transition.

The platform remains the source of truth. MotionFlow only renders non-interactive visual feedback after observing platform events.

## 3. Evidence and constraints

### 3.1 Reference extension findings

VSCode Animations 2.0.7 generates CSS from settings, injects CSS and JavaScript through a separate loader, observes DOM mutations for tab changes, polls DOM cursor elements, and draws a trail on a canvas. Its principal features are smooth layout transitions, command-palette effects, tab effects, list-item effects, scrolling effects, focus dimming, and a cursor trail.

Those product ideas are useful. Its implementation mechanism is not portable because IntelliJ Platform IDEs use Swing/JBR rather than an Electron DOM. See [the source analysis](research/upstream-analysis.md).

### 3.2 Platform constraints

- UI mutations and painting occur on Swing's Event Dispatch Thread (EDT); animation work on the EDT must be small and bounded.
- Resources and listeners must be tied to the shortest applicable `Disposable` lifetime.
- Stable releases must pass Plugin Verifier with zero internal-API, override-only-API, and compatibility violations.
- A plugin must not materially degrade IDE performance or interfere with product behavior to pass Marketplace review.
- The New UI is the supported visual baseline. Classic UI-specific tuning is out of scope.
- Split Mode separates frontend UI from the backend project model. Motion rendering belongs on the frontend; no animation should require PSI, indexes, VFS traversal, or backend RPC.

### 3.3 Legal and licensing constraints

- MotionFlow is an independent implementation. No source code or art is copied from VSCode Animations.
- The upstream project is acknowledged as product inspiration in `THIRD_PARTY_NOTICES.md`.
- The repository is licensed under Apache License 2.0.
- Marketing must not imply affiliation with Brandon Kirbyson, Microsoft, JetBrains, or the VSCode Animations project.

## 4. Target users and jobs to be done

### 4.1 Primary users

1. **Visual-orientation seeker**: uses many editor tabs and tool windows and wants state changes to be easier to follow.
2. **Aesthetic customizer**: wants a polished IDE and meaningful control over motion style and intensity.
3. **Keyboard-heavy developer**: moves rapidly through files and locations and needs effects that remain subtle during bursts.
4. **Performance-sensitive developer**: values visual feedback but will uninstall anything that increases typing latency or idle CPU.
5. **Motion-sensitive user**: requires an explicit, reliable reduced-motion mode.

### 4.2 Jobs to be done

- When the active editing context changes, help me see where focus moved without slowing the change.
- When I move the caret or click in the editor, preserve spatial continuity without obscuring code.
- When I switch tool windows or IDE frames, make focus obvious without reducing readability.
- Let me choose a coherent motion style once instead of tuning unrelated effects individually.
- Let me disable all motion instantly if it distracts me or affects performance.

## 5. Goals, non-goals, and principles

### 5.1 Goals

- **G-01 Native integration:** use supported IntelliJ Platform and JDK APIs only in stable features.
- **G-02 Coherent motion:** centralize timing, easing, cancellation, rendering, and motion policy.
- **G-03 Immediate control:** apply settings without restarting the IDE whenever the platform permits.
- **G-04 Performance safety:** produce no idle animation loop and stay within explicit frame-time budgets.
- **G-05 Accessibility:** provide reduced motion, safe defaults, contrast-aware colors, and no flashing.
- **G-06 Broad compatibility:** support platform-only products beginning with 2025.3 and verify representative current releases.
- **G-07 Diagnosability:** make it possible to identify active effects, skipped frames, adaptive degradation, and compatibility fallbacks locally.
- **G-08 Privacy:** collect and transmit no telemetry in `0.x` releases.

### 5.2 Non-goals

- Reproducing every VSCode Animations effect one-for-one.
- Replacing JetBrains themes, icons, or core interaction design.
- Animating text edits, completion ordering, code analysis, or other semantic behavior.
- Modifying IDE installation files, using Java agents, bytecode instrumentation, reflection, or undocumented registry keys.
- Animating geometry owned by platform layout managers.
- Guaranteeing support for third-party IDEs whose Swing hierarchy or JBR behavior materially diverges from the IntelliJ Platform baseline.
- Shipping a custom scripting language or arbitrary user-supplied code/CSS.
- Cloud synchronization or analytics in the initial product.

### 5.3 Product principles

1. **Clarity over spectacle.** Defaults are subtle and short.
2. **Observation over interception.** Subscribe to completed state changes; do not intercept actions or input.
3. **Overlay over mutation.** Paint non-interactive overlays rather than modifying platform components.
4. **One clock while active, none while idle.** All concurrent effects share one scheduler.
5. **Latest intent wins.** A new transition supersedes an obsolete one instead of building a queue.
6. **Capability-driven behavior.** Unsupported surfaces degrade to no effect, not to reflection or a crash.
7. **User control wins.** Disabled or reduced motion takes effect immediately.

## 6. Terminology

- **Effect:** one visual response to a platform event, such as a click ripple.
- **Motion profile:** named defaults for duration, distance, easing, and enabled effects.
- **Motion engine:** the scheduler, timeline, easing, cancellation, and repaint coordinator shared by all effects.
- **Surface adapter:** code that translates a public platform event into engine input and renders on a specific UI surface.
- **Capability:** a runtime fact indicating whether a feature is safe and supported in the current IDE/product/session.
- **Reduced motion:** a policy that removes spatial movement and uses no motion or a brief opacity cue.
- **Adaptive degradation:** automatically lowering fidelity when measured animation cost exceeds a budget.
- **Stable feature:** enabled through documented APIs and covered by compatibility verification.
- **Lab feature:** opt-in experiment with a narrower support promise; never enabled by default.

## 7. Scope by release

### 7.1 `0.1.0` MVP

| Area | Capability | Default |
| --- | --- | --- |
| Core | Shared frame clock, timelines, easing, cancellation, repaint coalescing | On when needed |
| Policy | Global enable, profiles, reduced motion, Power Save Mode behavior | Balanced profile |
| Editor | Silky frame-sampled caret trails | On for all carets up to the per-editor cap; primary-only and alternative styles remain selectable |
| Editor | Caret flow between visible split panes | On with the caret-trail setting |
| Editor | Click ripple | On |
| Editor | Active-line pulse after large navigation jumps | On |
| Context | Active-editor transition indicator | On |
| Workbench | Tool-window focus cue | On |
| Window | Inactive-frame dim overlay | Off |
| Settings | Searchable application-level settings with live preview | Available |
| Operations | Toggle action and diagnostics action | Available |

### 7.2 `0.2.x` beta expansion

- remote-development frontend validation and packaging decision;
- richer theme-derived palettes;
- per-effect custom timing and color controls;
- optional local performance HUD for developers;
- safe tree/list selection cues if public component events prove robust across products.

### 7.3 Future candidates

- popup reveal effects if a supported global popup lifecycle API becomes available;
- true editor-tab geometry transitions if the platform exposes stable tab layout hooks;
- tool-window resize interpolation if an API explicitly permits it;
- project-level overrides and shareable profile files;
- optional, consent-based anonymous telemetry only after a separate privacy review and PRD amendment.

## 8. User experience requirements

### 8.1 First run

No onboarding dialog appears automatically. MotionFlow starts with the Balanced profile and a conservative effect set. A single non-modal notification may be shown after the first project opens, with actions to open settings or disable MotionFlow. It must not recur after dismissal.

### 8.2 Settings experience

Settings live under **Settings | Appearance & Behavior | MotionFlow** and are application-scoped.

The page contains:

1. master enable switch;
2. motion profile selector;
3. reduced-motion switch;
4. effect groups with individual switches;
5. intensity, duration scale, trail length, trail style, caret coverage, and trail visibility controls;
6. Power Save Mode behavior;
7. diagnostics summary and reset action;
8. an isolated preview component that demonstrates the current policy without touching the live IDE surface.

Changing a value updates the preview immediately. Applying settings updates running effects without an IDE restart. Cancel restores the pre-dialog persisted state.

### 8.3 Profiles

| Profile | Intent | Duration scale | Distance/intensity | Default effect policy |
| --- | --- | ---: | ---: | --- |
| Subtle | Minimum distraction | `0.75` | `0.65` | No inactive-frame dimming |
| Balanced | Default clarity | `1.00` | `1.00` | MVP defaults |
| Expressive | More visible motion | `1.20` | `1.25` | All stable effects |
| Custom | User-edited values | User-set | User-set | User-set |
| Reduced Motion | Accessibility | `0` or fade ≤ 80 ms | `0` | Spatial effects replaced or disabled |

Selecting a named profile replaces the current effective values. Editing a profile-derived field changes the selection to Custom. Reduced Motion is an independent override and is never silently turned off by profile selection.

## 9. Functional requirements

### 9.1 Core and control

- **MF-F-001:** The user can enable or disable MotionFlow globally. Disabling cancels all timelines, removes painters/listeners that are only needed while enabled, clears overlays, and stops the clock within one EDT turn.
- **MF-F-002:** The user can choose Subtle, Balanced, Expressive, or Custom profiles.
- **MF-F-003:** Settings apply without restart and persist across IDE restarts.
- **MF-F-004:** The action **MotionFlow: Toggle Motion** is available through Find Action and reports the resulting state.
- **MF-F-005:** The action **MotionFlow: Open Settings** opens the product settings page.
- **MF-F-006:** The action **MotionFlow: Copy Diagnostics** copies a redacted local diagnostics snapshot; it never includes file names, paths, project names, document text, or identifiers.
- **MF-F-007:** Enabling or disabling one effect cannot recreate global listeners or timers more than once.
- **MF-F-008:** An invalid persisted value is clamped to a documented safe range and logged once at warning level.

### 9.2 Motion engine

- **MF-E-001:** All effects use a monotonic time source.
- **MF-E-002:** At most one Swing frame timer exists per application process.
- **MF-E-003:** The timer runs only while at least one timeline is active.
- **MF-E-004:** Repaint requests targeting the same component in one frame are coalesced.
- **MF-E-005:** Timelines support start, retarget, cancel, finish, and owner disposal.
- **MF-E-006:** A retargeted transition starts from its current interpolated value, not its original start value.
- **MF-E-007:** Easing functions are pure, deterministic, clamped to `[0,1]`, and unit-tested at boundary values.
- **MF-E-008:** Rendering receives normalized progress and immutable effect state; it does not query PSI, indexes, VFS, or the network.
- **MF-E-009:** If a frame is late, the engine advances by elapsed time and drops obsolete frames instead of replaying them.
- **MF-E-010:** The engine exposes local counters for active timelines, frame duration, dropped frames, and adaptive-degradation state.

### 9.3 Editor effects

- **MF-ED-001:** Caret movement events may produce a non-interactive trail rendered over the editor content component. In standard themes, horizontal travel uses the current logical caret height while other angles use the motion-normal projection of the native vertical caret footprint; vertical travel therefore remains a narrow aligned stem. High Contrast may use a thinner solid indicator to preserve readability.
- **MF-ED-002:** The default all-carets policy gives every admitted caret independent bounded origin, sample, visual, and timeline state. The primary caret is admitted first, total animated carets are capped at eight per editor, and excess secondary carets fail closed. Add/remove events initialize or dispose the corresponding state immediately. A primary-only policy remains selectable.
- **MF-ED-003:** Typing and caret movement are never delayed or intercepted.
- **MF-ED-004:** Trail points are bounded by the configured maximum and expire by age; no unbounded history is retained.
- **MF-ED-005:** The trail is clipped to the visible editor viewport and hidden when the editor is not showing, disposed, in a password field, or unsuitable for coordinate mapping.
- **MF-ED-006:** On platform versions with native smooth caret movement, MotionFlow detects available behavior conservatively. It may render a short trail, but must not implement a second competing caret interpolation. The settings page explains the effective mode.
- **MF-ED-007:** A primary-button click in an eligible editor may create a short ripple centered at the editor-local click coordinate.
- **MF-ED-008:** Click ripples never consume mouse events and are disabled during drag, selection expansion, or reduced motion.
- **MF-ED-009:** A navigation jump of at least the configured visual-line threshold may create a brief active-line pulse at the destination.
- **MF-ED-010:** Routine one-character caret movement does not trigger the active-line pulse.
- **MF-ED-011:** Editor effects adapt to scale changes and HiDPI transforms without storing device-pixel coordinates as logical coordinates.
- **MF-ED-012:** Editor release immediately disposes its painter, listeners, and timelines.
- **MF-ED-013:** When focus moves between two showing editor components in the same root pane, MotionFlow may animate a caret-sized, non-interactive flow cue from the previous caret position to the destination. It never moves either platform caret, crosses top-level frames, or runs when the caret trail is disabled by policy.
- **MF-ED-014:** Standard-theme trail geometry tapers to its origin, uses a transparent-to-accent gradient, and remains valid for horizontal, vertical, and diagonal arrival vectors. Its leading cross-section equals the motion-normal projection of a vertical caret silhouette, avoiding a full-height orthogonal bar during vertical travel. The default Silky style adds a bounded high-visibility center core; Speed lines and Rounded ribbon remain user-selectable alternatives. High Contrast keeps its solid fallback.
- **MF-ED-015:** Every local trail starts at the immediately previous accepted coordinate of the same caret, advances through bounded per-frame interpolated samples, settles at that caret's current coordinate, and fades after arrival. A new movement replaces only that caret's stale history. During cross-editor focus changes, destination local trails are suppressed/re-anchored so exactly one shared-root cue begins at the source editor's last primary-caret coordinate.
- **MF-ED-016:** The user can select caret coverage (`All carets`, capped at eight, or `Primary only`) and trail visibility (`Soft`, `Balanced`, `Clear`, or `Bright`). Balanced profile defaults to All carets and Clear. Visibility scales only bounded trail alpha/width, applies live, preserves High Contrast simplification, and cannot exceed painter alpha caps.

### 9.4 Context and workbench effects

- **MF-WB-001:** A selected-file change may render a short context-transition cue without modifying editor tabs or delaying selection.
- **MF-WB-002:** If a stable tab rectangle cannot be obtained through supported APIs, the cue degrades to an editor-edge indicator rather than traversing internal component types.
- **MF-WB-003:** Tool-window activation may render a focus cue attached to the tool-window component through a disposable painter.
- **MF-WB-004:** Tool-window resizing, activation, and visibility are never intercepted or replayed.
- **MF-WB-005:** Optional inactive-frame dimming uses a non-interactive overlay and restores full opacity immediately on focus gain or global disable.
- **MF-WB-006:** Dimming excludes modal dialogs, popup windows, presentation mode, screen-sharing-safe mode if one is exposed, and unsupported window types.
- **MF-WB-007:** Dimming defaults to off and is disabled by Reduced Motion and Power Save Mode.
- **MF-WB-008:** Multiple project frames maintain independent focus state but share application settings and the application clock.

### 9.5 Capability and failure behavior

- **MF-C-001:** Each effect declares required capabilities and can be disabled independently at runtime.
- **MF-C-002:** An unsupported capability produces a no-op and a diagnostics reason, not an exception shown to the user.
- **MF-C-003:** An unexpected rendering exception disables only the failing effect for the current session after one retry; it does not disable the whole plugin.
- **MF-C-004:** Repeated errors are rate-limited in logs.
- **MF-C-005:** Headless and unit-test environments do not install UI listeners or timers.
- **MF-C-006:** Dynamic plugin unload leaves no painter, listener, timer, AWT hook, or classloader reference behind.

## 10. Accessibility requirements

- **MF-A-001:** Reduced Motion is visible at the top level of settings and can be toggled without opening advanced controls.
- **MF-A-002:** Reduced Motion cancels active spatial timelines immediately and replaces allowed feedback with no effect or a fade no longer than 80 ms.
- **MF-A-003:** No effect flashes more than three times per second; the product contains no looping decorative animation.
- **MF-A-004:** Effects do not encode required information solely through color or motion.
- **MF-A-005:** Overlay colors derive from theme colors when possible and meet a target contrast ratio of at least 3:1 against adjacent backgrounds for non-text indicators.
- **MF-A-006:** High-contrast themes force simplified solid indicators and disable translucent dimming.
- **MF-A-007:** Settings controls have accessible names, descriptions, keyboard navigation, and logical focus order.
- **MF-A-008:** The preview has a pause control and never auto-loops indefinitely.
- **MF-A-009:** If reliable operating-system reduced-motion detection is unavailable through supported APIs, MotionFlow does not claim automatic detection; explicit user control remains authoritative.

## 11. Performance and resource requirements

Measurements use a release build, a warmed IDE, a representative 100k-line project, 150% scaling, and the Balanced profile. Baselines and plugin-on runs use the same IDE, JBR, project, and scripted interactions.

- **MF-P-001:** Idle CPU attributable to MotionFlow is statistically indistinguishable from disabled state; specifically, no periodic timer remains active while no effect is running.
- **MF-P-002:** Median MotionFlow update-and-paint preparation time is ≤ 0.50 ms per active frame on the reference machine.
- **MF-P-003:** p95 MotionFlow update-and-paint preparation time is ≤ 1.50 ms; p99 is ≤ 3.00 ms.
- **MF-P-004:** Added end-to-end typing latency at p95 is ≤ 2 ms in a controlled benchmark and no statistically significant regression is accepted without investigation.
- **MF-P-005:** Retained heap after opening and closing 100 editors differs from disabled baseline by ≤ 8 MiB after forced collection, with no retained disposed `Editor` instances attributable to MotionFlow.
- **MF-P-006:** One application process owns at most one active scheduler and one small metrics ring buffer capped at 512 samples.
- **MF-P-007:** The engine caps concurrent timelines at 64 globally and 8 per surface. Newer equivalent timelines retarget or replace older ones.
- **MF-P-008:** If p95 frame cost exceeds 2 ms for 120 sampled active frames, adaptive degradation reduces particle count and shadow/alpha complexity. If it remains above budget, nonessential effects pause for the session and diagnostics explain why.
- **MF-P-009:** Power Save Mode cancels nonessential effects and prevents new ones. The user may choose `Disable all`, `Reduced motion`, or `Ignore`, with `Disable all` as default.
- **MF-P-010:** Logging on animation hot paths is disabled unless debug diagnostics are explicitly enabled.

## 12. Privacy and security requirements

- **MF-S-001:** MotionFlow sends no network requests and bundles no analytics or crash-reporting SDK.
- **MF-S-002:** MotionFlow reads no document text and persists no file, project, caret, or usage history.
- **MF-S-003:** Diagnostics exclude personally identifiable information and workspace content.
- **MF-S-004:** No arbitrary code, CSS, scripts, or remote assets are loaded.
- **MF-S-005:** No IDE installation file, registry key, JVM argument, or other plugin configuration is modified.
- **MF-S-006:** Stable code uses no reflection, `sun.*`, `jdk.internal.*`, internal IntelliJ APIs, Java agents, or bytecode transformation.
- **MF-S-007:** Dependencies are minimized, pinned through build files, and reviewed for license and vulnerability implications.
- **MF-S-008:** Release artifacts are built in CI, checksummed, signed before Marketplace publication, and associated with source tags.

## 13. Compatibility requirements

### 13.1 Supported baseline

- IntelliJ Platform build `253` / 2025.3 or newer.
- Java/JBR 21 baseline for 2025.3–2026.1; builds targeting 2026.2+ must account for the platform's Java 25 requirement in CI and Plugin Verifier.
- Windows 11, current macOS, and two current Ubuntu LTS releases.
- 100%, 125%, 150%, 200%, and mixed-monitor scale factors where the OS supports them.
- Light, Dark, High Contrast, and a representative third-party theme.
- IntelliJ IDEA Community/Ultimate, PyCharm, WebStorm, GoLand, CLion, PhpStorm, RubyMine, DataGrip, Rider frontend where platform dependencies permit, and Android Studio only after a dedicated compatibility pass.

### 13.2 Compatibility policy

- The plugin declares only `com.intellij.modules.platform` unless implementation proves a narrower dependency is required.
- Stable code must compile without suppressing internal-API inspections.
- `until-build` remains open only while latest-release Plugin Verifier and smoke tests pass; a breaking EAP finding triggers an urgent compatibility issue or a release-range cap.
- Product-specific failures disable the relevant effect through a capability adapter.
- Classic UI is not an acceptance target.
- Remote development is architectural scope from day one but a release claim only after frontend tests pass.

## 14. Observability and evaluation

All observability is local.

### 14.1 Diagnostics snapshot

The copyable snapshot contains:

- plugin and IDE versions;
- OS name and architecture, but no host name;
- enabled profile and reduced-motion/Power Save Mode policy;
- capability states and disable reasons;
- active/peak timeline counts;
- rolling p50/p95/p99 engine cost;
- dropped-frame and adaptive-degradation counts;
- last five sanitized error categories without stack traces or paths.

### 14.2 Product success measures

Because initial releases contain no telemetry, product success is evaluated with:

- scripted performance and compatibility gates;
- opt-in user interviews and issue templates;
- Marketplace rating/review themes and support issue rate;
- beta survey responses collected outside the plugin;
- percentage of supported test matrix cells passing before release.

Targets for the first stable release:

- 100% of mandatory compatibility cells pass;
- zero known data-loss, input-interception, memory-leak, or IDE-freeze defects;
- zero Plugin Verifier internal-API or compatibility violations;
- at least 8 of 10 beta participants rate the Balanced profile as clear and non-disruptive;
- at least 8 of 10 beta participants can find global disable and Reduced Motion within 15 seconds.

## 15. Acceptance criteria and release gates

### 15.1 MVP feature gate

Every MVP requirement marked `MF-F`, `MF-E`, `MF-ED`, `MF-WB`, or `MF-C` must have:

1. an implementation owner;
2. a linked unit, integration, UI, performance, or manual test;
3. a failure-mode test or documented manual procedure;
4. evidence in the traceability matrix;
5. no unresolved P0/P1 defect.

### 15.2 Quality gate

- `./gradlew check` passes.
- `./gradlew verifyPluginProjectConfiguration` passes.
- `./gradlew verifyPluginStructure` passes.
- `./gradlew verifyPlugin` passes for the configured IDE matrix.
- Plugin DevKit dynamic-unload inspection passes.
- Qodana/IDE inspections report no internal or unstable API use in stable packages.
- Automated leak test and manual unload/reload test pass.
- Performance budgets pass on Windows and at least one of macOS/Linux.
- Accessibility checklist passes with keyboard-only use, high contrast, and Reduced Motion.

### 15.3 Marketplace gate

- Original 40×40 SVG icon and accurate English description.
- Name is unique, under 30 characters, and contains no prohibited product/vendor terms.
- Public source, issue tracker, license, privacy statement, and EULA/license link are ready.
- Screenshots/GIFs show real behavior on the default IDE theme and make no unsupported claims.
- Signed distribution matches the source tag and CI checksum.

## 16. Delivery milestones

| Milestone | Outcome | Exit signal |
| --- | --- | --- |
| M0 Foundation | Buildable repository, PRD, plans, research, CI | `check` and structure validation pass |
| M1 Engine | Clock, timeline, easing, repaint coordinator, policy | Deterministic tests and idle-zero proof |
| M2 Editor alpha | Caret trail, click ripple, active-line pulse | Editor lifecycle and latency gates pass |
| M3 Workbench alpha | Editor-context and tool-window cues | Cross-project/frame lifecycle tests pass |
| M4 Product beta | Settings, profiles, diagnostics, accessibility | UX and reduced-motion acceptance pass |
| M5 Compatibility beta | OS/product/scale/split-mode verification | Matrix and Plugin Verifier pass |
| M6 `0.1.0` | Signed Marketplace-ready release | All release gates pass |

## 17. Risks and mitigations

| Risk | Likelihood | Impact | Mitigation |
| --- | --- | --- | --- |
| Stable APIs cannot expose a desired surface | High | Medium | Capability-based no-op; use editor-edge cue; keep unsupported effects out of stable scope |
| Animation work causes EDT latency | Medium | Critical | One clock, bounded state, repaint coalescing, frame dropping, hard performance gates |
| Platform release changes component behavior | Medium | High | Avoid implementation classes; run latest verifier and smoke matrix; adapters fail closed |
| Painter/listener leaks editors or projects | Medium | High | Shortest-lifetime `Disposable`, editor churn test, dynamic unload test |
| Native 2026.1 caret motion conflicts with trail | Medium | Medium | Detect capability, never move the platform caret, document effective mode |
| Effects are distracting or inaccessible | Medium | High | Subtle defaults, Reduced Motion, instant toggle, no loops/flashing |
| Multi-monitor scaling misplaces overlays | Medium | Medium | Keep logical coordinates per component; conversion tests and manual mixed-DPI matrix |
| Scope expands toward unsupported DOM-like behavior | High | High | Public-API invariant, ADR, reviewer checklist, Lab isolation |
| Marketplace rejects name/assets/behavior | Low | High | Follow current approval guidelines and run a pre-submission checklist |

## 18. Open questions and default decisions

| Question | Default until evidence changes it |
| --- | --- |
| Should the plugin be modular for Split Mode immediately? | No. Modular plugins are still experimental; keep UI/business boundaries explicit and migrate after frontend validation. |
| Should native 2026.1 smooth caret be disabled? | No. Never change another product setting automatically. Complement or skip. |
| Should inactive-frame dimming be enabled by default? | No. It changes the whole frame and may reduce readability. |
| Should arbitrary custom easing strings be accepted? | No. Ship reviewed presets first; a numeric cubic-bezier editor can be considered later. |
| Should settings be per project? | No for `0.1.0`; visual preference is application-scoped. |
| Should user telemetry be added? | No. A separate privacy review and explicit opt-in are required. |
| Should unsupported popup/tab effects use reflection? | No. Wait for public APIs or keep a strictly isolated developer experiment out of releases. |
| Should Android Studio be advertised at launch? | Only after Plugin Verifier and a manual smoke test against the current stable version. |

## 19. Requirement traceability

Implementation ownership and verification mappings live in [the traceability matrix](traceability-matrix.md). Detailed engineering decisions live under [`implementation/details/`](implementation/details/), and the executable work sequence lives in [`implementation/implement.md`](implementation/implement.md).

## 20. Source references

Research claims and access dates are recorded in [references.md](research/references.md). The most consequential primary sources are the IntelliJ Platform threading model, disposer guidance, Plugin Verifier/compatibility documentation, split-mode guidance, and current Marketplace approval criteria.
