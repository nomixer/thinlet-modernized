# Input surface

> **First cut — source-derived, not trace-backed.** Unlike
> `rendering-primitives.md` and `layout-algorithms.md`, this inventory is **not**
> curated from the committed golden traces: the harness records the **paint
> stream** (`TracingGraphics2D`) and the **resolved layout** (`LayoutTrace`) only,
> so there is **no input-event trace to curate from** (see `DECISIONS.md` **D34**).
> It is instead read directly from `Thinlet.java`'s AWT event handling — so its
> provenance is the **source**, and it is **not cross-JDK-validated** by the
> existing tooling. The ±2 px numeric-tolerance model (D7) is **N/A** here: input
> is described by event ids, handler routing, and resulting widget state — all
> categorical/structural, not pixel metrics. Line refs are against
> `thinlet-core/src/main/java/thinlet/Thinlet.java` and were spot-checked at
> authoring; `.claude/paint-pipeline-map.md` covers paint/layout/model only, so
> this doc is greenfield.

Three purposes:

1. Document how Thinlet consumes input today.
2. Porting surface for an alternative backend's event/input plumbing.
3. Input/focus-model design input for Enhanced Thinlet.

## How input reaches Thinlet

Thinlet is a single AWT `Component` that opts into raw event delivery rather than
attaching listeners. The constructor (`Thinlet.java:108`) calls
`enableEvents(...)` (`:124`) with the mask set
`COMPONENT | FOCUS | KEY | MOUSE | MOUSE_MOTION | WHEEL`, then overrides
`processEvent(AWTEvent)` (`:3605`) as the single dispatcher — the input analogue
of how `paint(Graphics)` is the single draw entry point. Two reflection-guarded
details matter for a 1.4+/portability backend:

- **Mouse-wheel is optional.** `WHEEL_MASK` / `MOUSE_WHEEL` are *declared* at
  `:73`/`:74` and *resolved by reflection* at `:81`/`:82`; on a pre-1.4 runtime
  they stay `0` and the wheel mask/branch are simply absent.
- **Thinlet takes over focus traversal.** The ctor reflectively calls
  `setFocusTraversalKeysEnabled(false)` (`:117`) so Tab/Shift-Tab are delivered to
  Thinlet as ordinary key events instead of being consumed by AWT's focus manager
  — Thinlet walks focus itself (see Keyboard, below).

## The event surface

`processEvent` (`:3605`) branches on `e.getID()` in this order. Every numeric
arg below (coords, click count) is read off the AWT event; none is asserted with
tolerance.

| Event id(s) | Dispatch ref | Handler | Role |
| --- | --- | --- | --- |
| `MOUSE_ENTERED` / `MOVED` / `EXITED` / `PRESSED` / `DRAGGED` / `RELEASED` | guard `:3608` | `handleMouseEvent(...)` (`:4673`) | hover/press/drag/release routed per widget `:class` |
| `DRAG_ENTERED` / `DRAG_EXITED` (synthetic) | defined `:70`–`:71`, dispatched `:3731`/`:3752` | `handleMouseEvent(...)` | popup/combolist drag in/out — **not** OS drag-and-drop |
| `MOUSE_WHEEL` (1.4+, reflective) | `:3796` | `processScroll(...)` | scroll the scrollable widget under the pointer |
| `KEY_PRESSED` / `KEY_TYPED` | `:3816` | `processKeyPress(...)` (`:3907`) | activation, caret/selection navigation, mnemonics, focus traversal |
| `FOCUS_GAINED` / `FOCUS_LOST` | `:3879` / `:3873` | inline | track `focusinside`, repaint focus owner, `closeup()` popups |
| `COMPONENT_RESIZED` / `COMPONENT_SHOWN` | `:3886` | inline | reset `content` bounds, `validate(content)`, `requestFocus()` (`:3892`) |

## Per-family behavior & backend equivalents

- **Mouse** — the guard at `:3608` catches the six real mouse ids; it reads
  `x,y`, `clickcount`, `shiftdown`, `controldown`, and a `popuptrigger` computed
  as `MOUSE_PRESSED && isMetaDown()` (`:3620`–`:3621`; the comment notes
  `isPopupTrigger` is deliberately avoided as platform-dependent). Every mouse
  path first hit-tests with `findComponent(content, x, y)` (`:3624`, defined
  `:5418`), then routes into `handleMouseEvent` (`:4673`), a `:class`-keyed switch
  (button, combobox, list, tab, slider, splitpane, scrollbar, popup …) — the mouse
  analogue of `doLayout`/`paint` dispatch. *Backend:* DOM pointer events
  (`pointerdown`/`move`/`up`) plus your own hit-test against the widget tree.
- **Synthetic drag** — `DRAG_ENTERED`/`DRAG_EXITED` (`:70`–`:71`) are Thinlet's own
  pseudo-ids (`RESERVED_ID_MAX + 1/+2`), emitted during `MOUSE_DRAGGED`
  (`:3731`/`:3752`) to model the pointer crossing into/out of a popup or combo
  list while a button is held. *Backend:* synthesized from your own pointer-capture
  tracking; there is no native event for it.
- **Mouse wheel** — the `:3796` branch (present only when `MOUSE_WHEEL != 0`)
  reflectively reads `getWheelRotation()` and calls `processScroll(...)` on the
  scrollable widget under the pointer. *Backend:* the `wheel` event.
- **Keyboard** — `KEY_PRESSED`/`KEY_TYPED` (`:3816`) dispatch to
  `processKeyPress(...)` (`:3907`, `:class`-keyed, returns whether the key was
  "consumed"). Focus traversal lives in this block: `setNextFocusable` /
  `setPreviousFocusable` (`:3840`/`:3841`), reflective
  `transferFocus` / `transferFocusBackward` (`:3845`/`:3849`), F8 splitpane focus,
  F6 window-cycle (`outgo`), and mnemonic matching (`checkMnemonic`). *Backend:*
  `keydown`/`keyup`/`input`, plus your own focus-ring walk (you cannot lean on the
  host's, exactly as Thinlet does not lean on AWT's).
- **Focus** — `FOCUS_GAINED` (`:3879`) sets `focusinside` and seeds focus if none;
  `FOCUS_LOST` (`:3873`) clears it, repaints the focus owner, and `closeup()`s any
  open popup. *Backend:* `focus`/`blur`.
- **Component** — `COMPONENT_RESIZED`/`COMPONENT_SHOWN` (`:3886`) resets the root
  `content` bounds to `getSize()`, re-`validate`s the tree, `closeup()`s popups,
  and `requestFocus()`es (`:3892`) when focus was outside; `checkLocation()`
  (`:3900`) re-hit-tests after layout shifts. *Backend:* a resize/visibility
  observer driving the same re-layout.

## Enabled-but-ignored events

Two ids are turned on by the mask but **deliberately not acted on** — worth
calling out so a backend does not invent behavior for them:

- **`KEY_RELEASED`** — enabled via `KEY_EVENT_MASK` but has **no branch**;
  `processEvent` handles only `KEY_PRESSED`/`KEY_TYPED`. (Line `:3867` is the
  F8/mnemonic `else if` inside the key block, not a released-key handler; `:3873`
  is `FOCUS_LOST`.)
- **`MOUSE_CLICKED`** — not in the mouse guard at `:3608`; Thinlet derives clicks
  from press/release + `clickcount`, never from the synthesized click id.

## AWT touchpoints a backend must replace

- **`findComponent(content, x, y)`** (`:5418`) — the hit-test walk every mouse
  path runs first; a backend supplies its own point→widget resolution.
- **Focus-traversal takeover** — `setFocusTraversalKeysEnabled(false)` (`:117`)
  plus the in-handler `setNext/PreviousFocusable` and reflective
  `transferFocus[Backward]`: the host's native Tab handling is disabled and
  replaced by Thinlet's own focus walk.
- **Synthetic drag model** — `DRAG_ENTERED`/`DRAG_EXITED` are manufactured during
  drag, not delivered by the platform; a backend must synthesize them from pointer
  capture.
- **Reflection-guarded wheel** — the wheel mask/branch exist only when the runtime
  exposes `MOUSE_WHEEL` (`:81`/`:82`); a backend wires the wheel unconditionally.

## Capturing input behavior (the input-capture harness — Phase 2.x)

This doc is **source-derived**: it records what the source is wired to do, not what
Thinlet *observably does* when driven. Closing that gap needs an executable
**input-capture harness**, which D36 resequenced into **Phase 2.x** (a gate before
Phase 3) and reframed — its primary purpose is a **same-JDK refactor-safety net** over
the untested ~26% input surface above; cross-JDK input comparison is a later layer on
top. The design, validated by a landed feasibility probe (D36,
`input-harness-probe.md`), is deliberately small:

1. **An input driver** — a test-side component that synthesizes a *scripted* sequence
   of AWT events and pushes them through Thinlet's real `protected processEvent` on the
   pinned Xvfb `:99` (D22), targeting widgets by `find(name)`.
2. **Black-box assertions, reusing Phase 1** — outcomes are read through public getters
   (`getBoolean`/`getString`/`getSelectedIndex`/`getInteger`) and **re-paint `Trace`
   diffs** via the existing `TracingGraphics2D`/`TraceComparator`. There is **no
   dispatch/routing recorder** (D36 cut it: recording internal handler routing would
   re-lock the internals a refactor is meant to change, and the probe needed none).
3. **Replayable fixtures** — committed scripted-input fixtures whose post-event state is
   asserted same-JDK and, as a later layer, diffed across JDK 8/11/17/21 via the
   `crossjdk` matrix.

Tracked as the **Phase 2.x** deliverable (ROADMAP; `DECISIONS.md` **D34**/**D35**/**D36**).

## Cross-JDK note

Because there is no input trace, **no cross-JDK input claim is made here** —
neither that behavior is identical nor that it drifts. The dispatch *structure*
above (which ids route to which handlers) is read from source and is runtime-
independent; any *behavioral* per-JDK comparison waits on the capture harness
described above. No per-JDK figures are committed (consistent with D34); the
`trace-tolerance.json` `perOp` hook is paint/layout-only and does not apply to
input.
