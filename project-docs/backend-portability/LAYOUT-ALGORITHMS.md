# Layout algorithms

> **First cut — curated from the committed golden traces.** Sourced from the
> `layout` arrays of every golden under
> `thinlet-core/src/test/resources/trace/{demo,drafts,amazon}/*.json` (the
> `LayoutTrace` widget-bounds half of each trace), cross-referenced to
> `Thinlet.java`'s `doLayout` dispatch. The `{x, y, w, h}` bounds are the
> **tolerant numeric** column (±2 px); the widget `class` is categorical-exact.
> See `DECISIONS.md` **D7** and `CROSS-JDK-TRACE-DIFF.md`.

Each layout pass: its trigger, inputs, outputs (the bounds recorded per widget),
and the AWT touchpoints a porting backend must replace.

## How layout runs

`doLayout(Object component)` (`Thinlet.java:193`) is the single dispatcher,
keyed on the widget's `:class`. It runs lazily — when a component's stored width
is negated (the "invalid bounds" marker), layout recomputes and stores a
`Rectangle` under the `"bounds"` key via `setRectangle(...)`
(`Thinlet.java:7482`). The harness walks the `Object[]` tree after paint and
emits one `LayoutNode{class,x,y,w,h}` per widget that has computed bounds — so
the `layout` array is the *resolved* geometry, post-layout.

Preferred sizes feed every pass: `getPreferredSize(Object)`
(`Thinlet.java:1153`, public entry `:1145`) measures each widget, and for text it
calls `getFontMetrics(...)` (e.g. `:1194`, `:1207`, `:1499`, `:1517`) — the
**JDK-variable** input that makes bounds drift within tolerance across runtimes.

## Passes

Thinlet is a single-file immediate-mode-ish toolkit; the "layout managers" are
branches of `doLayout` rather than separate classes. The passes the corpus
exercises:

- **Grid (`panel` with column/row spec).** Inputs: child preferred sizes, the
  panel's `columns`/`gap`/`top|left|bottom|right` insets. Output: each child gets
  a cell-aligned `{x,y,w,h}`. Example (`demo/demo.json`): a content `panel` at
  `{2,21,1020,722}` lays a `label {4,4,98,21}`, `button {106,4,56,21}`,
  `label {166,4,704,21}`, two `checkbox {…,4,69,21}` across one row — uniform
  `h:21` rows, gap-separated `x` advances. AWT touchpoints: `Dimension` (from
  `getPreferredSize`), `Insets`.
- **Box / flow (menubars, button rows, toolbars).** Children packed along one
  axis at their preferred extent. Example (`demo/demo.json`): `menubar
  {0,0,1024,19}` with `menu` children at `x` = 0, 29, 61, 113 (each `h:19`,
  width = label advance + padding → the `FontMetrics`-derived part).
- **Border / split (`splitpane`, scroll frames).** A divider partitions the area;
  `layoutScroll(...)` (`Thinlet.java:1067`) computes `:port` / `:view` /
  `:horizontal` / `:vertical` sub-rectangles for scrollable content. Output bounds
  include the viewport and the (optional) scrollbar tracks.
- **Stacked / tabbed (`tabbedpane`, `dialog`, `desktop`).** The selected tab's
  content panel fills the body; siblings are positioned off-stage. Visible in
  `demo/demo.json`: `tabbedpane {0,23,1024,745}`, selected content `panel
  {2,21,1020,722}`, and an unselected `panel {-40,21,1020,722}` parked at
  negative `x`.
- **Field / text (`textfield`, `passwordfield`, `textarea`, `combobox`).**
  `layoutField(...)` (`Thinlet.java:1008`) sizes the editable area and caret/scroll
  offset from `FontMetrics` advances. Output: the content rect inside the border.

## Widget-class coverage

The 26 widget `class` values observed across the corpus, grouped by the pass that
positions them:

- **Containers / dispatch roots:** `desktop`, `dialog`, `panel`, `tabbedpane`,
  `tab`, `splitpane`, `menubar`, `menu`.
- **Grid/box leaves:** `button`, `togglebutton`, `checkbox`, `label`,
  `separator`, `progressbar`, `slider`.
- **Field / text:** `textfield`, `passwordfield`, `textarea`, `combobox`,
  `spinbox`.
- **List/collection (own internal item layout):** `list`, `table`, `tree`,
  `item`, `row`, `node`.

Each appears with concrete `{x,y,w,h}` in the goldens; the list above is the
exact observed set (verify with
`grep -ho '"class": "[a-z]*"' …/trace/*/*.json | sort -u`).

## AWT touchpoints a backend must replace

- `getPreferredSize` → child measurement; relies on `FontMetrics` for any text.
- `FontMetrics.getHeight()/getAscent()/stringWidth()/charWidth()` — text extents
  (call sites: grep `getFontMetrics` in `Thinlet.java`). **The drift source.**
- `Dimension`, `Rectangle`, `Insets` — value types for sizes/bounds/padding.
- `setRectangle`/`getRectangle` (`Thinlet.java:7482`/`:7494`) — bounds stored on
  the widget under `"bounds"`.

A non-AWT backend supplies its own text-measurement (web: `measureText` +
`actualBoundingBox*`) and rectangle math; the *algorithm* (grid/box/border/stack
branching on `:class`) is backend-agnostic.

## Cross-JDK note

Bounds are derived from `FontMetrics`-measured preferred sizes, so the same
JDK-variable text math that perturbs `drawString` coordinates perturbs widget
`{x,y,w,h}` — within D7's ±2 px tolerance. Structure (which widgets exist, their
nesting and `class`) is exact across runtimes; only the numbers drift. The
informational cross-JDK report (`CROSS-JDK-TRACE-DIFF.md`) quantifies it.
**No fixed per-JDK figures are committed here** — the multi-runtime report is
CI-only (JDK 8/11/17 absent in the authoring container), and an over-tolerance
bound is a `perOp` `trace-tolerance.json` candidate (D7), not prose.
