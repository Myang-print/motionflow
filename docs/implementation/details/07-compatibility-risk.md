# Compatibility and Risk Controls

## 1. Compatibility strategy

MotionFlow supports platform behavior, not Swing implementation details. The compatibility strategy is:

1. depend only on `com.intellij.modules.platform`;
2. compile against the latest 2025.3 patch (`2025.3.6` at initialization) with Java 21;
3. declare minimum build `253`;
4. keep `until-build` open only while latest-release verification stays green;
5. test every used symbol with Plugin Verifier and Plugin DevKit inspections;
6. put product/version differences behind capability adapters that fail closed;
7. avoid conditional reflection and class-name probing.

## 2. Why the baseline is 2025.3

- It is recent enough for current IntelliJ Platform Gradle Plugin 2.x workflows.
- It aligns with the locally available IntelliJ IDEA 2025.3 development environment.
- Split Mode testing tooling targets 2025.3 or later.
- A narrower modern baseline reduces compatibility branches while still covering users immediately before native 2026.1 smooth caret movement.

Changing the minimum build requires download/user evidence, implementation cost analysis, and a PRD decision. Do not lower it by importing old compatibility shims casually.

## 3. Java and Gradle matrix

| Target | Runtime/toolchain expectation | Action |
| --- | --- | --- |
| Platform 2025.3–2026.1 | Java 21 | Compile baseline and mandatory tests |
| Platform 2026.2+ | Java 25 platform runtime | Plugin Verifier/latest smoke; add dedicated CI JDK if building directly against it |
| Gradle | Wrapper 9.5.0 | Never require global Gradle |
| IntelliJ Platform Gradle Plugin | 2.18.1 baseline | Update through reviewed dependency PRs |

Developers may pass `-PlocalIdePath=<installed IDE>` for a local smoke build. This does not change the default CI/release target or justify a new compatibility claim.

Do not raise Kotlin/JVM bytecode target above the minimum platform requirement.

## 4. Capability states

Each effect reports one of:

```text
SUPPORTED
DISABLED_BY_SETTING
DISABLED_BY_POLICY
UNSUPPORTED_PLATFORM
UNSUPPORTED_PRODUCT
UNSUPPORTED_SESSION
UNSUITABLE_SURFACE
DEGRADED_FOR_PERFORMANCE
DISABLED_AFTER_FAILURE
```

Capability checks are pure over a small environment snapshot wherever possible. Unsupported is expected control flow and should not generate an error notification.

## 5. API admission checklist

Before a new platform API enters stable code:

- official SDK docs or source confirms intended plugin use;
- no `@ApiStatus.Internal` or scheduled-removal annotation;
- no implementation package/class dependency;
- available in build 253 or isolated behind a compiled compatibility module/adapter strategy that remains verifiable;
- ownership and callback thread are understood;
- a disposable or declarative registration path exists;
- behavior is verified in current and latest platform builds;
- Plugin Verifier reports no internal/compatibility problem;
- failure can degrade to no-op.

If evidence is ambiguous, treat the API as unavailable.

## 6. Product compatibility

Declaring `com.intellij.modules.platform` makes broad compatibility possible but does not prove visual correctness. Products can differ in editors, tool windows, UI composition, JBR, and remote architecture.

For each product claim:

1. Plugin Verifier passes;
2. install/uninstall succeeds;
3. a standard editor attaches and disposes correctly;
4. settings/actions work;
5. each advertised effect receives a smoke test;
6. diagnostics report no unexpected capability/failure state.

Product-specific effects are out of scope for MVP. Capability adapters may disable an effect for one product without changing the base dependency.

## 7. Operating systems and display scaling

### Windows

- test 100/125/150/200% scaling and moving between monitors;
- verify frame focus/dimming with native window decorations;
- test Direct3D rendering defaults and high contrast;
- avoid assumptions based on filesystem paths or display device order.

### macOS

- test Retina scaling, full-screen spaces, and native dialogs;
- verify application versus window focus semantics;
- keep menu-bar behavior outside plugin scope.

### Linux

- test X11/Wayland combinations available in supported IDEs;
- verify opacity/alpha compositing only through Swing painters;
- fail closed if frame focus/overlay behavior is unreliable.

Logical Swing coordinates are the stored model. Device transforms are observed at render time.

## 8. Remote development and Split Mode

Motion is latency-sensitive frontend behavior. The initial classic plugin must not claim native Split Mode support merely because installation succeeds.

Validation questions:

- where is the plugin installed by Marketplace/product defaults?
- do editor events and painting execute in JetBrains Client?
- does any application/project service accidentally resolve on backend only?
- can settings stay local to the frontend without inconsistent backend state?
- does unload clean both sides?

If classic packaging cannot provide correct frontend behavior, remote capability stays unsupported until a modular plugin migration. Do not add RPC solely to animate UI; the preferred model has no backend dependency.

## 9. Native feature coexistence

Platform releases may add motion features. MotionFlow policy is to complement, not override:

- never change IDE smooth-caret or scrolling settings automatically;
- avoid rendering a second moving caret;
- expose independent effect controls;
- use platform/version capability notes without reflection into settings internals;
- disable conflicting defaults for a version if user testing shows a clear issue.

The compatibility matrix records native settings used during tests.

## 10. Lab feature isolation

Experiments that are not ready for the stable API boundary must satisfy all rules:

- live in a separate `lab` source set/module excluded from release artifacts by default;
- require an explicit development property and opt-in setting;
- never modify IDE files or use bytecode instrumentation;
- never weaken stable architecture tests;
- have a time-boxed question and promotion criteria;
- are deleted when the spike is closed if no supported path exists.

No Marketplace build includes reflection/internal-API experiments.

## 11. Risk register operation

Each compatibility issue records:

- affected product/build/OS/JBR/theme/scale/session;
- exact capability and public APIs involved;
- severity and user-visible failure;
- whether stable platform behavior changed;
- safe disable/fallback path;
- reproducer and artifact checksum;
- verifier/inspection results;
- fix, release cap, or feature-disable decision.

P0/P1 compatibility regressions block release. A narrower truthful compatibility claim is preferable to an unverified broad claim.

## 12. Upgrade procedure

For each platform or Gradle-plugin update:

1. read incompatible/notable API changes;
2. update on a dedicated branch;
3. run project configuration and structure verification;
4. run full test suite;
5. run Plugin Verifier on minimum/current/latest;
6. run editor lifecycle, settings, and one effect per surface manually;
7. compare performance baselines;
8. update compatibility evidence and docs;
9. only then expand or retain release claims.
