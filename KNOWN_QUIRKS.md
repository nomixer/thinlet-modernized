# Known Quirks

This file catalogs behaviors of the 2005 Thinlet codebase that the maintainer
believes are *wrong, surprising, or buggy* — but which this project
**deliberately preserves**.

## Why quirks are locked in, not fixed

This is the **modernization** project. Its contract is: *preserve 2005
Thinlet's observable behavior and modernize the toolchain around it.* Anything
that changes user-visible behavior — including fixing a genuine bug — is out of
scope here and belongs to the future **Enhanced Thinlet** effort.

Consequently:

- **Tests describe current behavior, bugs included.** A failing test means the
  production code drifted from 2005 behavior, *not* that Thinlet was "right"
  before.
- Each quirk below is pinned by at least one test tagged
  `@Tag("documents-current-behavior")`. The test asserts *what Thinlet does
  today*, so the quirk cannot silently change.
- `mvn test -Dgroups='!documents-current-behavior'` runs only the
  "definitely correct" subset, excluding quirk-locking tests.
- Enhanced Thinlet picks entries off this list deliberately, one at a time,
  with the behavior change made explicit.

## Entry format

Each entry, added as quirks are discovered during Phase 1+ test authoring:

```
### Q<n> — <short title>
- **What happens:** observable behavior.
- **Why it's a quirk:** expected vs. actual.
- **Where:** Thinlet.java location(s).
- **Locked by:** test class/method name (tagged documents-current-behavior).
- **Enhanced Thinlet disposition:** keep / fix / undecided.
```

## Quirks

_None recorded yet. Populated during Phase 1 (test + trace harness)._
