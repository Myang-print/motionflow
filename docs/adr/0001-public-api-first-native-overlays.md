# ADR 0001: Public-API-First Native Overlays

- Status: Accepted
- Date: 2026-07-31
- Decision owners: Haoran / MotionFlow maintainers

## Context

VSCode Animations injects CSS and JavaScript into an Electron DOM. IntelliJ Platform IDEs use Swing/JBR and expose selected extension points, events, services, and painting utilities. Reaching arbitrary JetBrains-owned UI through implementation classes or reflection would be fragile, difficult to unload, and incompatible with current Marketplace approval criteria.

## Decision

Stable MotionFlow features will:

- observe supported platform events after state changes;
- render non-interactive overlays through supported painter/component APIs;
- attach all resources to `Disposable` lifetimes;
- fail closed when a surface is unavailable;
- pass Plugin Verifier with no internal-API violations.

Stable code will not use reflection, internal/implementation APIs, component class-name probing, UI delegate/layout replacement, Java agents, bytecode instrumentation, IDE-file patching, or arbitrary scripts/styles.

## Consequences

Positive:

- normal Marketplace installation and dynamic unload remain feasible;
- platform behavior stays authoritative;
- unsupported products degrade safely;
- compatibility issues are easier to isolate.

Negative:

- some visually attractive effects cannot be implemented now;
- true tab/popup/layout animations may remain unavailable;
- feature count grows more slowly than an implementation that patches internals.

## Alternatives rejected

- **Reflection/internal Swing hierarchy access:** rejects compatibility and approval goals.
- **Custom look and feel/UI delegates:** too broad and conflicts with themes/platform ownership.
- **IDE/JBR instrumentation:** disproportionate security and maintenance risk.
- **Whole-window screenshots/crossfades:** costly, accessibility-hostile, and platform-interfering.

## Reconsideration trigger

A specific public API may enable a previously deferred effect. That effect can be added without reversing this ADR. Reversing the public-API boundary requires a new ADR and PRD/Marketplace/security review.

