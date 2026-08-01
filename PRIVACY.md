# Privacy

MotionFlow is designed to operate entirely inside the local IDE process.

## Data practices

MotionFlow does not:

- collect or transmit telemetry, analytics, or usage statistics;
- make network requests;
- read or persist document text;
- persist file names, paths, project names, caret history, or user activity history;
- include third-party analytics or crash-reporting SDKs;
- load remote code, scripts, styles, images, or other assets.

## Local settings and diagnostics

The plugin stores application-level visual preferences through the IntelliJ Platform settings mechanism.

The **Copy Diagnostics** feature includes plugin/IDE/OS versions, effective settings and capabilities, aggregate bounded performance counters, and sanitized error categories. It excludes paths, project/file names, document content, user/host names, environment variables, raw stack traces, and persistent identifiers. The user decides whether to share the copied text.

## Future changes

Any future network, telemetry, or personal-data processing capability requires a separate product/privacy review, an update to this document, and explicit user consent before collection. It cannot be added as a silent implementation detail.

Last updated: 2026-08-01.
