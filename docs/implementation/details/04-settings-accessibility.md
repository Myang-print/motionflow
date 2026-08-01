# Settings and Accessibility Details

## 1. Settings model

Persist application-scoped user intent in a versioned `PersistentStateComponent`. Keep raw persisted state separate from the immutable effective `MotionPolicy` used by runtime code.

Illustrative schema:

```kotlin
data class MotionFlowState(
    var schemaVersion: Int = CURRENT_SCHEMA,
    var enabled: Boolean = true,
    var profile: String = "BALANCED",
    var reducedMotion: Boolean = false,
    var durationScale: Double = 1.0,
    var intensityScale: Double = 1.0,
    var powerSaveBehavior: String = "DISABLE_ALL",
    var caretTrailEnabled: Boolean = true,
    var caretTrailLength: Int = 7,
    var caretTrailStyle: String = "SILKY_TRAIL",
    var clickRippleEnabled: Boolean = true,
    var navigationPulseEnabled: Boolean = true,
    var editorContextCueEnabled: Boolean = true,
    var toolWindowCueEnabled: Boolean = true,
    var inactiveFrameDimEnabled: Boolean = false,
    var inactiveFrameDimAmount: Double = 0.10,
    var diagnosticsDebugMode: Boolean = false,
)
```

Persist enum-like values as stable uppercase IDs, not localized display strings.

## 2. Validation and ranges

| Value | Range | Balanced default | Invalid behavior |
| --- | ---: | ---: | --- |
| Duration scale | `0.25..2.00` | `1.00` | Clamp and warn once |
| Intensity scale | `0.25..1.50` | `1.00` | Clamp and warn once |
| Trail length | `2..12` | `7` | Clamp and warn once |
| Frame dim amount | `0.04..0.25` | `0.10` | Clamp and warn once |
| Navigation jump threshold | `3..50` visual lines | `8` | Clamp and warn once |

Duration zero is represented by Reduced Motion/effective policy, not by storing an arbitrary duration scale of zero.

Unknown enum IDs fall back to documented defaults. Missing fields take defaults. Migration is a pure function from schema N to N+1 and is unit-tested with serialized fixtures.

## 3. Effective policy precedence

Highest priority first:

1. global master disabled;
2. headless/test or unsupported capability;
3. Reduced Motion;
4. Power Save Mode behavior;
5. high-contrast simplification;
6. effect-specific user enablement;
7. selected profile/custom values;
8. adaptive degradation.

Adaptive degradation may reduce fidelity but never enables an effect disabled by a higher-priority rule. Leaving Power Save Mode restores user intent, not a stale intermediate policy.

## 4. Settings UI layout

Use Kotlin UI DSL 2 and standard platform controls.

```text
MotionFlow
├── [x] Enable MotionFlow
├── Profile: [Balanced v]     [x] Reduced motion
├── Preview [Play once] [Pause]
├── Editor
│   ├── [x] Caret trail       Length [7]
│   │   └── Style [Silky native trail v]
│   ├── [x] Click ripple
│   └── [x] Navigation pulse
├── Workbench
│   ├── [x] Editor-context cue
│   ├── [x] Tool-window cue
│   └── [ ] Dim inactive project frames  Amount [10%]
├── Behavior
│   ├── Duration [100%]
│   ├── Intensity [100%]
│   └── Power Save Mode [Disable all v]
└── Diagnostics
    ├── Effective capabilities summary
    ├── [Copy Diagnostics]
    └── [Reset to Defaults]
```

The exact UI may be refined, but grouping and top-level access to global enable/Reduced Motion are requirements.

## 5. Apply, cancel, reset, and live preview

- The settings form edits a detached draft model.
- Preview derives a policy from the draft and uses a preview-local target/disposable.
- `Apply` normalizes, persists, publishes one policy change, and updates live surfaces.
- `Cancel` disposes the preview and discards the draft; it does not need to undo live IDE effects because the draft never controlled them.
- `Reset` changes the draft to defaults and updates only the preview until Apply.
- External changes through an action update the service; an open settings page refreshes safely or prompts according to platform conventions.

Avoid writing settings on every slider movement. Preview is local; persisted/live policy changes on Apply.

## 6. Profiles

Profiles are named sets of default values. They are not separate persistent objects in MVP.

When a user selects a profile:

1. copy profile values into the draft;
2. retain the independent Reduced Motion value;
3. show the selected profile ID.

When a profile-controlled value changes manually, set profile to `CUSTOM`. Turning an effect on/off also produces Custom unless the resulting values exactly match a named profile.

## 7. Reduced Motion

Reduced Motion is an override, not a profile. It must:

- remain visible and operable when the plugin is otherwise enabled;
- cancel spatial timelines immediately after Apply or toggle action;
- disable caret trail and click ripple;
- replace navigation/context/tool-window cues with no effect or opacity-only feedback ≤80 ms;
- disable inactive-frame dimming;
- keep diagnostics and settings available;
- never be switched off automatically.

Operating-system detection may be added only through a supported, testable API. Until then the UI must say `Reduced motion`, not `Use system setting`.

## 8. Accessibility implementation checklist

### Keyboard and screen reader

- Every control has a localized label and description.
- Labels use `labelFor` relationships where applicable.
- Focus order follows visual order.
- Group headings are accessible and do not receive unnecessary focus.
- Sliders/spinners expose numeric value and unit.
- Preview Play/Pause is operable from keyboard and announces state.
- Disabled controls explain the governing reason, such as Reduced Motion.

### Visual

- Do not rely on color/motion to communicate required state.
- Use theme-aware colors; avoid hard-coded light/dark assumptions.
- Target 3:1 non-text indicator contrast.
- High contrast removes gradients/shadows/translucent frame dimming.
- No effect loops; no rapid flashing; no zoom of the entire interface.

### Cognitive/distraction

- Defaults are brief and localized to the changed surface.
- An instant toggle is available through Find Action.
- No first-run modal wizard.
- Preview runs once per activation, then stops.

## 9. Localization

All user-visible strings live in `MotionFlowBundle.properties`. Stable identifiers, settings keys, logs, and diagnostic field names remain English and nonlocalized.

Before release:

- run inspections for hard-coded UI strings;
- proofread English Marketplace and in-product copy;
- keep descriptions concise and use established JetBrains UI terminology;
- add translations only when maintainers can verify them; English remains first.

## 10. Tests

- default policy golden test;
- profile-to-policy table;
- precedence matrix for enable/reduced/power/high-contrast/capability/degradation;
- invalid/missing/unknown persisted values;
- schema migration fixtures;
- Apply/Cancel/Reset semantics;
- live runtime policy changes only after Apply;
- preview disposal and no idle timer;
- accessible names and keyboard focus order via UI test/manual audit;
- Reduced Motion cancels active spatial effects within one EDT turn.
