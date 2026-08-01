# Compatibility Evidence

Date: 2026-08-01

## Verified binary range

| Product | Version/build | Method | Result |
| --- | --- | --- | --- |
| IntelliJ IDEA Ultimate | 2025.3.6 / `IU-253.33813.25` | Build target, sandbox startup, Plugin Verifier | Compatible; plugin loaded |
| IntelliJ IDEA Ultimate | 2026.2 / `IU-262.9437.65` | Plugin Verifier | Compatible |

The plugin descriptor sets `since-build="253"` and deliberately leaves `until-build` open. The release workflow reruns Plugin Verifier before creating a tagged release.

## Product claim boundary

MotionFlow depends only on `com.intellij.modules.platform`, so its binary package is not tied to Java, Python, JavaScript, or another language plugin. That is an architectural compatibility property, not proof that every product-specific UI has been visually exercised.

`0.2.1` therefore claims the IntelliJ Platform baseline and the two verifier cells above. Product-specific Marketplace compatibility, remote development, Android Studio, Rider frontend behavior, macOS/Linux visual behavior, and mixed-monitor DPI remain unclaimed until their smoke cells are recorded.

## CI matrix

The build workflow runs the full package/structure gate on Ubuntu and adds test/configuration jobs for Windows and macOS. [GitHub Actions run 30683101832](https://github.com/Myang-print/motionflow/actions/runs/30683101832) passed all four jobs from the 0.2.1 implementation commit `e1f0960`: Linux package/structure checks, macOS tests, Windows tests, and the two-baseline Plugin Verifier.

The verifier runner explicitly reclaims unrelated Android, .NET, Haskell, and CodeQL SDK space before extracting both IntelliJ baselines. This prevents the general-purpose runner image from exhausting its disk without changing the plugin binary or verifier scope. The exact local 0.2.1 package was independently reverified against both listed builds on 2026-08-01; both verdict files report `Compatible` and probable dynamic enable/disable eligibility.
