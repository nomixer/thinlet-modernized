# Next steps — session handoff (2026-07-09)

> Refreshed at a session stop. State as of `main` = PR #67 merged. Supersedes
> `.claude/FABLE-NEXT-STEPS.md` (historical, Cut 1 era). Authority is `DECISIONS.md`
> (through **D52**); charter is `project-docs/PHASE-3-GOALS.md`.

## State at stop — Cut 2 paint-branch extraction COMPLETE

- **Merged this session:** #48–#67. **Every widget paint branch + shared helper is now
  in `Renderer.java`** (~1,900 lines): label/button/checkbox/combobox/tabbedpane/menubar/
  `:popup`/progressbar/slider/splitpane/panel(container)/dialog/spinbox, plus helpers
  `field`/`arrow`(×2)/`scroll`/`content`. `Thinlet.java` ≈ 6,470 lines (from 7,779).
  Only the **`desktop`** branch stays in `Thinlet` — it paints the timer-coupled tooltip
  (`tooltipowner` + `paintReverse`), the one net-invisible path (D45).
- **Net:** 41 static + **32 interaction goldens** + input suite (incl. the D50 `:lead`
  race pins). Every interaction-state family from the D45 survey is guarded except the
  deferred tooltip.
- **Reviews:** two Opus self-reviews done (`.claude/SELF-REVIEWS.md`). Review 1 → D50
  (plan holds, 4 guardrails). Review 2 → **D52: caught one real regression** the net was
  blind to — a `"font"`→`"t.font"` string-literal corruption from the #57 extraction
  regex (custom-font textarea rendered default). Fixed + guarded (#67). This is the proof
  the lull-time review is worth its cost.

## Next work, in order

1. **Fold the classname dispatch chain into `Renderer`** (`Thinlet.paint`, the
   `if (is(classname,…))` ladder ~Thinlet.java L1667+). Same seam recipe. Needs three
   more identical widenings — `mouseinside`, `focusowner`, `focusinside` (the dispatch
   computes `pressed`/`inside`/`focus` from them). Cannot be *fully* stateless until the
   tooltip path is handled, so keep `desktop` (and trivial `separator`/`bean`) dispatched
   from `Thinlet`, or have `Renderer.paint(t,…)` call back for `desktop`. No blocker
   (Review 2, item 4).
2. **Type the drawing vocabulary** — `paintRect`/`drawFocus`/the several `paint`
   overloads. The 22-arg `paint` (bounds+clip+4 borders+4 paddings+focus+`char mode`+
   `String align`+2 flags, Thinlet.java ~L2044) is the ergonomic wart worth a typed value
   object. All already package-private; no japicmp risk.
3. **Cut 3 — DTD → typed descriptors** is overlappable (D43) if paint work stalls.
4. **Fork mapping (task #3)** — when the maintainer's multi-file fork sources + apps
   arrive (week of 2026-07-13): fork files → subsystems; boundaries vs Cut 2–6 seams;
   enhancement backlog; static-ability map. **Lands before Cut 4/5/6 seam commitments.**
5. **Tooltip capture** — the last interaction golden, needs the 750ms timer handled
   (D45); unblocks the `desktop` branch extraction. Low priority.

## Standing discipline (hard-won, do not relearn)

- Goldens only **in the CI container** (bare-host diffs are unfaithful, D44). Record:
  `-pl thinlet-core -am test -Dtest=GoldenInteractionRecordMode -DtraceRecord=true`;
  then `git status` must show old goldens **byte-identical** (the determinism check).
  Local crossjdk rows can hit **stale incremental compilation** in the mounted `target/`
  — use `clean` when a golden must reflect a just-edited source (D52 lesson).
- **Never modify existing fixtures** — committed goldens depend on them; add new files.
- **Golden signal strength (D52):** a `font="bold"` change is within the ±2px gate; use a
  point-**size** change to force a categorical `setFont` diff.
- Press-holds on auto-repeat parts need the **D51 no-op-press** trick (scroll/spin only).
- Extraction recipe: python move with assertions. **Two traps, both bitten:** (a) arity
  alone is not identity — check argument *names* (the 11-arg border `paint` is a decoy);
  (b) blanket field-prefix regex **corrupts quoted string literals** (`"font"`→`"t.font"`,
  D52) — diff literal sequences after every mechanical move, or exclude quoted spans.
  Compiler catches missed `t.`/`Thinlet.` prefixes (no inner classes in paint code).
  Widen on demand only; comment every widening
  (`// package-private for Renderer (D48 seam; japicmp-invisible)`).
- Paint-side writes: **hoist, don't relocate** (D48); goldens cannot discriminate this
  (D50) — the `:lead` race is net-pinned; anything new needs review scrutiny.
- Verify every slice: spotless + `-DskipTests verify`, then container base + 8/11/17,
  then PR → required checks → **delegated squash-on-green auto-merge** (D46), watch in
  background. Commit trailers + PR attribution per CLAUDE.md.
- Maintainer workflow grants (2026-07-09): continue at lulls; Opus (not Fable) self-review
  at lulls with document+PR+merge rights; spell out "if and only if" (no "iff").
