# Next steps — session handoff (2026-09-07)

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
  against them passes. That slice was never committed and its stash is gone; six of
  its nine edits sit inside the now-fenced `parse`, and the other three are in
  `addAttribute`/`getMethod` where a later run will find them again. Test net only;
  no library change. Base row: 382 (core) + 13 (drafts).

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
- **Run-2 landed (D89, 2026-09-05, PR #137)**: the loop committed 3 of 3 again — the
  deprecated `Integer`/`Long` constructors, `getSelectedItems`' quadratic regrow,
  and the four `new String(…)` copies outside the fenced parser that D86 predicted
  a later run would find. Reviewed by **14 mutation runs**, not by inspection: the
  slices' live edits are watched, and the four green probes are the finding.
  **Blind spot #3** — `TracingGraphics2D` records only a scaled blit's destination
  rect, so `Thinlet.fill`'s gradient sampling is unobservable (collapsing the
  source rect to 1×1 passes 94/94; moving the destination fails 76/94). And a
  **third category** beyond D86's uncovered code and D88's unrecorded effects:
  `evm = -1` turns the whole Insignia workaround **on** and passes all 189
  goldens, because a 1 px delta is inside D7's ±2 px tolerance — recorded,
  executed, still invisible. `RENDERING-PRIMITIVES.md`'s `drawImage` row was wrong
  in consequence and is corrected; the EVM disposition is a new ROADMAP 3c backlog
  item. Base row unchanged (383 core + 13 drafts).
- **Run-1 landed (D87/D88, 2026-09-05, PRs #133/#135)**: the loop's first real run
  committed 3 of 3 slices, 17 Maven builds, no failures, no repairs. All three are
  on `main`. It also exposed two `.git`-is-a-file bugs in this repo's own scripts —
  `loop-modernise.sh`'s `flock` (#131) and `comment-pass.sh`'s attestation marker
  (#134) — both fixed; a linked worktree's `.git` is a file, so any script writing
  beneath it breaks. Check new helper scripts for the same assumption.

- **Coverage instrumented, tier 1 answered (D90, 2026-09-05)**: JaCoCo behind an
  opt-in `coverage` profile + `scripts/coverage.sh`; reports, never thresholds, and
  no gating row runs it. One shared exec file so the `thinlet-drafts` playthrough
  counts towards `thinlet-core`. Baseline **85.9 % instructions / 74.0 % branches /
  89.2 % methods**; `FrameLauncher` **0 %** (whole public class), `Thinlet` 84.2 %,
  `Renderer` 90.1 %, everything else 100 %. 27 methods never entered — including
  `findText` (type-ahead), `selectAll`, `hasAccelerator` (**accelerator dispatch has
  never been exercised**) and the public `getItems`. Coverage answers only "did this
  line run?": every D89 gap sits on a covered line, so the mutation probe stays the
  instrument for the other two tiers. Tier 2 gained a second confirmed instance —
  the trace records image geometry, not identity (swapping `hgradient`/`vgradient`
  passes 94/94) — and tier 3's band is now measured: text at **+2 px passes, +3 px
  fails 39 of 41**.
- **Pure-logic surface extracted to static seams (D91, 2026-09-05)**: 42 method
  names in `Thinlet.java` are now package-private `static`, in three tiers — 8
  already-static widenings, 12 pure bodies needing no `Thinlet` parameter, and 22
  threading an explicit `Thinlet t` in the D48 style `Renderer` already uses.
  Tests reach package-private but not private, so D90's named worst-covered
  methods — `hasAccelerator` (never entered), `findText`, `changeCheck` (2 of 16
  branches), `getListItem`, `processList`, `getChars` — were unreachable by any
  plain unit test. **25 of 27 candidates referenced zero instance fields**: the God
  class keeps its state in the `Object[]` model passed in, not in fields. Audit:
  all 1 294 string literals byte-identical to `main` (D52/D56). Four JDK rows
  green, zero golden re-records, japicmp clean. `parse` deliberately untouched
  (already reachable via three public entry points, D86). Base row unchanged
  (383 core + 13 drafts).
- **Mutation testing landed (D92, 2026-09-05)**: `pitest` behind an opt-in
  `mutation` profile, plus `scripts/mutation.sh` and `scripts/mutation-summary.py`.
  Reports, never gates. Two traps recorded, both of which produce a green build that
  measured nothing: `targetTests` matches fully-qualified names only, and PIT
  auto-adds `java.awt.headless=true`, which kills every Xvfb-dependent test.
- **The characterization loop landed (D93, 2026-09-05)**:
  `scripts/loop-characterise.sh`, `loop-modernise`'s mirror image — test tree
  writable, `guard_no_main` absolute, one new file per slice, targets from a
  pure-logic allowlist ordered by `coverage.sh --worklist`. Firing the guards
  deliberately caught the load-bearing one silently passing a modified
  `Renderer.java` (a wildcard git pathspec matches the whole path, so
  `*/src/main/java` matches the directory and nothing beneath it).
- **The gate is scoped to assigned lines, not whole methods (D94, 2026-09-05)**:
  the first `--dry-run` declined `parse`, correctly — a whole-method
  "zero survivors" rule is unreachable wherever equivalent mutants live. Scoping to
  the slice's assigned missed lines fixed it. D94 also corrects D92's survivor count
  (16 in `parse`, not 8) and withdraws an inference that a comment containing `>`
  mis-parses; it does not.
- **Loop run 1 (D95, 2026-09-05)**: 3 of 3 committed, no decline, no repair —
  `parse`, `getListItem`, `processList`, zero surviving mutants on every slice.
  Found **Q16** (GUI-mode `parse` silently discards a tag's body text) and **Q17**
  (`End` is a no-op on a list with no lead, while `Home` works), both
  `disposition: undecided`. Coverage **85.9 → 87.4 %** instructions, **74.0 →
  76.3 %** branches, 27 → 25 never-entered methods. Base row: **425** core + 13
  drafts.
- **XML dialect written down (D96, 2026-09-06)**:
  `project-docs/backend-portability/XML-DIALECT.md` is the specification, with
  every claim citing its pin — `ParserDialectTest` (28) for the lexical and
  mixed-content rules, `XmlDialectGrammarTest` (10) for the grammar, the DTD
  comparison and the corpus run. The structural grammar is a **generated** test
  resource (`test/resources/dialect/thinlet-dialect.dtd`, from
  `DescriptorTable` + 35 × 35 `addImpl` pairs), regenerated and compared every
  run, so it cannot drift the way the shipped DTD did. **41 of 42 corpus files
  validate clean**; the 42nd (`drafts/lists.xml`) is not well-formed XML at all —
  two buttons carry `text="<"` / `text=">"`. Four DTD defects pinned (the `gt`
  entity is `=`; zero `#PCDATA`; `desktop` carries the panel attribute list;
  `button` omits `for`) — points 3 and 4 are the *complete* attribute-name
  disagreement across all 35 elements, not a spot check. Four new quirks, all
  `undecided`: **Q18** markup declarations end at the first `>`, **Q19** the
  declared encoding re-encodes with the character count as a byte count, **Q20**
  a stray end tag throws NPE, **Q21** text before a child is lost in every mode.
  Two findings recorded but deliberately unpinned (triage section): truncated
  input **hangs** in five loops, and `parse`'s inner comment reader is
  **unreachable** — the dead code behind D95's seven `NO_COVERAGE` mutants. No
  library change; `thinlet.dtd` untouched. Base row: **463** core + 13 drafts.
- **The parser question is settled: replace (D98, 2026-09-07).** A throwaway JAXP
  spike — two variants, run against the whole net on JDK 21 **and** JDK 8 and then
  reverted — measured the swap instead of arguing it. **22 of 463 core tests fail,
  the identical set on both rows**; 38 of 41 static paint goldens are
  byte-identical; the entire input net and D86's 19 SAX/DOM-mode tests are green;
  the compiled `protected`+ surface does not move (71 members, `javap` both ways),
  so japicmp sees nothing. Of the three blockers the ROADMAP recorded, only one was
  real: whitespace collapsing is a **14-line** helper (worth exactly 4 tests), the
  protected callback *sequence* survives intact, and `AmazonExplorer` works except
  `convertHTML` — which matches the literal `"&lt;P>"` and so depends on entities
  **not** being decoded in element text (3-line fix). The real blocker is raw
  `<`/`>` in an attribute value: `drafts/lists.xml` becomes unreadable and the
  Drafts Lists page dies silently (`Drafts.showDraft` only `printStackTrace`s).
  Two goldens change because the swap **repairs** Q19 corrupting
  `drafts/internationalization.xml` (not ASCII — ISO-8859-2 Hungarian pangram,
  golden mangled *and* truncated) and `drafts/widgets.xml` (`text="Label &#169;"`
  → golden `Label Â`); both have been recorded corrupt since PR #9, unread because
  the docs asserted the corpus was pure ASCII. Costed the other way too: `parse`
  hangs on truncated input and `AmazonExplorer` parses an HTTP stream. Posture
  settled by the maintainer in session — **clean break, 0.2.x reads XML, v0.1.x
  stays the line that reads the 2005 dialect**. Decision entry only; no library
  change, no golden moved. Base row unchanged (463 core + 13 drafts).

## Next work, in order

0. **The parser swap (D98) — the next code PR.** The D97 freeze is lifted and the
   decision is made; what remains is the implementation, and it is fully scoped by
   the spike's measurements. The PR: a JAXP `DefaultHandler` plus the 14-line
   whitespace-collapse helper replacing `parse`'s 207-line body (the same
   `addElement`/`addImpl`/`addAttribute`/`finishParse` calls, so the definition
   table, the `i18n.` bundle lookup and every `IllegalArgumentException` message
   are untouched); SAX hardening (secure processing, external general and parameter
   entities off, no external DTD, no XInclude) — the 2005 parser could fetch
   nothing, so this is a cost of the swap, not a benefit; `SAXParseException` →
   `IllegalArgumentException` so malformedness keeps its 2005 type; **17 pins
   flipped** (16 in `ParserDialectTest`, 1 in `ParserSyntaxTest`, and
   `XmlDialectGrammarTest#theOneCorpusDocumentAStandardXmlParserRejectsIsStillParsedByThinlet`),
   each `documents-current-behavior` tag off; `internationalization.json` and
   `widgets.json` re-recorded citing D98 and nothing else; `convertHTML` fixed in
   `AmazonExplorer`; and a **new** well-formed sibling for `drafts/lists.xml` —
   the imported file stays exactly as it is (D9/D12), so the Drafts Lists page and
   its playthrough scenario get a document that a conforming parser can read.
   Q16, Q18, Q19, Q20 and Q21 are all retitled **fixed in 0.2.x (D98)** by that one
   PR; none of them is fixable alone. A shipped replacement DTD derived from D96's
   generated grammar is a **separate** decision afterwards — nothing in the swap
   needs one. **Q17 (`End`) is outside `parse`** and remains schedulable on its own
   under the ordinary D69 protocol.
1. **Q14 (inert table column header) — parked, not open** — held until the fork
   sources land, because wiring a header click adds *new* public behavior the
   maintainer's own fork may already define (D78). Q6/Q10 stay kept (D75), and Q2's
   non-proportional half stays 2005 by choice (D82).
2. **Fork mapping (arrival-triggered; no expectations built on it)** — sources still
   pending (2026-08-15: not arrived, a month past the expected window). The gate
   covers **only** the Cut 4/5/6 seam commitments (D48/D50/D61/D69), never net or
   preparatory work. When they land: fork files → subsystems; boundaries vs Cut 2–6
   seams; enhancement backlog; then Cut 4+ seam commitments unblock (3a resumes).
3. **`loop-characterise` — proven, 26 allowlisted targets left.** Run it with
   `scripts/loop-characterise.sh <N>`; roughly 15 minutes per slice, unattended.
   Next on the worklist: `findText`, `getChars`, `changeCheck`. Its **repair path
   has never executed** — no slice has yet failed verification.
4. **`loop-modernise` — runs 1 and 2 done (D87/D89); the tool is proven, the net is
   the constraint.** Both runs' most valuable output was a net gap rather than a
   diff. Before a run-3, settle whether the loop is still the right instrument: the
   gaps it exposes are findable directly, sooner, and without committing code to get
   at them. The XML parser stays fenced for *modernisation* (D86 + the ROADMAP 3c
   question) — note that fence does **not** apply to tests. Its repair path has also
   never executed across six slices.
5. **Recorded, unscheduled**: `FrameLauncher` (0 % covered, and it is published
   API); the four methods D91 left alone (`getSize`, `setRectangle`, `update`,
   `findComponent`); teaching `loop-characterise` to reach event-driven code (menus,
   dialogs, focus), which is where most remaining branch coverage lives.

> **Posture: D69 governs `main` throughout again.** D97's parser freeze is lifted
> by **D98**, which settles the question it was waiting on: the hand-rolled parser
> is replaced by JAXP on 0.2.x as a clean break, and v0.1.x stays the line that
> reads the 2005 dialect. Parser behavior changes are once more ordinary D69 work —
> but the five `parse` quirks are already dispositioned to that one swap, so do not
> fix them individually.

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
