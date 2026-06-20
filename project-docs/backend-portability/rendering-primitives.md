# Rendering primitives

> **First cut — curated from the committed golden traces.** Sourced from the
> `calls` arrays of every golden under
> `thinlet-core/src/test/resources/trace/{demo,drafts,amazon}/*.json` (41 files),
> cross-referenced to the `Graphics2D` call sites in
> `thinlet-core/src/main/java/thinlet/Thinlet.java`. Provenance and tolerance
> posture: a call's **method name and argument arity/types are exact** across
> JDK runtimes; **categorical** args (colors, fonts, strings) are exact; only
> **numeric** coordinate/size args are tolerant (±2 px). See `DECISIONS.md` **D7**
> and `cross-jdk-trace-diff.md`.

Three purposes:

1. Document how Thinlet renders today.
2. Porting surface for an alternative Canvas / WebGPU / WASM backend.
3. HiDPI design input for Enhanced Thinlet.

## The observed vocabulary

Thinlet drives **one shared `Graphics` object** through the recursive
`paint(Graphics, …)` tree walk (`Thinlet.java:1608`); every drawing call lands on
that object, which is how the harness captures the full stream by wrapping it
(`TracingGraphics2D`). Across the entire corpus the draw stream uses exactly
**eleven** distinct primitives — the whole backend porting surface for *static
paint*:

| Op | Args (numeric unless noted) | Corpus calls | Role |
| --- | --- | --- | --- |
| `drawImage` | `x, y, w, h` | 1777 | Blit a pre-decoded icon/glyph image |
| `drawLine` | `x1, y1, x2, y2` | 1309 | 1-px strokes; **also rectangle borders** (4 lines) |
| `setColor` | `#RRGGBBAA` (categorical) | 1121 | Set current foreground color |
| `translate` | `dx, dy` | 740 | Shift the origin (widget-relative coords) |
| `fillRect` | `x, y, w, h` | 342 | Solid fills (backgrounds, selections) |
| `drawString` | `text` (categorical), `x, y` | 311 | Text at a baseline point |
| `setClip` | `x, y, w, h` | 82 | Replace the clip rectangle |
| `clipRect` | `x, y, w, h` | 82 | Intersect the clip rectangle |
| `setFont` | `family\|style\|size` (categorical) | 65 | Set current font |
| `fillOval` | `x, y, w, h` | 5 | Filled disc (radio/bullet) |
| `drawOval` | `x, y, w, h` | 5 | Stroked circle (radio/bullet) |

> **Observed ≠ implemented.** `Thinlet.java` *also* contains `drawRect` (4 call
> sites) and a handful of other paths, but **no `drawRect` appears in any
> golden** — those code paths are not exercised by the static corpus render.
> The table above is the *observed* surface; a backend that reproduces it covers
> every corpus screen. Treat additional source-level primitives as a secondary
> tier to validate if/when a corpus file exercises them.

## State-setting ops (the backend state machine)

These mutate the device context; they emit no pixels themselves but every
subsequent primitive reads them. A porting backend needs an equivalent
context-state stack.

- **`setColor(#RRGGBBAA)`** — current foreground. Recorded categorically (e.g.
  `#909090FF`, `#E6E6E6FF`, `#FFFFFFFF`, `#000000FF` are the recurring frame /
  panel / field / text colors). Must match **exactly** across runtimes — color
  is not pixel-metric and carries no tolerance. Canvas: `fillStyle` /
  `strokeStyle`. Set at `Thinlet.java` paint sites throughout (35 source call
  sites).
- **`setFont(family|style|size)`** — current font, recorded as
  `SansSerif|0|12` (the default from the `Thinlet()` ctor, `Thinlet.java:108`).
  The *selection* is categorical-exact; its *pixel consequences* (advance widths,
  ascent) are the D7 drift source and are absorbed downstream, not asserted here.
- **`translate(dx, dy)`** — origin shift. The paint walk calls
  `g.translate(bounds.x, bounds.y)` on entry to each widget and the inverse on
  exit (`Thinlet.java:1608` region), so coordinates inside a primitive are
  **widget-relative**. A backend must keep a save/restore (or explicit inverse)
  transform stack; the trace shows the paired `translate`/inverse-`translate`
  bracketing each subtree (clearly visible in `demo/calculator.json`).
- **`setClip(x,y,w,h)` / `clipRect(x,y,w,h)`** — `setClip` **replaces** the clip;
  `clipRect` **intersects** it. They appear in equal numbers (82/82): Thinlet
  narrows the clip to a widget's content box with `clipRect`, draws, then restores
  the prior clip with `setClip`. Canvas equivalent: `save()`+`rect()`+`clip()` /
  `restore()`. The clip rectangles carry the ±2 px numeric tolerance.

## Drawing primitives (what reaches the framebuffer)

- **`drawLine(x1,y1,x2,y2)`** — Thinlet has no rounded-rect primitive and does
  **not** use `drawRect` on any corpus screen; instead it strokes box borders as
  **four `drawLine`s**. The four consecutive `drawLine`s opening
  `demo/calculator.json` (top, left, bottom, right of a 35×20 button cell) are
  the canonical example. A porting backend can collapse a recognized 4-line box
  into a single stroked rect, but must preserve the per-edge colors Thinlet uses
  for bevels (e.g. `#909090FF` frame vs. `#FFFFFFFF` highlight).
- **`fillRect(x,y,w,h)`** — solid fills: the full-canvas background
  (`fillRect 0 0 1024 768` with the panel color opens most goldens), field
  interiors, and selections. Canvas: `fillRect`.
- **`drawString(text,x,y)`** — text at a **baseline** point (note: `y` is the
  baseline, computed from `FontMetrics.getAscent()`, e.g. `Thinlet.java:2119`).
  `text` is categorical-exact; `x,y` tolerant. This is the single largest porting
  risk: the baseline `y` and any following advance depend on the backend's font
  metrics, which is exactly the variance D7's ±2 px tolerance absorbs. Canvas:
  `fillText` (with `textBaseline = "alphabetic"`).
- **`drawImage(img,x,y,w,h)`** — icon/glyph blits at an explicit destination
  rect (the corpus uses scaled 15×15 and similar icon sizes — see the three
  consecutive `drawImage … 15 15` blits in `amazon/about.json`). Images are
  pre-decoded AWT `Image`s; the trace records only geometry, not pixels. Canvas:
  `drawImage(img, x, y, w, h)`.
- **`drawOval` / `fillOval(x,y,w,h)`** — the only curved primitives, used sparsely
  (5 each) for radio-style indicators. Canvas: `arc()` + `stroke`/`fill`.

## HiDPI / device-pixel-ratio notes

All numeric args are **logical** pixels at 1× (the corpus renders at a fixed
1024×768 canvas). For a HiDPI backend:

- Multiply every coordinate/size arg (`drawLine`, `fillRect`, `clip*`,
  `translate`, image dest rects, oval rects) by the device-pixel-ratio, or apply
  a global DPR transform once and keep emitting logical coords.
- `drawString` does **not** scale linearly: glyph layout must be re-run at the
  target DPR through the backend's own metrics. This is the same coupling that
  makes `FontMetrics` the cross-JDK drift source — see below.
- `drawImage` of raster icons needs DPR-appropriate source assets to avoid
  upscaling blur; vector/glyph sources should be re-rasterized at DPR.

## Cross-JDK note

The geometry above is stable in *structure* across JDK 8/11/17/21 — same ops,
same order, same categorical values. The numeric coordinates drift slightly
between runtimes because `FontMetrics` pixel math is JDK-variable; D7's ±2 px
tolerance absorbs it, and the informational cross-JDK report
(`cross-jdk-trace-diff.md`) quantifies *where* it lands. **No fixed per-JDK
figures are committed here**: the multi-runtime report is produced only in CI
(JDK 8/11/17 are not present in the doc-authoring container), and any position
that ever exceeds tolerance is a finding to triage into a `perOp`
`trace-tolerance.json` entry (D7's reserved hook), not to bake into prose.
