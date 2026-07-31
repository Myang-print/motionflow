# ADR 0002: Classic Plugin with Split-Ready Boundaries

- Status: Accepted
- Date: 2026-07-31

## Context

Motion is frontend, latency-sensitive behavior. JetBrains recommends separating frontend UI from backend project logic for remote development. The modular plugin format provides that split but remains experimental and is recommended primarily when remote development requires it.

MotionFlow's MVP needs no PSI, VFS, indexes, filesystem access, or backend processes.

## Decision

Initialize MotionFlow as a classic plugin targeting 2025.3+, while enforcing boundaries that make a later modular migration straightforward:

- core contains no project-model/backend dependencies;
- all event and painting code is modeled as frontend behavior;
- settings/policy are independent from surface adapters;
- remote capability is explicit and unsupported until tested;
- no remote-support claim is made from installation success alone.

## Consequences

Positive:

- lower initial build and packaging complexity;
- stable, well-understood plugin lifecycle;
- no premature RPC/backend module;
- architecture still aligns with future frontend separation.

Negative:

- remote development may be unavailable in `0.1.0`;
- modular migration may later change project structure and packaging.

## Alternatives rejected

- **Start modular immediately:** adds experimental complexity without a backend requirement.
- **Ignore Split Mode:** would make later frontend support expensive and could create latency mistakes.
- **Add backend RPC preemptively:** violates minimality; animation has no backend data need.

## Reconsideration trigger

If a real Split Mode prototype shows the classic artifact cannot run UI code correctly on the frontend, create a superseding ADR and modular migration plan before advertising remote support.

