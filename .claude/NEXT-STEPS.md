# Next steps — session handoff (2026-07-13)

> Refreshed after PR #79 merged. Supersedes the 2026-07-10 revision. Authority is
> `DECISIONS.md` (through **D56**); charter is `project-docs/PHASE-3-GOALS.md`.

## State at stop — Cut 2 COMPLETE (extraction + dispatch fold + vocabulary typing)

- **Cut 2 is done end-to-end.** Every widget paint branch + shared helper is in
  `Renderer.java`; the classname dispatch chain itself is folded into
  `Renderer.paint(Thinlet t, …)` behind a one-line `Thinlet.paint` shim (**D55**, #78 —
  three D48 widenings: `mouseinside`/`focusowner`/`focusinside`); and the drawing
  vocabulary's wart is typed (**D56**, #79): the icon+text dispatcher — **23 formals**,
  not D49's "22-arg"; pin that number — now takes a package-private fluent
  **`IconTextSpec`** parameter object, with the 2005 body byte-identical behind an
  unpack-prologue. Only `desktop` stays in `Thinlet` (`paintDesktop` callback;
  timer-coupled tooltip, the net-invisible path, D45).
- **D56 scope cut (recorded there):** `char mode` stays the 2005 12-char vocabulary (an
  enum would rewrite verbatim switches); the 11-arg border overload, 7-arg dialog-glyph
  `paint`, `paintRect` (25 sites), `drawFocus` (8 sites) stay untyped. Same recipe
  (spec object + unpack-prologue + scripted conversion + round-trip audit) applies if
  extended.
- **Net: 41 static + 49 interaction goldens** + input suite. New `link-button-hover`
  golden (fixture `input/link.xml`) pins the hovered-link underline — previously the one
  icon+text flag whose regression was provably zero-diff.
- **`IconTextSpec` discipline:** fresh instance at every call site, never cache/reuse
  (mutable spec; the D48 shared-state hazard). It is a data carrier, not a stateful
  subsystem — D48-compatible, package-private, japicmp-invisible.

## Next work, in order

1. **Fork mapping (task #3)** — the maintainer said (2026-07-13) he will provide his
   manual-Thinlet fork sources + apps "later this week or at the weekend" (~2026-07-17
   to 07-19). When they arrive: fork files → subsystems; boundaries vs Cut 2–6 seams;
   enhancement backlog; static-ability map. **Lands before any Cut 4/5/6 seam
   commitments.** Check for them at session start.
2. **Cut 3 — DTD → typed descriptors** (D43) — **in progress** (fork sources hadn't
   arrived, checked 2026-07-14). Net landed first: `DescriptorContractTest` (24 pins:
   lookup walk, defaults, omit-at-default asymmetries, canonicalization, exact IAE
   messages). Next: typed-descriptor core (verbatim table move → typed transform →
   convert the 15 consumers; design in the D57 entry when it lands), then façade
   cleanup.
3. **Tooltip capture** — the last interaction golden; needs the 750ms timer handled
   (D45); unblocks extracting `paintDesktop`/`paintReverse`. Low priority.
4. **Live-`Drafts` playthrough (D53 deferred)** — needs the `thinlet-testkit`
   extraction (D37) + a determinism allowlist; separately scoped.
5. **Optional vocabulary follow-ons** (D56 scope cut) — type `paintRect`/`drawFocus`/
   the 11-arg overload, or enum-ify `char mode` (rewrites verbatim switches — needs its
   own risk call). Only if it earns its keep against Cut 3+.

## Standing discipline (hard-won, do not relearn)

- Goldens only **in the CI container** (bare-host diffs are unfaithful, D44). Record:
  `-pl thinlet-core -am clean test -Dtest=GoldenInteractionRecordMode -DtraceRecord=true`
  (use `clean` — stale incremental compilation in the mounted `target/`, D52); then
  `git status` must show old goldens **byte-identical** (the determinism check: record
  twice, hash the new golden).
- **Never modify existing fixtures** — committed goldens depend on them; add new files.
- **Golden signal strength (D52):** `font="bold"` is within the ±2px gate; force a
  categorical `setFont` diff with a point-**size** change.
- Press-holds on auto-repeat parts need the **D51 no-op-press** trick (scroll/spin only).
- Mechanical moves: python with boundary assertions; **check argument *names*** (the
  11-arg border `paint` is a decoy; the icon+text dispatcher is **23 formals**, D56);
  never blanket-regex quoted spans (`"font"`→`"t.font"`, D52) — diff literal sequences.
  For call-site rewrites, add a **round-trip audit** (re-parse emitted code, reconstruct
  the original tuple, token-compare against git; D56) — it catches what the net can't.
- Before typing/moving a paint path, ask **which golden pins it**; if none does
  (tooltip, hovered-link underline before D56), land a new golden first — new files
  only, never a re-record.
- Paint-side writes: **hoist, don't relocate** (D48); widen on demand only; comment
  every widening (`// package-private for Renderer (D48 seam; japicmp-invisible)`).
- Verify every slice: spotless + `-DskipTests verify`, container base + 8/11/17, PR →
  required checks → **delegated squash-on-green auto-merge** (D46), watch in background.
  Commit trailers + PR attribution per CLAUDE.md.
- Maintainer workflow grants (2026-07-09): continue at lulls; Opus (not Fable)
  self-review at lulls with document+PR+merge rights; spell out "if and only if".
