# Next steps — session handoff (2026-09-02)

> State pointers + ordered work only; rationale lives in `DECISIONS.md`
> (single-home rule + comment rules: **D57**). Charter:
> `project-docs/PHASE-3-GOALS.md`.

## State

- **Cuts 1–3 done** (D42–D59). Cut 3 landed 2026-07-14: net #81, typed core
  D58 (one recorded divergence there), façade cleanup + close D59.
- **Cut 4 net prerequisite done (D61)**: layout-state sidecar goldens pin
  `:port`/`:view`/`:widths`/`:offset` (bidirectional regression + permanent
  coverage guard). Its residual `:view.x` gap is closed (D84).
- **Tooltip captured (D62)**: the last D45-deferred interaction state; every
  interaction state D45 enumerated is now guarded.
- **Cut 2 fully closed (D63)**: `paintDesktop`/`paintReverse` moved to
  `Renderer` behind the D62 golden — every 2005 paint branch body now lives
  in `Renderer`; `Thinlet` keeps only the D50-gated shared paint helpers.
- **Input blind spot closed (D64, three slices)**: 58 characterization tests
  across spinbox/slider/tabbedpane/scrollbar-mouse (A), menubar/context-menu
  (B), focus/clipboard/dialog/tooltip-hide (C); quirks Q4–Q7 locked;
  Phase 2.y finished — the Cut 6 net is in place.
- **Testkit + live-Drafts playthrough done (D65, PRs #92/#93)**: the harness
  ships as the `thinlet-core` test-jar (no module — the D37 reactor cycle
  never materializes); `InputDriver.attach` drives existing hosts;
  `DraftsPlaythroughTest` (12 scenarios, deterministic-page allowlist) makes
  the Drafts app the first 3b living test bed; Q8 locked.
- Net: 41 static + 51 interaction goldens + 58 layout-state sidecars + input
  suite + 25 contract pins (`DescriptorContractTest`) + the live playthrough;
  strict-intern tripwire live in every test JVM (D43, both modules). The
  current base-row count lives with the newest state bullet below.
- **Vocabulary decode + constants research done (D67, 2026-07-15)**:
  `project-docs/VOCABULARY-INVENTORY.md` (11 vocabularies, collision table,
  absorb-at-cut recommendations) + in-source annotations at the consumers.
- **D67 candidates pinned (D68, 2026-07-16)**: Q9 (then-click-dead combobox icon) +
  Q10 (ascent-sort down-arrow) locked by `InputQuirkPinsTest`; the
  `checkLocation` mousex-for-y bug proven unobservable and triaged (not
  behavior-locked), guarded by a canary. Base row: +4 tests.
- **3c opened (D69, 2026-07-17)**: `main` is the enhanced line
  (0.2.0-SNAPSHOT); v0.1.x is the frozen modernized-2005 line (`v0.1.0` tag);
  behavior changes go through the D69 protocol. Fork mapping unaffected,
  arrival-triggered. The D69 quirk-fix batch completed 2026-07-17 (D70–D73:
  `checkLocation` y-arg, Q1 parser null-source, `FileChooser` guard + Q8 root
  + SpotBugs null-deref exclusions off, Q7 close glyph live /
  maximize+iconify undrawn) — zero golden re-records across the batch.
- **Public vocabulary shipped (D74, 2026-07-18, PR #105 merged)**: 8 choice
  enums (each with `KEY` + DTD tokens + `fromToken`) and the 11 `EventNames`
  constants — pure API addition, new files only, welded to the definition
  table by `PublicVocabularyContractTest` (+7 tests). Opus-reviewed
  (ship-with-nits, applied); maintainer signed off the `Alignment` naming and
  auto-merge in-session (recorded in D74). japicmp additions-only vs v0.1.0;
  post-merge `main` run green. Base row: 337 (core) + 13 (drafts) tests.
- **Quirk dispositions settled (D75, 2026-07-22)**: Q5 fixed (`editable="false"`
  gates spinning too, gate in `processSpin`), Q9 fixed (combobox icon strip folds
  into the text area, part token kept), Q6 kept (slider jump-to-pointer), Q10 kept
  (inverted sort glyph — documented, not flipped). Q11 added and fixed in the same
  batch: an explicit `sort="none"` had drawn the `"descent"` glyph. Zero golden
  re-records. Three `documents-current-behavior` tags off, two new pins. Base
  row: 339 (core) + 13 (drafts) tests.
- **Q12/Q13 catalogued then fixed (D76/D77, 2026-07-22)**: the last two D64
  candidates were pinned-but-uncatalogued; D76 wrote them up, D77 fixed both —
  a tab with no focusable content keeps focus on the pane (the pane asks before
  it walks, via the new `hasFocusableInside`), and a disabled menu item swallows
  the release with the popup left open. Zero golden re-records; two more
  `documents-current-behavior` tags off. (This entry once read "the quirk backlog is
  now empty" — it was wrong; see the 2026-08-15 bullet.)
- **Table behavior recorded (D78, 2026-07-23)**: `InputTableTest` (14 tests) +
  `input/table2.xml` close the last major input-net gap — selection by mouse and
  keyboard, shift/control paths, `interval` vs `multiple`, `perform`, and the
  header. Found Q14 (the column header is inert — an empty `if` body where
  hit-testing belongs) and that a double-click fires `action` once, not twice.
  Recording only, zero behavior change. Base row: 353 (core) + 13 (drafts).
- **local-ci single-test filter (D79, 2026-07-24)**: `.devcontainer/ci/local-ci.sh
  -t <pattern>` runs one suite on the `test` goal (composes with the JDK-row arg).
  Tooling only; no library change.
- **`InputDriver.origin` made scroll/header-aware (D80, 2026-07-31)**: `origin`
  now adds each parent's `:port − :view` offset, so `d.click(child)` lands true
  inside a headered/scrolled container; `InputTableTest.clickRow` deleted. No
  live-suite moved (list/tree tolerant; splitpane/scroll unaffected); one
  interaction golden (`table-selected-lead-focus`) re-recorded because its click
  now lands on the intended row 1 instead of mis-aiming to row 0. Test harness
  only; no library change.
- **Three dispositions authorized, one correction (2026-08-15)**: Q2/Q3/Q4 carried
  `disposition: fix` citing **no** D-entry — proposals, not decisions — while this
  file reported the backlog as empty. The maintainer settled all three, and held the
  3a Cut 4/5/6 gate closed despite the fork delay. **Q3 step 1 done (D81)**: the
  icon miss logs at WARNING (and FINE per attempt, plus a `MediaTracker` check for
  resolved-but-undecodable) — **log only, no throw**, return value untouched, so no
  golden moved; step 2 (a supplied missing-image indicator) is deferred with a brief
  in KNOWN-QUIRKS Q3 and the ROADMAP 3c backlog.
- **Q2 done (D82, 2026-08-15)**: the layout clamp no longer overwrites the requested
  `divider`, so a transient shrink is lossless. Not the one-liner it looked like —
  `Renderer` paints the bar from the same value layout was clamping, so the effective
  position is published as the reserved `:divider` and read by the renderer and the
  keyboard step (the drag path derives its own and needed nothing). Non-proportional
  resize stays 2005 by choice. No golden re-record.
- **Q4 done (D83, 2026-08-15)**: `value` and `text` mirror each other on all four write
  paths (spin, typed-digit commit, the two public setters, XML parse), so the
  DTD-declared integer is live. `text` stays authoritative and wins a declared conflict
  in either parse order; non-numeric text leaves `value` alone. The attribute is kept —
  removing it would edit the verbatim 2005 DTD (D8). No golden re-record.
  **The quirk backlog is empty again — this time checked against `KNOWN-QUIRKS.md`
  dispositions, not memory.** Base row: 361 (core) + 13 (drafts).
- **`:view.x` pinned (D84, 2026-08-16)**: the D61 residual gap closed — every one of
  the 58 sidecars had `:view.x == 0`, and the coverage guard's either-axis check
  called that covered. New `arrows-hlist-scrolled-right` scenario (knob drag to the
  clamp; the wheel cannot scroll horizontally) plus a per-axis guard. No existing
  golden moved. Test net only; no library change. Base row: 363 (core) + 13 (drafts).
- **Parser SAX/DOM modes netted (D86, 2026-09-02)**: `loop-modernise`'s first
  completed `--dry-run` proposed a slice whose green rows were partly hollow —
  five of its nine edits sat in the `'D'`/`'S'` branches of `parse`, which no test
  called. `ParserSaxModeTest` + `ParserDomModeTest` pin those branches through the
  SAX callbacks and the `getDOM*` accessors; the five lines were each mutated to
  prove the suites have teeth (15 of 19 failed), then reverted. The slice re-run
  against them passes and stays stashed, uncommitted. Test net only; no library
  change. Base row: 382 (core) + 13 (drafts).

- **Rendering hints netted, Q15 found and fixed (D88, 2026-09-05)**:
  `TracingGraphics2D.setRenderingHint` delegated without recording, so the trace was
  identical whether `Thinlet.paint` set both antialiasing hints or neither. Making it
  observable exposed **Q15** within one CI run: the 2005 code cached the reflective
  `setRenderingHint` `Method` in a static keyed to the first `Graphics` class, so a
  second implementation latched antialiasing **off for the whole JVM**. Fixed under
  D69 with `loop-modernise`'s own slice (`instanceof Graphics2D` + direct calls),
  pinned by `AntialiasingPersistenceTest`. 93 paint goldens re-recorded, every file
  `+2/-0` with exactly two distinct lines added and none removed; dropping one hint
  now fails 93 of 94 golden tests. Second instance of the loop finding code the net
  did not watch (after D86). Base row: 383 (core) + 13 (drafts).

## Next work, in order (3c open per D69 — the enhanced line is `main`/0.2.x)

1. **Q14 (inert table column header) — parked, not open** — held deliberately until
   the fork sources land, because wiring a header click adds *new* public behavior
   the maintainer's own fork may already define (D78). Q6/Q10 stay kept (D75), and
   Q2's non-proportional half stays 2005 by choice (D82).
2. **No other open dispositions** — the three authorized on 2026-08-15 are done
   (D81/D82/D83). Further 3c work starts from fresh recording (drive a widget, assert
   what it does today, then decide) rather than a queue.
3. **Fork mapping (arrival-triggered; no expectations built on it)** — sources still
   pending (2026-08-15: not arrived, a month past the expected window). The gate
   covers **only** the Cut 4/5/6 seam commitments (D48/D50/D61/D69), never net or
   preparatory work. When they land: fork files → subsystems; boundaries vs Cut 2–6
   seams; enhancement backlog; then Cut 4+ seam commitments unblock (3a resumes).
4. **`loop-modernise` — merged to `main`, first real run capped at 3 (D87)** —
   the script is trunk tooling now, not branch-only work. Runs happen in a linked
   worktree on their own branch off `main`; the primary checkout is never the
   target. Still unexercised: the repair path (no slice has failed verification)
   and the commit path. The XML parser is fenced (D86 + the ROADMAP 3c question),
   so no slice may touch it. The D85 entry in `project-docs/UNFINISHED-IDEAS.md`
   is now a removal candidate.

## Discipline (one-liners; the D-entries carry the why)

- Goldens only in the CI container, `clean` before record, never re-record to
  make an *unexplained* diff go away (D44/D52); on the enhanced line a
  re-record must cite the authorizing D-entry and cover only the affected
  scenarios (D69). Never modify existing fixtures — new files only.
- Behavior changes (3c): disposition first, flip the pin in the same PR, tag
  off `documents-current-behavior`, KNOWN-QUIRKS entry → "fixed in 0.2.x"
  (D69).
- Golden signal strength: force categorical diffs (font point-**size**, not
  `bold`) (D52); auto-repeat parts need the no-op-press trick (D51).
- Mechanical changes: scripted with boundary assertions + round-trip audit;
  check argument *names*, never blanket-regex quoted spans (D52/D56).
- Before typing/moving an unpinned path, land its pin first — new files only
  (D50/D56).
- Paint-side writes: hoist, don't relocate; widen on demand; comment every
  widening (D48).
- Every slice: local gates → container base + 8/11/17 → PR → delegated
  squash-on-green auto-merge (D46); watch CI to green; docs accurate as-of-merge.
- Docs/comments: single-home rule + the three comment rules (D57).
- Maintainer grants (2026-07-09): continue at lulls; Opus (not Fable)
  self-review at lulls with document+PR+merge rights; spell out "if and only
  if".
