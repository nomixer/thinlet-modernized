# Input-capture harness — feasibility probe findings (Phase 2.x gate)

**Status:** probe complete, **MVP landed (D37)**, and the crossjdk 8/11/17 rows have run
the suite green on every slice since; the net has since grown through D64 (the Cut-6
surface characterization) and D65 (the live-Drafts playthrough). **Recommendation
(was):** feasible — proceed to the MVP; this has since shipped. The probe findings below
are the historical spike record; the **MVP graduation** section at the end records what
the suite became (and the renamed classes).

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

## MVP graduation (D37)

The gate was accepted and the MVP shipped (test scope only; no `Thinlet.java` change). The
probe artifacts were graduated, not duplicated:

- `InputProbeDriver` → **`InputDriver`** — keeps the validated gestures (`click`/`type`/
  `focusGained`/`paint`) and adds: `press(keyCode, modifiers)` + Arrow/Home/End/Enter
  helpers (keyboard navigation), `scroll(widget, notches)` (mouse wheel), and a generalized
  `property(widget, key)` / `viewRect(widget)` `Object[]` reader.
- `InputProbeHandler` → **`InputHandler`**; `input/probe.xml` → **`input/smoke.xml`**.
- `InputProbeTest` (4 cases) → **`InputSmokeTest`**, joined by per-widget classes
  **`InputListTest`** (click, Arrow/Home/End, Shift-extend multi-select), **`InputTreeTest`**
  (click-select, Right/Left expand/collapse, Down-descend), **`InputComboBoxTest`** (click
  opens popup; Down+Enter commits), **`InputScrollTest`** (wheel-down advances `:view.y`;
  wheel-up at top is a no-op). Fixtures: `input/{list,tree,combobox,scroll}.xml`.
- **Tag:** `@Tag("input")` (selector only). The classes **run by default** in `./mvnw -B
  verify` — there is no `<excludedGroups>` — which is the point of a regression gate.
  Selectable in isolation with `-Dgroups=input`. (The probe's `@Tag("input-probe")` is
  retired with `InputProbeTest`.)

**Two findings beyond the probe (both source-confirmed and now encoded in the driver):**

5. **Keyboard dispatch is split by event type.** Thinlet processes a key only when
   `control == (id == KEY_PRESSED)` (`Thinlet.java:3827`, where `control` is true for
   control-char/action keys or Ctrl-down). So navigation/control keys (Arrows, Home/End,
   PageUp/Down, Enter, Esc) are handled **only on `KEY_PRESSED`** — `InputDriver.press`
   sends `KEY_PRESSED` with `CHAR_UNDEFINED` so `keychar >= 0xffff` forces the control
   branch and the keycode is read. Printable characters **including the space bar** (`0x20`
   is *not* a control char) are handled **only on `KEY_TYPED`** — they go through `type(…)`,
   and space is deliberately *not* a `press` helper.
6. **Wheel scroll needs a real `MouseWheelEvent`.** Thinlet detects the wheel by
   `getID() == MOUSE_WHEEL` and reads `getWheelRotation()` reflectively (`:3802`), so a
   plain `MouseEvent` won't do; `scroll` constructs a `java.awt.event.MouseWheelEvent` and
   primes it with the same `MOUSE_MOVED` `click` uses (the wheel path reads `mouseinside`'s
   `:port` with no fallback hit-test). Neither scroll offset (`:view`) nor combobox
   open-state (`:combolist`) has a public getter, so both are read off the `Object[]` model
   like `LayoutTrace` reads `"bounds"`; scroll is asserted on **direction**, never a pixel.

**Assertion model.** Public getters are the primary, JDK-invariant assertion; an ephemeral
**same-JVM** before/after `TraceComparator.compare(…, 0.0)` corroborates a visible change,
and run-to-run determinism is proven once in `InputSmokeTest`. There are **no committed
input golden files** (input state is read live), and `trace-tolerance.json` is unchanged
(its ±2px is for cross-JDK *paint* goldens, not these same-JVM input diffs).

**Still deferred** (unchanged from the probe limits): fully trace-backed
`INPUT-SURFACE.md`, scroll-offset *item* targeting, and keyboard type-ahead (wall-clock +
text-width dependent). **Resolved (D65):** the standalone `thinlet-testkit` module —
landed instead as the `thinlet-core` test-jar consumed by `thinlet-drafts` (the D37
reactor cycle never materializes). **Resolved (D62/D64):** tooltip/auto-repeat timers —
the tooltip is captured via a bounded poll, and auto-repeat is neutralized structurally
(clamp-adjacent positioning) in the D64 characterization suite. **Resolved (D41):** the D40 **mouse click → caret index** deferral —
a manual real-desktop probe confirmed a click lands the caret on the nearest character
boundary, and `processField`'s press branch self-primes its reference (`setReference` +
`:offset`=0), so `InputDriver.clickAt` only has to aim the press. `InputTextEditTest` now
covers it with FontMetrics-tolerant assertions (left/right clamps, left→right monotonicity,
an interior-landing existence check, and press-drag range selection), and the Robot
cross-check matches the native click→caret clamps.

**Robot fidelity cross-check (D40).** `InputRobotFidelityTest` (`@Tag("robot")`) runs a few
gestures through a real `java.awt.Robot` on a shown `Frame` on Xvfb `:99` and asserts the
outcome matches the synthetic driver — validating the synthesized `FOCUS_GAINED` and the
KEY_PRESSED/KEY_TYPED split against a genuine native path. Base JDK-21 only (excluded from
the cross-JDK matrix). Finding: native focus works WM-less; the one gotcha is X keyboard
auto-repeat (press+release with zero delay). Extended (D41) with a native **click → caret**
case: native type + native clicks at the two field edges, asserting the caret clamps
(`0` / length) equal the synthetic driver's — the fidelity check for `clickAt`.
