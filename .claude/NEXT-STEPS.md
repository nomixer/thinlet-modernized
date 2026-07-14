# Next steps — session handoff (2026-07-14)

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
- Net: 41 static + 51 interaction goldens + 58 layout-state sidecars + input
  suite + 25 contract pins (`DescriptorContractTest`); strict-intern tripwire
  live in every test JVM (D43). Base row: 268 tests.

## Next work, in order

1. **Fork mapping** — maintainer's fork sources expected ~2026-07-17/19; check
   at session start (2026-07-14: not arrived). Fork files → subsystems;
   boundaries vs Cut 2–6 seams; enhancement backlog; static-ability map.
   **Lands before any Cut 4/5/6 seam commitments.**
2. **Cut 4 (after fork mapping)** — layout → per-widget strategies; the D61
   net prerequisite is in place, seam commitments wait for the fork mapping.
3. **Maintainer quirk dispositions (from D64)** — Q5 gate-spinning?, Q6 keep
   jump-to-pointer?, Q7 wire-or-remove the dialog glyphs?; candidates:
   empty-tab focus-escape, disabled-menuitem release-closes-silently.
   Behavior is pinned either way — decisions feed Enhanced Thinlet (3c).
4. **Live-`Drafts` playthrough: DONE (D65, two PRs)** — the testkit landed as
   the `thinlet-core` test-jar + `InputDriver.attach` seam;
   `DraftsPlaythroughTest` (12 scenarios) drives the real app over the
   deterministic allowlist; Q8 locked (FolderBrowser off-Windows NPE →
   ExceptionDialog). The Drafts app is the first 3b living test bed.
5. **Optional vocabulary follow-ons** (D56 scope cut) — only if they earn their
   keep against Cut 3+.

## Discipline (one-liners; the D-entries carry the why)

- Goldens only in the CI container, `clean` before record, never re-record to
  make a diff go away (D44/D52); never modify existing fixtures — new files
  only.
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
