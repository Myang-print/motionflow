# MotionFlow

MotionFlow is a native, configurable motion system for IntelliJ Platform IDEs. It aims to make caret movement, editor changes, tool-window focus, and other state transitions easier to follow without delaying input or modifying platform behavior.

> **Project status:** foundation/design phase. The repository is buildable, but animation effects are not implemented yet. The accepted scope and engineering gates are defined before feature work begins.

## Why MotionFlow

[VSCode Animations](https://github.com/BrandonKirbyson/VSCode-Animations) demonstrates how much polish thoughtful motion can add to an editor. VS Code uses an Electron DOM; IntelliJ Platform IDEs use Swing/JBR. MotionFlow is therefore an independent native implementation, not a CSS/JavaScript port.

The central invariant is:

> Motion may clarify an IDE state transition, but it must never delay, block, reorder, or alter that transition.

Planned MVP effects include:

- an event-driven editor caret trail;
- editor click ripple and large-navigation active-line pulse;
- selected-editor and tool-window context cues;
- optional inactive-frame dimming;
- motion profiles, Reduced Motion, Power Save Mode integration, and local diagnostics.

The stable plugin will use supported APIs only. It will not patch IDE files, inject code, use reflection/internal APIs, replace UI delegates, or run an idle animation loop.

## Documentation map

- [Product requirements](docs/PRD.md)
- [Implementation plan](docs/implementation/implement.md)
- [Detailed implementation designs](docs/implementation/details/)
- [Requirement traceability](docs/traceability-matrix.md)
- [VSCode Animations analysis](docs/research/upstream-analysis.md)
- [IntelliJ Platform feasibility](docs/research/platform-feasibility.md)
- [Architecture decisions](docs/adr/)
- [Roadmap](ROADMAP.md)
- [Contributing](CONTRIBUTING.md)

## Development

Requirements:

- JDK 21 (the Gradle toolchain resolver can provision it);
- no global Gradle installation is required;
- IntelliJ IDEA 2025.3+ is recommended for development.

On Windows PowerShell:

```powershell
.\gradlew.bat check
.\gradlew.bat verifyPluginProjectConfiguration
.\gradlew.bat verifyPluginStructure
.\gradlew.bat buildPlugin
```

Run a sandbox IDE:

```powershell
.\gradlew.bat runIde
```

To validate against an already installed compatible IDE without downloading another distribution:

```powershell
.\gradlew.bat check -PlocalIdePath="C:\Program Files\JetBrains\IntelliJ IDEA 2025.3.1"
```

CI and normal builds target the latest 2025.3 patch (`2025.3.6`); `localIdePath` is a developer override only.

Run the broader compatibility verifier (downloads additional IDE distributions):

```powershell
.\gradlew.bat verifyPlugin
```

## Compatibility baseline

- Minimum IntelliJ Platform: 2025.3 / build 253
- Build language: Kotlin
- JVM bytecode/toolchain: Java 21
- Gradle: Wrapper 9.5.0
- IntelliJ Platform Gradle Plugin: 2.18.1

Broad product and remote-development support are goals, not current release claims. Each claim must pass the matrix in the PRD and implementation plan.

## Privacy

MotionFlow is designed to make no network requests, collect no telemetry, read no document text, and retain no workspace history. See [PRIVACY.md](PRIVACY.md).

## License

Apache License 2.0. See [LICENSE](LICENSE). VSCode Animations is credited as product inspiration in [THIRD_PARTY_NOTICES.md](THIRD_PARTY_NOTICES.md).
