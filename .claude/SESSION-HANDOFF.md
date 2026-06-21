# Session handoff — current status (deletable meta)

> Orientation pointer for resuming work in a fresh session. The repo's permanent
> docs (`DECISIONS.md`, `ROADMAP.md`, `input-harness-probe.md`) are the source of
> truth; this just says "where we are right now" and "what to do next." Safe to
> delete (ideally fold its removal into the MVP slice).

**As of:** 2026-06-21. **Branch:** `claude/confident-curie-rbymwo` (reset to `main`
before the MVP slice). **Last merged:** PR #28 (squash `d9b63a0`).

## Where we are

Phase 2.x's **feasibility probe is merged to `main`** and its feasibility is
**accepted**. The next step is the **MVP** (the first real build of the input-capture
harness). **Phase 3 stays blocked until the MVP lands.**

## What's done (on `main`)

- **D36 — feasibility probe** (test scope): `InputProbeDriver`/`InputProbeTest`/
  `InputProbeHandler` + `thinlet-core/src/test/resources/input/probe.xml`, under
  package `thinlet.trace`. Drives scripted AWT input through the real `processEvent`
  headless on Xvfb `:99`, asserts black-box via getters + re-paint `Trace` diffs.
- **Cross-JDK confirmed.** PR #28 CI was green on **JDK 8/11/17/21** — headless input
  determinism (incl. keyboard + synthetic focus) holds across the LTS line, not just 21.
- **D35** — Phase 2.0 close-out also landed (source-derived `input-surface.md`, matrix
  bullet ✅, `perOp` posture).

## Next: build the Phase 2.x MVP

Scope (per `input-harness-probe.md` + D36): broaden fixtures/scenarios beyond the probe
— list/tree/combo selection, scroll, more keyboard nav — keep the **black-box** stance
(public getters + re-paint `Trace` diffs; **no dispatch recorder**), run under the
`crossjdk` matrix, and **graduate `input-surface.md` from source-derived to
trace-backed**. Deferred regardless: drag pseudo-events, tooltip/auto-repeat timers,
keyboard type-ahead timing (wall-clock at `Thinlet.java:4500`).

## Building blocks + gotchas to reuse (learned by the probe)

- **Driver pattern:** `InputProbeDriver` — subclass `Thinlet` to reach `protected
  processEvent`; target by `find(name)`; sum the `Object[]` `"bounds"` chain (à la
  `LayoutTrace`) for coordinates (no public bounds getter).
- **Three load-bearing gotchas:** (1) widget bounds are computed during `paint()`, so
  paint once after `setSize` before reading coordinates; (2) `MOUSE_PRESSED`/`RELEASED`
  don't hit-test — prime each click with a `MOUSE_MOVED`; (3) headless `requestFocus()`
  delivers no `FOCUS_GAINED` — synthesize one before keyboard input.
- **Reuse:** `GoldenTraceRecorder.pumpEventQueue()`, `TracingGraphics2D`, `Trace`,
  `TraceComparator` (tol 0 for determinism), `XvfbDisplayExtension`.

## Open decision for the user (MVP session)

MVP fixture/scenario breadth and whether to keep the probe tests or fold them into the
MVP suite. Recommendation: keep the probe as the smoke layer; build the MVP alongside.
