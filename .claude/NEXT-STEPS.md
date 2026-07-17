# Next steps — session handoff (2026-07-17)

> State pointers + ordered work only; rationale lives in `DECISIONS.md`
> (single-home rule + comment rules: **D57**). Charter:
> `project-docs/PHASE-3-GOALS.md`.

## State

- **Cuts 1–3 done** (D42–D59). Cut 3 landed 2026-07-14: net #81, typed core
  D58 (one recorded divergence there), façade cleanup + close D59.
- **Cut 4 net prerequisite done (D61)**: layout-state sidecar goldens pin
  `:port`/`:view`/`:widths`/`:offset` (58 sidecars / 184 nodes, bidirectional
  regression + permanent coverage guard). Residual gap: non-zero `:view.x`
  (no horizontal-scroll scenario).
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
  strict-intern tripwire live in every test JVM (D43, both modules). Base
  row: 326 (core) + 13 (drafts) tests.
- **Vocabulary decode + constants research done (D67, 2026-07-15)**:
  `project-docs/VOCABULARY-INVENTORY.md` (11 vocabularies, collision table,
  absorb-at-cut recommendations) + in-source annotations at the consumers.
- **D67 candidates pinned (D68, 2026-07-16)**: Q9 (click-dead combobox icon) +
  Q10 (ascent-sort down-arrow) locked by `InputQuirkPinsTest`; the
  `checkLocation` mousex-for-y bug proven unobservable and triaged (not
  behavior-locked), guarded by a canary. Base row: +4 tests.
- **3c opened (D69, 2026-07-17)**: `main` is the enhanced line
  (0.2.0-SNAPSHOT); v0.1.x is the frozen modernized-2005 line (`v0.1.0` tag);
  behavior changes go through the D69 protocol. Fork mapping unaffected,
  arrival-triggered.

## Next work, in order (3c open per D69 — the enhanced line is `main`/0.2.x)

1. **Quirk-fix batch (3c, active)** — per the D69 protocol. Landed:
   `checkLocation` y-arg (D70), Q1 parser null-source (D71), `FileChooser`
   guard + Q8 FolderBrowser root + the null-deref SpotBugs exclusions off
   (D72). Remaining: **Q7 dialog glyphs** (biggest; possibly split
   close/maximize/iconify; needs authorized golden re-records — start after
   the PR stack flattens).
2. **Public vocabulary (3c, after the batch)** — the D67 inventory rows
   marked 3c (choice-value enums/constants, event names); DTD-anchored so
   fork-proof.
3. **Maintainer quirk dispositions, remaining** — Q5 gate-spinning?, Q6
   jump-to-pointer?, Q9 wire-or-drop the combobox icon part?, Q10
   keep-or-flip the sort glyph?; candidates: empty-tab focus-escape,
   disabled-menuitem release-closes-silently. Behavior pinned either way.
4. **Fork mapping (arrival-triggered; no expectations built on it)** — sources
   still pending (2026-07-17: not arrived). When they land: fork files →
   subsystems; boundaries vs Cut 2–6 seams; enhancement backlog; then Cut 4+
   seam commitments unblock (3a resumes).

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
