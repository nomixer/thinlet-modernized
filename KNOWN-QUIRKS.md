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

### Q1 — `parse` throws `NullPointerException` on an unreadable source
- **What happens:** `parse(String)` with a path that is neither a classpath
  resource nor a valid URL, and `parse(InputStream)` with a `null` stream, throw
  `NullPointerException` rather than a descriptive `IOException`.
- **Why it's a quirk:** an unreadable source should report a clear I/O error; the
  2005 code instead lets a `null` stream reach `new InputStreamReader(...)`. The
  author was aware — `parse(String, Object)` carries the comment
  `/* thows nullpointerexception*/` where the `MalformedURLException` is swallowed.
- **Where:** `Thinlet.java` `parse(String, Object)` (~6451-6464; the `null`
  stream is not guarded) → `parse(InputStream, char, Object)` (~6601,
  `new InputStreamReader`).
- **Locked by:** `thinlet.quirks.ParserNullSourceQuirkTest` (tagged
  documents-current-behavior).
- **Enhanced Thinlet disposition:** fix — throw a descriptive `IOException`.

## Triaged for Enhanced Thinlet (not behavior-locked)

These D13 candidate findings were investigated during Phase 1 but are *not* pinned
by behavior tests this slice, with reasons. They remain SpotBugs suppressions
(`config/spotbugs/exclude.xml`) for Enhanced Thinlet to address.

- **XML parser "unclosed stream" (`OBL_*`, `OS_OPEN_STREAM`).** Not reproducible
  as a leak: `parse(InputStream, char, Object)` closes the `Reader` (which closes
  the underlying stream) in a `finally` on every practical path. The SpotBugs
  finding is the exception-edge of the `Reader` being constructed just outside the
  `try` (~6601) — there is no behavioral quirk to lock; tightening the cleanup is
  an Enhanced Thinlet nicety.
- **`FileChooser` fallback `View.getFiles` null deref.** `File.list()` can return
  `null` (non-directory / I/O error) and is dereferenced unguarded
  (`thinlet-demos` `FileChooser.java` ~106-108). It lives in the `View` *fallback*
  inner class, used only when the Swing-backed `View2` fails to load, so it is not
  exercised on a normal JDK; combined with `thinlet-demos` having no test harness
  and `View` being a private inner class, it is documented here rather than
  test-locked. Disposition: fix — guard the `null`.
