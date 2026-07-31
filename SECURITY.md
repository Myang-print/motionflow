# Security Policy

## Supported versions

MotionFlow has not published a stable version yet. Security fixes will target the latest maintained release once releases begin.

## Reporting a vulnerability

Do not open a public issue for a vulnerability or include sensitive exploit details in ordinary diagnostics.

After the repository is hosted on GitHub, use **Security → Report a vulnerability** to create a private security advisory. If private reporting is not enabled, contact the repository owner through their GitHub profile to establish a private channel before sharing details.

Include:

- affected MotionFlow and IDE versions;
- impact and prerequisites;
- minimal reproduction steps or proof of concept;
- whether the issue exposes data, executes code, crosses trust boundaries, or persists after disable/uninstall;
- suggested mitigation if known.

Please allow reasonable time for triage and a coordinated fix before disclosure.

## Security boundaries

Stable MotionFlow releases must not use reflection/internal APIs, modify IDE files, load arbitrary or remote code, transmit telemetry, or read document content. See the [PRD](docs/PRD.md) and [privacy policy](PRIVACY.md).

