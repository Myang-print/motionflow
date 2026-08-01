# MotionFlow User Guide

## Install and open settings

Install the release ZIP through **Settings | Plugins | gear menu | Install Plugin from Disk…**. MotionFlow requires IntelliJ Platform build 253 or later.

Open **Settings | Appearance & Behavior | MotionFlow**. Settings are application-wide and apply after selecting **Apply** or **OK**. The preview uses the draft values without changing live IDE effects.

## Profiles

| Profile | Best for | Behavior |
| --- | --- | --- |
| Subtle | Minimal distraction | Shorter duration, lower intensity, shorter trail |
| Balanced | Everyday use | Default timings and stable effects |
| Expressive | More visible feedback | Longer duration, stronger cues, optional frame dim enabled |
| Custom | Individual tuning | Selected automatically after changing a profile-controlled value |

Reduced Motion is independent of profiles. It disables the caret trail, click ripple, and inactive-frame dimming. Orientation feedback is removed or converted to an opacity-only cue no longer than 80 ms.

## Effects

### Caret trail

The trail follows only the primary caret and stores at most 12 samples. The default **Silky native trail** begins at the caret's immediately previous location, follows a smooth frame-sampled visual head to the new location, then fades after arrival. Its transparent tail grows into a caret-height leading edge, while a narrow luminous center curve keeps the effect visible. Select **Speed lines** or **Rounded ribbon** under Trail style for the alternative forms. High Contrast always uses a thinner solid indicator. MotionFlow does not move or replace the IDE caret; the platform move completes first.

When focus moves between two visible Split Right or Split Down editor panes, the caret-trail setting also enables a distance-aware flow cue across the shared project frame. MotionFlow captures the source editor's last caret, clears any stale destination-local trail, and samples one continuous path to the settled destination. The native caret appears at the destination immediately; the cue does not run between separate top-level IDE windows or hidden editor tabs.

### Click ripple

A short ripple appears after a primary-button click in a standard editor. Drag-selection, popup triggers, right-clicks, and releases outside the click time/distance limits do not trigger it.

### Navigation pulse

A brief destination-line cue appears when the primary caret crosses at least the configured number of visual lines. Routine typing and one-line movement remain quiet.

### Editor and tool-window cues

Changing the selected editor paints a short edge cue on the destination component. Activating a tool window paints a brief outline. Neither effect modifies tabs, layout, focus behavior, or component geometry.

### Inactive-frame dim

This effect is off by default. When enabled, an eligible project frame receives a small input-transparent dim overlay after losing focus. It is disabled in Reduced Motion, Power Save disable modes, high contrast, presentation mode, and modal-dialog transitions.

## Power Save Mode

- **Disable all:** stop every effect while Power Save Mode is active.
- **Essential only:** keep only short orientation cues.
- **Ignore:** preserve the selected motion policy.

Leaving Power Save Mode restores the saved user settings.

## Actions

Use **Find Action** to run:

- **MotionFlow: Toggle Motion** — instant global enable/disable.
- **MotionFlow: Open Settings** — open the application settings page.
- **MotionFlow: Copy Diagnostics** — copy a redacted local snapshot for an issue report.

## Diagnostics and privacy

Diagnostics include plugin/IDE versions, policy flags, categorical capability states, bounded engine counters, frame-cost percentiles, degradation state, and sanitized error categories. They exclude paths, project/file names, document contents, host/user names, environment variables, exception messages, and stack traces.

MotionFlow sends nothing over the network.
