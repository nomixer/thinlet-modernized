# Interaction-state paint goldens — determinism design

> **Status:** built — the capture harness + first 10 goldens landed with **D47**
> (2026-07-09), validating this design empirically (green on JDK 8/11/17/21). The Cut 2
> prerequisite from D42/D43: before the paint→Renderer refactor lifts the
> hover/press/focus/selection/caret branches, those branches need goldens — the 41 static
> goldens leave them unguarded. **Source-derived** from a full survey of `Thinlet.java`'s
> interaction-state paint reads (line refs as of the post-Cut-1 file, 7,812 lines).

## Headline finding: interaction paint has NO time dependence

**The caret does not blink.** D43 assumed "caret blink is timer-phase-dependent" — that
premise is wrong, and this note corrects it (recorded in D45):

- There is no blink-phase field anywhere in `Thinlet.java` (grep `blink` = 0 hits).
- The single `timer` thread (`run()`, L3540–3567) dispatches exactly three things
  (L3551–3560): scrollbar auto-repeat (300→60 ms), spinbox auto-repeat (375→75 ms), and
  the tooltip show delay (750 ms → `showTip()`). No caret case, no repaint-on-tick.
- The caret is drawn **unconditionally whenever the widget has focus**: `paintField`
  L2512–2514 and textarea L2901–2904, gated only by
  `focus = focusinside && (focusowner == component)` (L1635–1637).

Consequently a paint frame is a **pure function** of the widget model plus seven transient
fields: `{mouseinside, insidepart, mousepressed, pressedpart, focusinside, focusowner,
tooltipowner}` (declared L47–67). Hold that tuple constant and the trace is deterministic —
no clock control, no scheduling games.

## State families and how to establish each

| Family | State | How a test establishes it | Deterministic? |
|---|---|---|---|
| Model-resident | item `selected` (L2995→3003), `:lead` row (L2956→3010), menu `selected`/armed (L2309/2377) | parse fixture / public setters / prior gestures | yes — plain model state |
| Focus | `focusinside` + `focusowner` (L1637) | existing `InputDriver` click or synthetic `FOCUS_GAINED` (D36-proven) | yes — discrete and stable |
| Hover | `mouseinside` + `insidepart` (L1636, part gate L3232) | dispatch `MOUSE_MOVED` at the target and **hold** (no further events) | yes — held transient |
| Press | `mousepressed` + `pressedpart` (L1635, part gate L3233) | dispatch `MOUSE_PRESSED` **without release** and hold | yes — held transient |
| Tooltip | `tooltipowner` (desktop paint L2114–2119) | only set by the timer thread after 750 ms | **deferred** — the one timer-coupled state |

The L1636 guard matters for the scenario matrix: hover renders only while no press is in
flight (`mousepressed == null`) or while pressing the same component — hover+press
combinations must respect it rather than enumerate blindly.

## Capture protocol

1. **Fixtures:** small per-widget XML under the existing `input/` test-resource pattern
   (reuse where a fixture already exists: `smoke.xml`, `list.xml`, `splitpane.xml`, …).
2. **Gestures:** driven through the existing `InputDriver` (real `processEvent`, Xvfb `:99`).
   One small test-scope addition: a `hoverAt(widget, x, y)` gesture — a bare `MOUSE_MOVED`
   without the click that today's `click`/`clickAt` append. Press-and-hold falls out of
   dispatching `MOUSE_PRESSED` without the release half.
3. **Trace:** after the gesture sequence, paint into the existing `TracingGraphics2D`
   (the driver's `paint()` — same recorder as the static goldens; honor the D39
   negative-width dirty-flag idiom by re-painting between gestures that read bounds).
4. **Goldens:** committed under `thinlet-core/src/test/resources/trace/interaction/` as
   `<fixture>-<scenario>.json` (e.g. `smoke-button-hover.json`,
   `textedit-field-focus-caret.json`), written only with `-Dtrace.record=true` — exactly
   the static-golden lifecycle (D24).
5. **Regression test:** a `GoldenInteractionTraceTest` mirroring
   `GoldenTraceRegressionTest`: replay fixture+gestures, compare against the golden with
   `TraceComparator` at the D7 ±2 px tolerance; runs by default and on the `crossjdk`
   matrix like every other golden.

**Aiming discipline:** all gesture coordinates derive from widget `bounds`/part geometry
(as `clickAt`/`size` already do), never from text metrics — FontMetrics variance is
absorbed by the ±2 px compare, but aim math must stay bounds-based so the *state* is
JDK-invariant (the D41 lesson).

## Known hazards (all previously recorded, collected here)

- **`:lead` paint-time write** (focused list/tree/table with no lead adopts the first item
  *during paint*) — **hoisted, not relocated, in D48** (`ensureLeadForPaint`, still invoked
  at paint time: moving it to focus-gain would flip the Down-before-repaint race). The
  goldens were recorded before the hoist and held zero-diff across it, as required.
- **Negative-`bounds.width` dirty flag** — re-`paint()` between gestures (D39).
- **`MOUSE_EXITED` on the first drag event** (D39) — only relevant if drag-in-progress
  states are ever captured (deferred).
- **Priming `MOUSE_MOVED`** before click/press gestures (D36/D37 driver findings).

## Initial scenario inventory (from the survey)

Hover/press (transient-tinted widgets only — knob/track/slider are *not* hover-tinted):
button rollover + pressed (L1694) and link-underline (L1692); checkbox hover/press fill +
pressed check-preview (L1750, 1763); combobox body (L1831) and arrow (L1802 + part gate);
spinbox up/down arrows (L2143/2158); scrollbar arrows (L2583/2673 → `paintArrow`
L3232–3241); tabbedpane tab hover (L1898/1920); menubar title hover vs armed (L2320–2342).

Focus + caret/selection: `drawFocus` sites — checkbox L1761, slider L2202, splitpane
L2265–2270, field L2535, scrollpane viewport L2837–2851, list/tree/table lead row
L3009–3016, shared dispatcher L3437; field caret + selection highlight L2500–2514;
textarea caret + selection L2893–2904; combolist open with `:lead` highlight (L2915–2946,
opened via the proven combobox click).

Model-state renders (no transient needed, still untraced today): list/tree/table rows
selected vs lead-focused; menu `selected` armed state.

## Deferred / non-goals

- **Tooltip paint** (`tooltipowner`, L2114–2119) — the only timer-coupled state; capturing
  it means racing or refactoring the 750 ms timer. Defer; revisit if Cut 2 touches
  desktop/tooltip paint early.
- Drag-in-progress visuals, popup menus beyond the combolist (menus join the net in the
  remaining Phase 2.y slices), and any `Robot`-driven capture (D40 keeps Robot as a thin
  fidelity cross-check, not a recorder).

## Posture note — this does not reverse D37

D37 deliberately rejected *input* goldens: input outcomes are asserted live via getters,
so there is no stored input baseline to drift. Interaction-state goldens are **paint**
goldens — the same artifact class as the 41 static goldens, just captured under a held
input-state tuple. The input net's getter-first posture is unchanged.
