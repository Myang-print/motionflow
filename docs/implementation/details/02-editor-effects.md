# Editor Effects Details

## 1. Scope and invariant

Editor effects illustrate caret, click, and navigation events. They do not replace editor actions, modify document content, add range highlighters, change caret settings, or own scrolling.

All editor resources belong to an `EditorEffectsSession` disposed when `editorReleased` fires or the plugin unloads.

## 2. Editor session lifecycle

Attach flow:

1. `EditorFactoryListener.editorCreated` receives an editor.
2. Reject headless, disposed, non-showing, console/password/unsupported editors through capability rules.
3. Create a child disposable associated with the editor.
4. Install one combined overlay painter on `editor.contentComponent`.
5. Register caret and mouse listeners under the child disposable.
6. Store only bounded effect models and up to eight admitted caret states, with the primary admitted first.

Release flow:

1. cancel all effect keys owned by the session;
2. clear painter state and request a final bounded repaint if showing;
3. dispose the child disposable;
4. remove the session mapping;
5. never access the editor again.

Duplicate attach is an assertion failure in tests and a no-op with a sanitized warning in production.

## 3. Coordinate system

Use supported editor methods to convert caret visual positions into points relative to the editor content component. Keep positions in Swing logical coordinates. Do not manually multiply by display scale.

Before submitting an effect:

- verify editor and component are valid/showing;
- compute point after the platform event updates caret state;
- include scrolling offset through the platform conversion result;
- clamp or reject points outside the visible rectangle plus small antialias padding;
- derive a safely clamped caret stem width from the logical line height when a public per-caret width is unavailable.

Recompute derived dimensions after font, zoom, theme, or graphics-configuration changes. Do not cache device transforms across monitors.

## 4. Combined editor painter

One painter renders all editor effects for a session to reduce registrations and repaint fragmentation. Paint order:

1. active-line pulse behind other editor overlays but above editor content;
2. click ripples;
3. caret trail.

The painter:

- is input-transparent;
- clips to the editor visible rectangle;
- enables only necessary antialiasing hints;
- reads one immutable `EditorOverlayModel` reference;
- performs no service lookup, listener registration, logging, or collection growth;
- restores any changed `Graphics2D` state through a copied graphics context.

## 5. Caret trail

### 5.1 Input

Use caret position change notifications plus the public caret-added and caret-removed callbacks. All carets are eligible by default up to a hard cap of eight per editor; Primary only is selectable. Ignore a change if:

- the policy/effect is disabled;
- Reduced Motion or Power Save policy disables it;
- the editor is not active/showing;
- the event caret is outside the selected coverage or bounded admission set;
- the event caret cannot be mapped safely;
- the move is induced by disposal or an unsuitable component state;
- the new point equals the last accepted point within a small epsilon.

### 5.2 Model

```kotlin
data class TrailPoint(
    val point: Point2D.Float,
    val createdAtNanos: Long,
)

data class CaretTrailModel(
    val points: List<TrailPoint>, // bounded, immutable snapshot
    val caretWidth: Float,
    val caretHeight: Float,
    val color: Color,
)
```

The adapter maintains one bounded deque and one keyed timeline per admitted caret but publishes one immutable compact editor snapshot. Each caret has an independent exact origin, generation counter, and visual. Points expire by maximum age and maximum count. Multiple move events replace only the same caret's current target rather than append unlimited points. The per-editor admission cap matches the engine's eight-timeline surface cap and the primary caret is always selected first.

### 5.3 Rendering

Balanced mode defaults to a silky frame-sampled ribbon, not a polygon joining raw caret events. On every accepted move, the adapter discards stale history, records the immediately previous caret as the exact origin, and advances a visual trail head toward the already-completed platform destination during the travel phase of one finite timeline. The shared 16 ms clock contributes bounded intermediate samples; after arrival, a separate fade phase reduces the complete afterimage smoothly to zero.

Bounded centerline samples produce a tapered polygon: its oldest edge converges to the motion origin and its paint progresses from transparent accent to the strongest bounded accent at the caret. Cross-section thickness is the motion-normal projection of a vertical native-caret silhouette. It reaches logical caret height during horizontal travel, collapses to a clamped 2–4 px caret stem during vertical travel, and blends continuously for diagonals. This removes the full-height horizontal slab that appeared during up/down movement.

The Silky painter adds a narrow, high-alpha antialiased center curve for visibility without a blur or shadow filter. The schema-v4 visibility choices scale bounded trail alpha and width: Soft (`0.78`/`0.86`), Balanced (`1.00`/`1.00`), Clear (`1.25`/`1.10`, default), and Bright (`1.50`/`1.20`). The local base peak is `0.72` before profile and visibility scaling and never exceeds `0.96`; cross-pane alpha never exceeds `0.94`.

The same geometry handles left, right, up, down, and diagonal movement. Consecutive centerline tangents define each polygon edge, so reverse-direction movement does not assume a fixed screen side. The projected footprint prevents vertical trails from becoming orthogonal bars. The leading edge meets the plugin-owned cross-pane caret cue; inside one editor it terminates beneath the native caret without replacing it.

`Silky native trail` is the schema-v3 default persisted style. A schema-v2 `Speed lines` default migrates to Silky once; users may then explicitly choose Speed lines or `Rounded ribbon`, which preserves the earlier caret-height `CAP_ROUND` rendering. High Contrast ignores translucent styles and uses a thinner solid, square-ended indicator. Theme accent/editor foreground provides the base color with bounded alpha.

Avoid expensive blur filters. Level-1 degradation reduces point count and renders simple line segments. High contrast uses a solid, short indicator with no translucent trail.

### 5.4 Native smooth caret coexistence

IntelliJ Platform 2026.1 introduces native smooth caret movement. MotionFlow never interpolates or moves the platform caret. On versions where native motion can be assumed available but its user setting cannot be safely queried:

- keep the trail short and event-driven;
- expose `Native-aware trail` as the effective capability;
- avoid drawing a second opaque caret head;
- let users disable the trail independently;
- document that MotionFlow does not change the IDE's native caret preference.

If testing reveals visual conflict that cannot be detected safely, default the trail off for affected versions and keep click/navigation cues.

### 5.5 Multiple carets

Schema v4 resolves the earlier primary-only deferral with bounded public-API behavior:

- `All carets` is the Balanced default; `Primary only` is the lower-activity option and the Subtle-profile default;
- selection is identity-based, deterministic, primary-first, and capped at eight carets per editor;
- `caretAdded` initializes that caret at its current mapped coordinate without fabricating a trail;
- `caretPositionChanged` updates only the event caret and uses a distinct effect-key channel;
- `caretRemoved` increments its generation, cancels its timeline, clears its samples/visual, and releases the identity mapping immediately;
- excess secondary carets remain native and simply receive no MotionFlow trail;
- all buffers remain capped at 12 samples, and adaptive degradation reduces each active visual to four.

The cap makes worst-case editor state and paint work explicit: at most eight trails × 12 points, sharing the existing active-only scheduler and one editor overlay. Navigation pulse remains primary-caret only.

### 5.6 Split-pane focus flow

Each eligible editor content component owns a standard JDK focus listener under its session lifetime. After focus reaches a different showing editor, the manager first captures the source session's last accepted caret anchor, immediately clears any destination-local stale trail, waits one EDT turn for click/focus caret state to settle, re-anchors the destination again, and starts a temporary shared-root flow only when both editor components still share the same `JRootPane`.

The transition overlay targets the root content component, so its input-transparent caret-height ribbon can cross a Split Right or Split Down divider. A distance-aware 180–420 ms timeline moves a sharp-edged, plugin-owned caret cue toward the destination while the same bounded frame-sample buffer builds a continuous tail behind it. Only this source-to-destination cue runs; a destination editor never contributes its older local caret as a competing origin. The native caret moves immediately under IntelliJ control and is never hidden, delayed, or repositioned.

The flow fails closed when either editor is hidden or disposed, focus changes again before the deferred EDT turn, the components belong to different top-level frames, or Reduced Motion, Power Save policy, global disable, or the caret-trail switch disables spatial motion. A newer focus request replaces the active flow for that root pane, and the temporary overlay is disposed at completion or cancellation.

## 6. Click ripple

### 6.1 Classification

Observe editor mouse events without consuming them. Store press point/time and accept a ripple on primary-button release only if:

- press and release occurred in the same editor session;
- movement is below the platform drag threshold or a conservative logical-pixel threshold;
- elapsed time is below a generous click threshold;
- no popup trigger/right button is involved;
- policy and capability allow it.

This prevents a ripple on selection drag.

### 6.2 Animation

Balanced defaults:

- duration: 180 ms;
- radius: 4 → 18 logical px;
- alpha: 0.26 → 0;
- easing: emphasized decelerate.

At most three ripple timelines may coexist per editor. A fourth replaces the oldest. Reduced Motion disables the ripple rather than flashing a static circle.

## 7. Active-line navigation pulse

Track the last accepted primary-caret visual line and recent input category if a supported signal exists. Trigger when:

- visual-line delta ≥ default threshold 8; or
- the selected editor changes and a valid destination line becomes active.

Do not trigger for:

- ordinary typing with a one-line change;
- IME composition;
- document reflow where caret offset did not represent intentional navigation;
- rapid repeated movement where a current pulse can simply retarget.

Render a narrow theme-derived horizontal wash centered on the destination line for 140 ms. Reduced Motion may replace it with a ≤80 ms non-spatial opacity cue because this effect provides orientation.

## 8. Scrolling, folding, soft wraps, and inlays

- Coordinate mapping uses visual rather than logical position so folding and soft wraps are reflected.
- A scroll event does not cause the plugin to move existing trail points relative to the viewport indefinitely; either map model positions safely or expire/clear the trail on significant scrolling.
- Inline/block inlays may change caret geometry. Clamp dimensions and verify with inlay-heavy files.
- When the caret destination is outside the viewport, wait for a subsequent valid position/showing state or skip the effect; do not force scrolling.

## 9. Special editors

Capability rules must cover:

- diff editors;
- consoles and terminal-backed editors;
- commit message editors;
- settings text fields and one-line editors;
- injected-language editors;
- read-only previews;
- editors in dialogs;
- remote frontend editors.

Default: support standard text editors; fail closed elsewhere until a test proves correct geometry and lifecycle.

## 10. Test matrix

Automated where feasible:

- primary and secondary carets move horizontally/vertically;
- vertical movement paints a narrow aligned stem rather than a full-height orthogonal bar;
- a second movement while the first trail is active replaces the visible origin with its exact previous caret;
- large navigation jump versus typing;
- folded and soft-wrapped lines;
- scroll during active trail;
- editor zoom/font change;
- multiple carets added/removed, independent origins, primary-first selection, and eight-caret saturation;
- All carets/Primary only and Soft/Balanced/Clear/Bright policy changes during active trails;
- click versus drag/right-click/double-click;
- release editor during every animation phase;
- rapid file switch and editor split;
- destination caret relocation during focus gain does not emit a competing destination-local trail;
- split transitions from every cardinal direction and all four diagonal quadrants;
- global disable/reduced motion/Power Save mid-animation;
- 100 create/release cycles with leak assertion.

Manual visual:

- light/dark/high-contrast themes;
- 100–200% scale and mixed-DPI monitors;
- IME input;
- very large files and low-power hardware;
- 2025.3 versus native smooth caret on 2026.1+.
