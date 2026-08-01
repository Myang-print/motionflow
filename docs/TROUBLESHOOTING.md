# Troubleshooting

## An effect does not appear

1. Confirm **Enable MotionFlow** and the individual effect are enabled.
2. Check Reduced Motion and the selected Power Save Mode behavior.
3. Use a standard main editor. Consoles, previews, diff editors, one-line editors, and unsupported sessions fail closed in `0.2.2`.
4. Ensure the target component is visible in a normal project frame.
5. Run **MotionFlow: Copy Diagnostics** and inspect the categorical capability/degradation state.

## Motion paused automatically

MotionFlow measures only its own bounded update callback cost. Sustained p95 cost above 2 ms first simplifies visual detail, then pauses nonessential effects. This is session-local adaptive degradation, not a claim that MotionFlow caused every slow IDE frame.

Restarting the IDE resets session degradation. If it recurs, attach copied diagnostics, IDE build, OS, scaling, theme, effect/profile, and reproducible steps to a performance issue. Do not include private project data.

## The caret trail conflicts with native smooth caret movement

MotionFlow never changes the IDE's caret settings. On the native smooth-caret platform era it automatically caps the trail to four points and a shorter duration. Disable **Caret trail** if the combined appearance is distracting.

## The caret trail shape is not what I prefer

Open MotionFlow settings and change **Trail style**. **Silky native trail** is the default continuous afterimage; **Speed lines** keeps the sharper wedge, and **Rounded ribbon** restores the capsule form. Intensity controls peak opacity, while Trail length controls the bounded frame history. High Contrast intentionally replaces translucent styles with a solid square-ended indicator.

## The trail starts from an old or unrelated caret position

Version `0.2.2` resets every local trail to the immediately previous accepted caret and suppresses destination-local history during split-pane focus changes. If this still occurs, record whether the move was typing, mouse relocation, Find Action navigation, Split Right/Down focus, or a scroll/fold change, and attach a short recording plus copied diagnostics.

## The split-pane caret flow does not appear

Both editor panes must remain visible, belong to the same top-level IDE frame, and use supported standard editors. Enable **Caret trail**, disable Reduced Motion, and click from one editor content area into the other after using Split Right or Split Down. Hidden tab changes and movement between separate IDE windows intentionally fail closed.

## The project frame remains dimmed

Run **MotionFlow: Toggle Motion** twice or disable **Dim inactive project frames**. Global disable, focus gain, project close, and plugin unload are designed to clear dimming immediately. If the overlay remains, capture a screenshot and diagnostics, then report it as a high-priority bug.

## Report a bug

Use the repository's [bug report form](https://github.com/Myang-print/motionflow/issues/new?template=bug.yml). Include:

- IDE product and exact build;
- OS, theme, and display scaling;
- MotionFlow version and profile;
- reproducible steps;
- copied diagnostics;
- a screenshot or short recording when the issue is visual.
