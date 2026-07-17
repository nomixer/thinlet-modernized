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

### Q1 — `parse` threw `NullPointerException` on an unreadable source — **fixed in 0.2.x (D71)**
- **What happened (≤0.1.x):** `parse(String)` with a path that was neither a
  classpath resource nor a valid URL, and `parse(InputStream)` with a `null`
  stream, threw `NullPointerException` rather than a descriptive `IOException`.
  The author was aware — `parse(String, Object)` carried the comment
  `/* thows nullpointerexception*/` where the `MalformedURLException` was
  swallowed.
- **The fix:** both public paths now throw a descriptive `IOException`
  (`"unreadable source: <path>"` / `"null input stream"`); a valid stream
  parses unchanged.
- **Where:** `Thinlet.java` `parse(String, Object)` (the unreadable-source
  guard) and `parse(InputStream, Object)` (the null-stream guard).
- **Locked by:** `thinlet.quirks.ParserUnreadableSourceTest` (asserts the fixed
  contract; the old NPE-locking tests were flipped in the same PR, red-green
  checked both ways).

### Q2 — `splitpane` divider is absolute pixels: non-proportional + destructive clamp on resize
- **What happens:** the divider position is an absolute pixel value (`divider`
  property) that is only ever *clamped* on resize, never rescaled. Growing the
  splitpane keeps the same pixel divider (the split ratio drifts); shrinking it
  below the divider clamps the value to `width-5` and does **not** restore the
  original position when the pane grows back — the value is lost.
- **Why it's a quirk:** a 50/50 split is expected to track on resize, or at least
  to survive a transient shrink. Both fail: resize is non-proportional, and the
  shrink-clamp is destructive (permanent position loss). Surfaces on 2026 hardware
  where window/scale changes are routine.
- **Where:** `Thinlet.java` splitpane layout (~457-475): the `divider > maxdiv`
  branch overwrites `divider` with `maxdiv`; there is no proportional rescale.
- **Locked by:** `thinlet.trace.InputSplitPaneTest`
  `#dividerIsAbsolutePixels_nonProportionalAndDestructiveClampOnResize` (tagged
  documents-current-behavior).
- **Enhanced Thinlet disposition:** fix — preserve the ratio on resize (or at least
  restore the remembered divider after a transient shrink).

### Q3 — `getIcon` silently returns `null` for a missing/unloadable resource
- **What happens:** `getIcon(String path, boolean preload)` resolves an icon via
  `getClass().getResource(path)` / `getResourceAsStream(path)`; when neither
  resolves it returns `null` — the classpath lookup misses, the `new URL(path)`
  fallback throws `MalformedURLException` (a protocol-less `/icon/...` path), and
  **both** attempts are swallowed by empty `catch (Throwable e) {}` blocks. No log,
  no throw. The widget then lays out and paints as if icon-less (icon width/height
  count as 0; the guarded `drawImage` is skipped).
- **Why it's a quirk:** a missing resource is a configuration error that should be
  surfaced (log or throw), not silently absorbed. It is exactly what hid the fact
  that the corpus referenced 25 `/icon/*.gif` assets that were never vendored — every
  reference resolved to `null` and every golden was captured blank (fixed by D54,
  which restores the 24 authentic assets that shipped; `volume.gif` was genuinely
  absent in 2005 and stays null).
- **Where:** `Thinlet.java` `getIcon(String, boolean)` (~6212-6249; empty catches at
  ~6222-6223 and ~6236-6237; `getIcon(Object, String, Image)` at ~6147-6150 reads the
  possibly-null value back out of the widget map).
- **Locked by:** `thinlet.quirks.GetIconSilentNullQuirkTest` (tagged
  documents-current-behavior). The *library* silent-null is preserved here; the
  separate `thinlet.trace.CorpusResourceResolutionTest` guard ensures our own
  fixtures never *rely* on it (it fails the build on any unresolved corpus resource,
  save the documented 2005 gap `/icon/volume.gif`).
- **Enhanced Thinlet disposition:** fix — log or throw a descriptive error on an
  unresolved/unloadable resource instead of returning `null`.

### Q4 — spinbox `value` attribute is dead storage; the spin state lives in `text`
- **What happens:** the DTD registers an integer `value` attribute on `spinbox`,
  but spinning (arrows or Up/Down keys) reads and writes only the `text` string;
  a parsed `value` is stored and never touched again.
- **Why it's a quirk:** two same-named states, one dead — `getInteger(spinbox,
  "value")` reads back whatever the XML set (or 0), never the live number. The
  2005 source itself annotates the registration `// == text? deprecated`.
- **Where:** `Thinlet.java` `processSpin` (reads/writes `"text"` only);
  `DescriptorTable.java` spinbox `value` registration.
- **Locked by:** `thinlet.trace.InputSpinBoxTest`
  `#valueAttributeIsDeadStorage_theSpinStateLivesInText` (tagged
  documents-current-behavior).
- **Enhanced Thinlet disposition:** fix — reconcile (make `value` the live state)
  or remove the dead attribute.

### Q5 — spinbox `editable="false"` gates typed digits but not spinning
- **What happens:** a non-editable spinbox rejects typed characters, yet the
  mouse arrows and Up/Down keys still change the value freely.
- **Why it's a quirk:** `editable` reads as "read-only" but only gates the
  text-entry path (`processField`); neither spin path checks it.
- **Where:** `Thinlet.java` — the spinbox key branch and mouse branch call
  `processSpin` unconditionally; the `editable` gate lives inside `processField`.
- **Locked by:** `thinlet.trace.InputSpinBoxTest`
  `#nonEditableSpinboxStillSpinsViaArrowsAndKeys` (tagged
  documents-current-behavior).
- **Enhanced Thinlet disposition:** undecided — flag for the maintainer (either
  gate spinning too, or rename/re-document the attribute).

### Q6 — slider press teleports the knob to the pointer; no track-paging
- **What happens:** pressing anywhere on a slider jumps the value proportionally
  to the pointer position — there is no knob/track distinction and no
  click-to-page-by-`block` semantics (the `block` attribute affects only
  PageUp/PageDown).
- **Why it's a quirk:** most toolkits page toward the click when the track is
  pressed; Thinlet's whole widget is one jump-to-pointer hit region. Arguably a
  feature; locked so a Cut 6 refactor cannot change it silently.
- **Where:** `Thinlet.java` slider branch of `handleMouseEvent` (press and drag
  share the proportional formula; `setReference` centers on the knob half-width).
- **Locked by:** `thinlet.trace.InputSliderTest`
  `#trackPressTeleportsTheKnob_noPageStepSemantics` (tagged
  documents-current-behavior).
- **Enhanced Thinlet disposition:** keep — flag for maintainer confirmation.

### Q7 — dialog title glyphs were paint-only — **fixed in 0.2.x (D73)**
- **What happened (≤0.1.x):** the `closable`/`maximizable`/`iconifiable`
  attributes drew the familiar X/box/underscore glyphs in the dialog header, but
  no click wiring existed for any of them: the hit-test mapped the entire title
  strip to `"header"`, so clicking the X did nothing and dragging on it moved
  the dialog.
- **The fix (maintainer's pick):** the **close glyph is live** — its rect is a
  `":close"` part; release over it removes the dialog (release anywhere else
  cancels, and the glyph is no longer a drag handle). The **maximize/iconify
  glyphs are no longer drawn** — they never had wiring; their attributes stay
  parseable and inert until a real windowing story exists. The Drafts demo was
  deliberately left untouched.
- **Where:** `Renderer` dialog branch (glyph painting), `findComponent`'s dialog
  branch (the `":close"` carve-out), `handleMouseEvent`'s dialog branch (the
  release-to-close wiring).
- **Locked by:**
  `thinlet.trace.InputDialogTest#closeGlyphClosesTheDialogAndTheOtherGlyphsAreUndrawn`
  (asserts the fixed contract; red-green checked both ways).

### Q8 — Drafts FolderBrowser NPE'd off-Windows (hardcoded `C:` root) — **fixed in 0.2.x (D72)**
- **What happened (≤0.1.x):** the Folder browser page rooted its lazy filesystem
  tree at the hardcoded path `C:`; expanding it called `new File("C:/").list()`,
  which returns `null` on any non-Windows system, and the unguarded `.length`
  dereference threw `NullPointerException` — surfaced as a live ExceptionDialog
  via `Drafts.handleException`.
- **The fix:** the tree roots at `File.listRoots()` (falling back to
  `user.home`), and `expand` treats a `null` listing as an empty directory.
- **Where:** `thinlet-drafts` `FolderBrowser.init` / `FolderBrowser.expand`.
- **Locked by:** `thinlet.trace.DraftsPlaythroughTest`
  `#folderBrowserExpandsTheRealFilesystemRootGracefully` (asserts the fixed
  contract; red-green checked both ways). SpotBugs additionally guards the
  null-deref family — the `NP_NULL_PARAM_DEREF` /
  `NP_NULL_ON_SOME_PATH_FROM_RETURN_VALUE` exclusions came off with D71/D72.

### Q9 — the combobox icon glyph is click-dead
- **What happens:** an editable combobox with an `icon` attribute hit-tests the
  glyph's strip (`x <= 2 + iconWidth`) as its own `"icon"` part, and the mouse
  handler's click branch excludes exactly that part: clicking the icon neither
  opens the drop-down, nor moves the caret, nor fires anything — while the same
  click one pixel to the right edits text, and the arrow strip opens the list.
- **Why it's a quirk:** a hit-tested region with no behavior is a dead zone the
  user can feel; the 2005 code names the part but never wires it.
- **Where:** `Thinlet.java` — `findComponent`'s combobox branch yields `"icon"`;
  `handleMouseEvent`'s combobox branch handles `part == null` (text) and
  `!is(part, "icon")` (drop button), leaving `"icon"` to fall through.
- **Locked by:** `thinlet.trace.InputQuirkPinsTest#clickingTheComboboxIconGlyphDoesNothing`
  (tagged documents-current-behavior).
- **Enhanced Thinlet disposition:** undecided — plausible fixes: treat the icon
  as part of the text area, or drop the dedicated part token.

### Q10 — ascending sort draws a downward triangle
- **What happens:** a table column with `sort="ascent"` draws the south-pointing
  (downward) header glyph, `sort="descent"` the north-pointing one.
- **Why it's a quirk:** most toolkits draw ascending sort as an upward wedge;
  2005 Thinlet's mapping is inverted relative to that convention (though
  self-consistent — it can be read as "smallest at top").
- **Where:** `Renderer.java` — the table-header branch's sort-glyph `arrow(...)`
  call (`is(sort, "ascent") ? 'S' : 'N'`).
- **Locked by:** `thinlet.trace.InputQuirkPinsTest#ascendingSortDrawsADownwardTriangleGlyph`
  and `#descendingSortDrawsAnUpwardTriangleGlyph` (tagged documents-current-behavior).
- **Enhanced Thinlet disposition:** undecided — flipping it is user-visible;
  document-or-flip is a 3c call.

## Triaged for Enhanced Thinlet (not behavior-locked)

Findings investigated but *not* pinned by behavior tests, with reasons — the
first two are Phase-1 D13 candidates that remain SpotBugs suppressions
(`config/spotbugs/exclude.xml`); later entries cite their own D-numbers. All are
Enhanced Thinlet's to address.

- **XML parser "unclosed stream" (`OBL_*`, `OS_OPEN_STREAM`).** Not reproducible
  as a leak: `parse(InputStream, char, Object)` closes the `Reader` (which closes
  the underlying stream) in a `finally` on every practical path. The SpotBugs
  finding is the exception-edge of the `Reader` being constructed just outside the
  `try` (~6601) — there is no behavioral quirk to lock; tightening the cleanup is
  an Enhanced Thinlet nicety.
- **`FileChooser` fallback `View.getFiles` null deref — fixed in 0.2.x (D72).**
  `File.list()` can return `null` (non-directory / I/O error) and was
  dereferenced unguarded in the `View` *fallback* inner class (used only when
  the Swing-backed `View2` fails to load). The guard now returns an empty
  array. Not test-locked (`thinlet-demos` has no test harness and `View` is a
  private inner class — recorded honestly in D72); guarded instead by SpotBugs,
  whose null-deref exclusions came off with this fix.
- **`checkLocation` passed `mousex` for the y argument (D67/D68) — fixed in
  0.2.x (D70).** After a layout change under a stationary cursor, hover state
  was re-synthesized via `handleMouseEvent(mousex, mousex, …, MOUSE_ENTERED,
  …)` — y received the x coordinate. Traced as unobservable before fixing
  (D68): `findComponent` had already recomputed `mouseinside`/`insidepart`
  from the correct coordinates, no MOUSE_ENTERED consumer reads the raw x/y
  parameters, and nothing persisted the corrupted value. The argument now
  passes `mousey`; the full net was empirically indifferent (zero golden
  diffs), confirming the D68 proof. Guarded by the canary
  `thinlet.trace.InputQuirkPinsTest#closingTheDropDownUnderTheCursorCommitsAndStaysConsistent`,
  which drives the path with differing x/y and fails if a change ever makes
  the parameter live-and-wrong.
