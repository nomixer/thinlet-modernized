# Next steps — session handoff (2026-07-14)

> State pointers + ordered work only; rationale lives in `DECISIONS.md`
> (single-home rule + comment rules: **D57**). Charter:
> `project-docs/PHASE-3-GOALS.md`.

## State

- **Cuts 1–2 done** (D42–D56). **Cut 3 in progress** (fork sources hadn't
  arrived, checked 2026-07-14): the characterization net
  `DescriptorContractTest` (24 contract pins) is merged (#81).
- Net: 41 static + 49 interaction goldens + input suite + the Cut 3 pins;
  strict-intern tripwire live in every test JVM (D43).

## Next work, in order

1. **Fork mapping** — maintainer's fork sources expected ~2026-07-17/19; check
   at session start. Fork files → subsystems; boundaries vs Cut 2–6 seams;
   enhancement backlog; static-ability map. **Lands before any Cut 4/5/6 seam
   commitments.**
2. **Cut 3 continues** — typed core landed (D58); remaining: accessor-façade
   cleanup + close (dead `defaultvalue` param, asymmetry-table comment, D59).
3. **Tooltip capture** — the last interaction golden; needs the 750ms timer
   handled (D45); unblocks extracting `paintDesktop`/`paintReverse`. Low
   priority.
4. **Live-`Drafts` playthrough** — needs the `thinlet-testkit` extraction +
   determinism allowlist (D37/D53). Separately scoped.
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
