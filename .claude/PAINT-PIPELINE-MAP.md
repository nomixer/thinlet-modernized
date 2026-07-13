# Paint-pipeline map — `Thinlet.java`

> **STALE ON LOCATIONS (kept as the 2005 map).** Written against the verbatim
> 7779-line import, before the Phase-3a decomposition: every paint branch and the
> classname dispatch now live in `Renderer.java` (D48–D55), and the icon+text
> dispatcher ("large dispatcher", pre-decomposition L3393) takes a typed
> `IconTextSpec` instead of 23 positional parameters (D56). The *semantic*
> descriptions (pipeline shape, drawing vocabulary, `Object[]` model) remain
> valid; treat every line number and "lives in `Thinlet.java`" claim as
> historical. Current locations: grep `Renderer.java` / `DECISIONS.md` D48+.

> Internal engineering reference (lives under `.claude/`, deliberately outside the
> product source tree). Maps the paint/layout pipeline, drawing vocabulary, and
> widget model of the verbatim 2005 `Thinlet.java`
> (`thinlet-core/src/main/java/thinlet/Thinlet.java`, 7779 lines as imported).
>
> Purpose: orient work on the Phase 1 golden-trace harness. This is a hand-built
> dev reference and is intentionally **separate** from
> `project-docs/backend-portability/`, whose stubs are reserved for the trace-curator
> agent to populate from actual trace JSON. Line numbers are accurate as of the
> verbatim import; re-verify if `Thinlet.java` is ever reformatted.

## Class declaration

- **Line 31:** `public class Thinlet extends Container implements Runnable, Serializable`
  — a heavyweight AWT `Container`.

## Paint entry points

- **Line 1537:** `public void update(Graphics g)` → delegates to `paint(g)`
  (no background clear).
- **Line 1551:** `public void paint(Graphics g)` — sets antialiasing hints
  (Java 1.4+), the default font, and gradient caches if needed, then calls the
  recursive paint.
- **Line 1608 (primary hook point):**
  `private void paint(Graphics g, int clipx, int clipy, int clipwidth, int clipheight, Object component, boolean enabled)`
  — the recursive tree walk. It checks visibility/bounds and returns early when
  outside the clip, calls `g.translate(bounds.x, bounds.y)`, dispatches on the
  widget classname, and recurses over children. Example child loop (line 2106):

  ```java
  for (Object comp = get(component, ":comp"); comp != null; comp = get(comp, ":next")) {
      paint(g, clipx, clipy, clipwidth, clipheight, comp, enabled);
  }
  ```

  **Harness implication:** passing a wrapping `Graphics2D` into the public
  `paint(Graphics)` captures the entire draw stream — Thinlet makes every drawing
  call on that same `Graphics`. **No edits to `Thinlet.java` are required.**

### Dispatch / decoration helpers (overloaded `paint`)
- Line 2449: `paintReverse(...)` — paints children in reverse order.
- Line 2463: `paintField(...)` — textfield/passwordfield/combobox input area.
- Line 2560: `paintScroll(...)` — scrollbars.
- Line 2858: textarea-specific `paint(Object, String classname, boolean focus, boolean enabled, Graphics g, int clipx, int clipy, int clipwidth, int clipheight, int portwidth, int viewwidth)`.
- Line 3126: `paintRect(...)` — rectangular frame with optional fill/borders.
- Line 3215 / 3256: `paintArrow(...)` — arrow buttons (spinbox/combo).
- Line 3275: border `paint(Object, int x, int y, int width, int height, Graphics g, boolean top, boolean left, boolean bottom, boolean right, char mode)`.
- Line 3393: large dispatcher — icon + text with alignment, mnemonic underline, focus indicator.

## Drawing vocabulary (what `TracingGraphics2D` must record)

Distinct `Graphics`/`Graphics2D` primitives Thinlet actually calls (call counts
approximate):

| Method | ~count |
| --- | --- |
| `drawLine(x1,y1,x2,y2)` | 37 |
| `setColor(Color)` | 35 |
| `fillRect(x,y,w,h)` | 18 |
| `setClip(x,y,w,h)` | 9 |
| `translate(x,y)` | 8 |
| `setFont(Font)` | 7 |
| `clipRect(x,y,w,h)` | 6 |
| `drawString(String,x,y)` | 5 |
| `drawRect(x,y,w,h)` | 4 |
| `drawOval(x,y,w,h)` | 4 |
| `drawImage(Image,x,y,observer)` | 4 |
| `fillOval(x,y,w,h)` | 3 |

`g.create()` may be called too — a tracing wrapper must return a child wrapper
sharing the same recording sink.

## FontMetrics surface (JDK-variable — do NOT assert; see DECISIONS.md D7)

`FontMetrics` pixel math varies across JDKs, so the harness records the
*downstream* drawing primitives and applies the ±2 px tolerance to their numeric
args rather than asserting on these calls directly:

- `getFontMetrics(Font)` — used at lines 186, 234, 650, 682, 985, 1022, 1194,
  1207, 1499, 1517, 2119, 2498, 2880, 3461, 4004, 5201.
- `fm.getHeight()`, `fm.getAscent()`, `fm.getDescent()`, `fm.getLeading()`,
  `fm.stringWidth(String)`, `fm.charsWidth(char[],off,len)`, `fm.charWidth(char)`.

## Layout

- **Line 193:** `private void doLayout(Object component)` — main dispatcher by
  classname; runs when bounds are invalid (width negated, see line 5667).
- **Line 1008:** `private void layoutField(Object component, int dw, boolean hidden, int left)`.
- **Line 1067:** `private boolean layoutScroll(Object component, int contentwidth, int contentheight, int top, int left, int bottom, int right, boolean border, int topgap)`
  — computes `:port` / `:view` / `:horizontal` / `:vertical`.
- **Line 1145:** `public Dimension getPreferredSize()` → `getPreferredSize(content)`.
- **Line 1153:** `private Dimension getPreferredSize(Object component)`.
- **Line 7482:** `private void setRectangle(Object component, String key, int x, int y, int width, int height)`
  — stores a `Rectangle` (e.g. under `"bounds"`).

## Widget model

Widgets are `Object[]` chains `[key, value, nextEntry]`; entries link via
`entry[2]`. Keys are interned string literals.

- **Line 5905:** `private static Object createImpl(String classname)` →
  `new Object[]{":class", classname, null}`.
- **Line 5932:** `private static Object get(Object component, Object key)` —
  iterates the chain by identity key.
- **Line 5909:** `private static boolean set(Object component, Object key, Object value)`.
- **Line 5892:** `public static String getClass(Object component)` →
  `":class"` value (e.g. `label`, `button`, `textfield`, `panel`, `desktop`,
  `table`, `tree`, ...).
- **Line 7494:** `private Rectangle getRectangle(Object component, String key)`.
- Tree links: `:comp` (first child), `:next` (sibling), `:parent` (line 5958).
  Children are linked-list / definition ordered → deterministic traversal.
- `Hashtable` bindings (`:bind`, line 6407) are **not** painted — ignore them.
  This is why D7's hash-iteration-ordering concern does not affect the paint
  trace.

**Harness implication:** a test in package `thinlet` can walk the tree by plain
`Object[]` traversal, matching keys with `.equals(":class"/"bounds"/":comp"/":next")`,
to emit `LayoutTrace` records without reflection or production edits.

## Parse / instantiation

- **Line 108:** `public Thinlet()` — default font (SansSerif, plain, 12), default
  colors, a `desktop` root, AWT listeners enabled.
- Parse chain:
  - **6438:** `public Object parse(String path)` →
  - **6451:** `public Object parse(String path, Object handler)` →
  - **6474:** `public Object parse(InputStream inputstream)` →
  - **6486:** `public Object parse(InputStream inputstream, Object handler)` →
  - **6600:** `private Object parse(InputStream inputstream, char mode, Object handler)`
    — a custom SAX-like parser (not JAXP). Returns the root widget.

## Headless rendering

Paint can be driven into an offscreen `BufferedImage.createGraphics()` with no
visible `Frame` and no native peer; Thinlet never forces visibility. AWT
`Toolkit`/`getFontMetrics` still initialize, which is why the harness runs under
a controlled Xvfb `:99` with pinned fonts (DECISIONS.md D22) and the AWT X11
client libs are installed in the image (D21).

Typical harness flow:

```java
Thinlet t = new Thinlet();
Object root = t.parse("corpus/demo/demo.xml");
t.add(root);
t.setSize(w, h);
t.paint(tracingGraphics2D);   // drives layout + paint; populates "bounds"
// then walk the widget tree for LayoutTrace
```

## Corpus (43 XML files under `thinlet-core/src/test/resources/corpus/`)

- **`demo/` (3):** `calculator.xml`, `demo.xml`, `demodialog.xml`.
- **`drafts/` (~30):** widget galleries and feature demos — `widgets.xml`,
  `lists.xml`, `treedemo.xml`, `tabbedpane.xml`, `choosers.xml`, `chart.xml`,
  `mdi.xml`, dialogs, progress, i18n, etc. One file,
  `drafts/internationalization.xml`, carries a legacy non-UTF-8 encoding (D12) —
  handle as bytes.
- **`amazon/` (10):** real-world UIs — `explorer.xml` (complex), `details.xml`,
  `result.xml`, `market.xml`, `about.xml`, `error.xml`, `exception.xml`,
  `exchange.xml`, `linklist.xml`, `listmania.xml`.
