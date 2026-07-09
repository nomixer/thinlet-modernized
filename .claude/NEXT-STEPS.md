# Next steps — session handoff (2026-07-09)

> Written at the maintainer's request at a session stop. State as of `main` = PR #57
> merged. Supersedes `.claude/FABLE-NEXT-STEPS.md` (historical, Cut 1 era). The
> authority is `DECISIONS.md` (through **D51**); the charter is
> `project-docs/PHASE-3-GOALS.md`.

## State at stop

- **Merged this session:** #50–#57. `Renderer.java` (~1,050 lines) now holds the
  label/button/checkbox branches + the shared helpers `field`, `arrow` (both
  overloads), `scroll`, and `content` (the port-content painter). `Thinlet.java`
  ≈ 6,760 lines (from 7,779). Every slice zero-diff on JDK 8/11/17/21 in the CI
  container (`.devcontainer/ci/local-ci.sh`, D44).
- **Net:** 41 static + **26 interaction goldens** (D47 + D51 + Package A) + input suite
  (118 tests, incl. the two D50 `:lead` race pins in `InputListTest`).
- **Reviews:** first Opus self-review done (D50, `.claude/SELF-REVIEWS.md`) — plan holds;
  four guardrails active.

## Next work, in order (approved plan: `.claude/../plans/ahh-there-has-been-zany-candy.md`)

1. **Package C — tab/menubar goldens** (1 PR). New fixtures `input/tabs.xml`
   (3 tabs, `<tab name= text=>` each holding a panel) and `input/menu.xml`
   (menubar, 2 menus with menuitems). Scenarios: `tabs-tab-hover` (hover the
   **non-selected** tab header — gate is `insidepart == tab`, the tab *object*,
   Thinlet.java ~L1821; plain held hover), `menu-title-hover`, `menu-armed-open`
   (click title → popup stays open; no timer involvement — same class as
   `combobox-open-lead`). Record in-container; **old goldens must re-record
   byte-identical**; verify 4 rows.
2. **Branch slices unlocked** (separate PRs, proven recipe): combobox branch
   (goldens landed in #56), tabbedpane branch, menubar/popup branch (after
   Package C). Then the remaining small branches (panel/desktop/dialog chrome,
   progressbar, splitpane, slider — check each for unguarded focus states first;
   slider focus is knowingly unguarded, golden it before its slice).
3. **Fork mapping (task #3)** — when the maintainer's multi-file fork sources +
   apps arrive (week of 2026-07-13): fork files → subsystems; boundaries vs Cut
   2–6 seams; enhancement backlog; static-ability map. **Lands before Cut 4/5/6
   seam commitments.**
4. Cut 3 descriptor core is overlappable (D43) if slice work is blocked.

## Standing discipline (hard-won, do not relearn)

- Goldens only **in the CI container**; bare-host golden diffs are unfaithful (D44).
  Record mode: `-pl thinlet-core -am test -Dtest=GoldenInteractionRecordMode
  -DtraceRecord=true` in the container; then `git status` must show old goldens
  **unmodified** (byte-identical re-record is the determinism check).
- **Never modify existing fixtures** — committed goldens depend on them; add new files.
- Press-holds on auto-repeat parts need the **D51 no-op-press** trick (scroll/spin
  only; combobox/tab/menu presses are timer-free).
- Extraction recipe: python move with assertions; **arity alone is not identity** —
  the 11-arg border/background `paint` is a decoy for the (removed) port painter;
  check argument names. Compiler catches missed `t.`/`Thinlet.` prefixes (no inner
  classes in paint code). Widen on demand only; comment every widening
  (`// package-private for Renderer (D48 seam; japicmp-invisible)`).
- Paint-side writes: **hoist, don't relocate** (D48); goldens cannot discriminate
  this (D50) — the `:lead` race is net-pinned, anything new needs review scrutiny.
- Verify every slice: spotless + `-DskipTests verify`, then container base + 8/11/17
  rows, then PR → required checks → **delegated squash-on-green auto-merge** (D46),
  watch in background. Commit trailers + PR attribution per CLAUDE.md.
- Maintainer workflow grants (2026-07-09): continue at lulls; Opus (not Fable)
  self-review at lulls with document+PR+merge rights; spell out "if and only if".
