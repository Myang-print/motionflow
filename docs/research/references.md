# Research References

Accessed 2026-07-31 unless otherwise stated.

## Primary reference project

- [VSCode Animations repository](https://github.com/BrandonKirbyson/VSCode-Animations) — product inspiration and source analysis at version 2.0.7.

## IntelliJ Platform SDK

- [IntelliJ Platform Plugin Template](https://plugins.jetbrains.com/docs/intellij/plugin-github-template.html) — official project scaffold and current Gradle Plugin 2.x direction.
- [Configuring IntelliJ Platform Gradle Plugin 2.x](https://plugins.jetbrains.com/docs/intellij/configuring-gradle.html) — target platform and verification tasks.
- [IntelliJ Platform Gradle Plugin 2.x](https://plugins.jetbrains.com/docs/intellij/tools-intellij-platform-gradle-plugin.html) — plugin requirements and current setup.
- [Plugin compatibility](https://plugins.jetbrains.com/docs/intellij/plugin-compatibility.html) — module dependencies and Plugin Verifier.
- [Build number ranges](https://plugins.jetbrains.com/docs/intellij/build-number-ranges.html) — `since-build`/`until-build` rules.
- [Threading model](https://plugins.jetbrains.com/docs/intellij/threading-model.html) — EDT responsibilities and UI-freeze prevention.
- [Disposer and Disposable](https://plugins.jetbrains.com/docs/intellij/disposers.html) — resource ownership and listener cleanup.
- [Listeners](https://plugins.jetbrains.com/docs/intellij/plugin-listeners.html) — declarative listener registration and lifecycle guidance.
- [Editors](https://plugins.jetbrains.com/docs/intellij/editors.html) — selected editor and editor event orientation.
- [Editor coordinate systems](https://plugins.jetbrains.com/docs/intellij/coordinates-system.html) — logical/visual positions and editor coordinates.
- [Multiple carets](https://plugins.jetbrains.com/docs/intellij/multiple-carets.html) — primary and multiple caret behavior.
- [Tool windows](https://plugins.jetbrains.com/docs/intellij/tool-windows.html) — tool-window APIs and events.
- [User interface components](https://plugins.jetbrains.com/docs/intellij/user-interface-components.html) — native Swing components and UI DSL guidance.
- [Dynamic plugins](https://plugins.jetbrains.com/docs/intellij/dynamic-plugins.html) — unload requirements and cleanup.
- [Plugin UX and performance](https://plugins.jetbrains.com/docs/intellij/plugin-user-experience.html) — perceived performance and Split Mode.
- [Split Mode](https://plugins.jetbrains.com/docs/intellij/split-mode-and-remote-development.html) — frontend/backend responsibilities.
- [Modular plugins](https://plugins.jetbrains.com/docs/intellij/modular-plugins.html) — experimental modular format and frontend modules.
- [Incompatible API changes 2026](https://plugins.jetbrains.com/docs/intellij/api-changes-list-2026.html) — Java/platform compatibility changes.
- [IntelliJ IDEA 2025.3.6 release](https://blog.jetbrains.com/idea/2026/06/intellij-idea-2025-3-6/) — latest 2025.3 maintenance baseline used by the default build.
- [IJPL-226110](https://youtrack.jetbrains.com/issue/IJPL-226110) — known malformed full-line module descriptor in 2025.3.1, supporting the latest-patch build decision.
- [IntelliJ Platform source: `IdeGlassPaneUtil`](https://github.com/JetBrains/intellij-community/blob/master/platform/platform-api/src/com/intellij/openapi/wm/IdeGlassPaneUtil.java) — implementation-spike reference; the SDK annotation check found `installPainter` internal, so MotionFlow does not use it.
- [IntelliJ Platform source: `CaretListener`](https://github.com/JetBrains/intellij-community/blob/master/platform/editor-ui-api/src/com/intellij/openapi/editor/event/CaretListener.java) — caret movement lifecycle.

## Current IDE behavior

- [Editor appearance settings](https://www.jetbrains.com/help/idea/settings-editor-appearance.html) — native smooth caret movement in current IDE documentation.
- [Smooth caret movement issue IJPL-793](https://youtrack.jetbrains.com/issue/IJPL-793) — platform feature history/context.

## Marketplace

- [Marketplace approval guidelines](https://plugins.jetbrains.com/docs/marketplace/jetbrains-marketplace-approval-guidelines.html) — current naming, icon, compatibility, performance, privacy, and internal-API criteria.
- [Best practices for listing](https://plugins.jetbrains.com/docs/marketplace/best-practices-for-listing.html) — listing name, description, and media guidance.
- [Understanding plugin security](https://plugins.jetbrains.com/docs/marketplace/understanding-plugin-security.html) — review and moderation context.

## Comparable products

- [SmoothScroll](https://plugins.jetbrains.com/plugin/7355-smoothscroll) — narrow existing smooth-scrolling plugin.
- [Smooth Caret Trail](https://plugins.jetbrains.com/plugin/31756-smooth-caret-trail) — current narrow caret-trail offering; used only for market/feature context, not implementation.

## Evidence policy

Official SDK documentation and IntelliJ Platform source are authoritative for implementation decisions. Marketplace listings are evidence that a product exists, not evidence that its implementation approach is safe. Search results, forums, and community posts may generate questions but do not justify a stable API dependency.
