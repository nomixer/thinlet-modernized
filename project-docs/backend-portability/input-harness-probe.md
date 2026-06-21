# Input-capture harness — feasibility probe findings (Phase 2.x gate)

**Status:** probe complete on the base JVM (JDK 21); cross-JDK confirmation pending CI.
**Recommendation:** **feasible — proceed to the first real build (MVP).** See the gate at the
end.

> A *feasibility probe* (XP "spike") is a short, throwaway investigation that answers one
> risky question and produces a decision, not shippable code. This note is that decision
> record. It feeds the Phase 2.x acceptance gate (ROADMAP); the MVP is built only if accepted.

## The question

Can scripted AWT input be driven through Thinlet's real `processEvent` funnel — **headless**,
on the window-manager-less Xvfb `:99` — with **deterministic, black-box-assertable** outcomes?
This is the one thing source review could not answer (D35/D36): mouse/key dispatch usually
survives headless, but native focus traversal is the classic thing that does not.

## What the probe built (test scope only)

- `thinlet-core/src/test/resources/input/probe.xml` — a tiny fixture: a `button`, a `checkbox`,
  and a `textfield`, each `name`-addressable.
- `InputProbeDriver` — synthesizes `MouseEvent`/`KeyEvent`/`FocusEvent` and pushes them through
  a `Thinlet` subclass exposing the `protected processEvent`. Targets by `find(name)`; computes
  absolute coordinates by summing the `Object[]` `"bounds"` chain (the way `LayoutTrace` reads
  it). Renders post-event state to a `Trace` by reusing the Phase 1 `TracingGraphics2D`.
- `InputProbeHandler` — a one-method handler so a click can be asserted to route to user code.
- `InputProbeTest` (`@Tag("input-probe")`) — four cases, all green on JDK 21.

> **Deviation from the plan, noted:** the probe lives in package `thinlet.trace`, not
> `thinlet.input`. The trace types it reuses (`Trace`, `TraceComparator`, `LayoutTrace`) are
> package-private; co-locating reuses them without widening Phase 1 visibility. Revisit at MVP
> if a dedicated package is wanted.

## Results (JDK 21, headless Xvfb `:99`)

| Seam | Result | How asserted |
|---|---|---|
| Mouse click → state change | ✅ | black-box getter `getBoolean(checkbox,"selected")` flips |
| Click → user action dispatch | ✅ | bound `action` reaches the handler |
| Re-paint after event differs | ✅ | post-click `Trace` ≠ pre-click `Trace` (tol 0) |
| Determinism (run to run) | ✅ | two independent runs → byte-identical post-click `Trace` |
| **Keyboard + focus** | ✅ | synthetic `FOCUS_GAINED` + `KEY_TYPED` → `getString(field,"text")` |

`./mvnw -B verify` is green end to end (0 Checkstyle, 0 SpotBugs; existing golden tests
unaffected). New code is test-scope only; `thinlet-core` stays runtime-dependency-free.

## Findings that will shape the MVP

1. **Bounds are computed during `paint()`, not on `setSize`.** The driver must run one throwaway
   paint after layout before any coordinate is read. (The Phase 1 recorder hides this by walking
   layout *after* it paints.)
2. **Press/release do not hit-test.** Thinlet caches `mouseinside` from motion events; a click
   must be primed with a `MOUSE_MOVED` to the target, or the press lands on nothing.
3. **Keyboard needs a synthetic focus gain.** Headless `requestFocus()` delivers no
   `FOCUS_GAINED`, and Thinlet drops key events unless `focusinside` is set — so the driver
   dispatches a synthetic `FocusEvent`. With that, focus + typing are deterministic. This is the
   headline positive: the seam most likely to fail headless **did not**.
4. **No public bounds getter** — coordinates are summed from the internal `"bounds"`, coupling
   the driver to that representation (acceptable in test scope, precedent in `LayoutTrace`).

## Design choices validated

- **Black-box assertion works.** Outcomes are fully observable via public getters and via
  re-paint trace diffs. The **dispatch/routing recorder (cut in D36) was not needed** — nothing
  in the probe wanted it.
- **Re-paint reuse works.** The Phase 1 `TracingGraphics2D`/`Trace`/`TraceComparator` machinery
  captures visible input effects and their determinism with no new serializer.

## Known limits of this probe (deliberate)

- **Cross-JDK is unproven locally** — only JDK 21 is in the container (`/opt/jdk8|11|17` absent).
  The same-JDK determinism is confirmed; cross-JDK (8/11/17) determinism is the remaining
  unknown and is delegated to the `crossjdk` CI matrix on push.
- Direct `processEvent` is used (deterministic); the heavier full-AWT `dispatchEvent` path is
  not exercised. Deferred regardless: list/tree/combo (scroll-offset coordinates), drag
  pseudo-events, tooltip/auto-repeat timers, keyboard type-ahead timing (wall-clock at `:4500`).

## Gate

Same-JDK feasibility is **confirmed** (deterministic test, direct observation). The design is
sound and small. **Recommendation: accept Phase 2.x feasibility and authorise the MVP** —
broaden fixtures/scenarios, keep the black-box + re-paint assertions, and lean on the `crossjdk`
matrix for the cross-version determinism signal. If CI surfaces a cross-JDK divergence
(especially in focus/keyboard), that becomes the first MVP finding to absorb.
