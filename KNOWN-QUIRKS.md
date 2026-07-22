# Known Quirks

This file catalogs behaviors of the 2005 Thinlet codebase that the maintainer
believes are *wrong, surprising, or buggy*. Each is pinned by a test, so it can
only change deliberately.

## Why quirks are pinned

The pin means different things on the two lines (D69):

- **v0.1.x — the frozen modernized-2005 line.** Its contract is *preserve 2005
  Thinlet's observable behavior and modernize the toolchain around it*, so every
  quirk here is preserved exactly.
- **`main` (0.2.x, Enhanced Thinlet).** Entries are picked off deliberately, one
  recorded disposition at a time: the D-entry decides, the pin flips to the new
  behavior in the same PR, and the entry below is retitled **fixed in 0.2.x**
  with the authorizing D-number. Entries with a *keep* disposition stay pinned
  and tagged — the behavior they document is still current.

Either way:

- **Pinning tests describe current behavior, bugs included.** A failing pin means
  the code drifted, *not* that Thinlet was "right" before.
- Unfixed quirks are pinned by at least one test tagged
  `@Tag("documents-current-behavior")`; `mvn test
  -Dgroups='!documents-current-behavior'` runs the subset that excludes them.
  A fixed entry's test loses that tag — it now asserts a chosen contract.

## Entry format

```
### Q<n> — <short title>                       (unfixed)
- **What happens:** observable behavior.
- **Why it's a quirk:** expected vs. actual.
- **Where:** Thinlet.java location(s).
- **Locked by:** test class/method name (tagged documents-current-behavior).
- **Enhanced Thinlet disposition:** keep / fix / undecided (cite the D-entry once decided).

### Q<n> — <short title> — **fixed in 0.2.x (D<n>)**
- **What happened (≤0.1.x):** the old behavior, and why it was wrong.
- **The fix:** the new contract.
- **Where:** location(s).
- **Locked by:** test asserting the fixed contract.
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

### Q5 — spinbox `editable="false"` gated typed digits but not spinning — **fixed in 0.2.x (D75)**
- **What happened (≤0.1.x):** a non-editable spinbox rejected typed characters,
  yet the mouse arrows and Up/Down keys still changed the value freely.
  `editable` read as "read-only" but gated only the text-entry path
  (`processField`); neither spin path checked it.
- **The fix:** `editable="false"` is read-only on every value path. The gate
  sits in `processSpin`, the single choke point for all three callers — the
  Up/Down key branch, the arrow-block press, and the auto-repeat timer; the
  false return also stops a press from arming the 375 ms repeat.
- **Where:** `Thinlet.java` `processSpin`.
- **Locked by:** `thinlet.trace.InputSpinBoxTest`
  `#nonEditableSpinboxRejectsSpinningAsWellAsTyping` (asserts the fixed
  contract; each no-op is paired with the same gesture on an editable sibling).

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
- **Enhanced Thinlet disposition (D75):** keep. Confirmed as a feature, not a
  defect; the pin stays as the guard against silent drift in a later refactor.

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

### Q9 — the combobox icon glyph was click-dead — **fixed in 0.2.x (D75)**
- **What happened (≤0.1.x):** an editable combobox with an `icon` attribute
  hit-tested the glyph's strip (`x <= 2 + iconWidth`) as its own `"icon"` part,
  and the mouse handler's click branch excluded exactly that part: clicking the
  icon neither opened the drop-down, nor moved the caret, nor fired anything —
  while the same click one pixel to the right edited text. A hit-tested region
  with no behavior is a dead zone the user can feel.
- **The fix:** the icon strip is part of the text area. The text branch accepts
  the `"icon"` part, so the strip takes the caret (parking at the start, since
  the click lands left of the text origin), the text cursor, and the icon-width
  caret offset the branch already computed. The part token is kept, so the
  drop-button branch is untouched. Editable comboboxes only — `findComponent`
  reports `"down"` for the whole widget when `editable="false"`.
- **Where:** `Thinlet.java` — `handleMouseEvent`'s combobox branch.
- **Locked by:** `thinlet.trace.InputQuirkPinsTest`
  `#clickingTheComboboxIconGlyphPlacesTheCaretLikeTheTextArea` and
  `#hoveringTheComboboxIconGlyphShowsTheTextCursor` (the fold is pinned as
  total, not click-only).

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
- **Enhanced Thinlet disposition (D75):** keep. The glyph is pure decoration —
  Thinlet never sorts data, so nothing reads the direction — and flipping it is
  asymmetric: naive apps improve, but any app that compensated for the inversion
  (writing `descent` to get the arrow it wanted) silently becomes wrong the
  other way, and we cannot detect which exist. Documented rather than flipped.

### Q11 — an explicit `sort="none"` drew the descending glyph — **fixed in 0.2.x (D75)**
- **What happened (≤0.1.x):** the header painter skipped the sort glyph only
  when the attribute was unset (`null`), and `setChoice` stores `"none"`
  verbatim — it substitutes the row default only for a `null` *value*. So
  `sort="none"` painted the same north triangle as `sort="descent"`, while an
  unset `sort` painted nothing and `getChoice(column, "sort")` reported
  `"none"` for both.
- **The fix:** `"none"` paints no glyph, so the paint agrees with the accessor
  and with the unset attribute.
- **Where:** `Renderer.java` — the table-header branch's sort-glyph guard.
- **Locked by:** `thinlet.trace.InputQuirkPinsTest#explicitSortNoneDrawsNoGlyphAtAll`
  (proven against the 2005 `'N'` glyph first, then flipped in the same PR).

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
