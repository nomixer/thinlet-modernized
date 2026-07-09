# Running the bundled examples

The repo carries the original 2005 Thinlet sample applications, preserved as-is, in
two **non-published** reactor modules:

- **`thinlet-demos/`** — the polished demos (`thinlet.demo.*`, `thinlet.amazon.*`).
- **`thinlet-drafts/`** — rougher draft samples exercising more widgets
  (`thinlet.drafts.*`).

They are ordinary AWT `Frame` apps (each `main()` opens a `FrameLauncher` window), so
they need a **display**. This doc is the quick "how do I look at them" guide; for the
build system itself see `README.md` → *Building*.

## Prerequisites: a display

- **In the Dev Container** (recommended): a noVNC desktop is served in the browser at
  forwarded port **6080** (password `vscode`), and the default `DISPLAY` is **`:1`**,
  which targets that desktop. Open port 6080 and the launched window appears there.
  (The automated trace tests use a *separate* controlled headless display `:99` — never
  `:1`; see `DECISIONS.md` D22. Don't launch demos against `:99`.)
- **On a bare Linux host**: use your real display (usually `DISPLAY=:0`).

A JRE 8/11/17/21 can run the examples; the modules build with the repo's Maven wrapper.

## Quickest path: the launcher script

`scripts/run-example.sh` builds the needed module (if not already built) and launches by
short name. Run it with no argument to list what's available:

```sh
scripts/run-example.sh                # list the examples
scripts/run-example.sh demo           # launch the widget showcase
scripts/run-example.sh --build drafts # force a rebuild first, then launch
DISPLAY=:0 scripts/run-example.sh calculator   # override the display
```

## The examples

| Short name   | Module          | Main class                      | What it is |
|--------------|-----------------|---------------------------------|------------|
| `demo`       | `thinlet-demos` | `thinlet.demo.Demo`             | The main widget showcase (buttons, lists, tree, tabs, dialogs). |
| `calculator` | `thinlet-demos` | `thinlet.demo.Calculator`       | A small self-contained calculator. |
| `amazon`     | `thinlet-demos` | `thinlet.amazon.AmazonExplorer` | Amazon product explorer — **UI only**; it targets Amazon's 2005 SOAP web service, which no longer exists, so live queries fail. |
| `drafts`     | `thinlet-drafts`| `thinlet.drafts.Drafts`         | Draft-samples showcase aggregating many components: lists, tree, tabbed pane, MDI, a chart bean, file/colour choosers, i18n, focus tests, etc. |
| `swing`      | `thinlet-drafts`| `thinlet.drafts.SwingProperties`| A Swing look-and-feel reproduction (custom fonts/colours). |

The remaining `thinlet.drafts.*` classes (`AutoFill`, `Chart`, `Choosers`, `Lists`,
`Looks`, `MDI`, `TabbedPane`, `TreeDemo`, `Widgets`, …) have no `main()` of their own —
they are the components the **`drafts`** showcase loads. `thinlet.AppletLauncher` is the
2005 applet host, not a standalone `Frame` app.

## Running manually (without the script)

Each module's compiled classes plus `thinlet-core` are all that's on the classpath (the
`*.xml` layouts live in each module's `src/main/resources` and compile into
`target/classes`). Build once, then launch:

```sh
./mvnw -q -pl thinlet-demos,thinlet-drafts -am -DskipTests package

# demos module
java -cp thinlet-core/target/classes:thinlet-demos/target/classes thinlet.demo.Demo
java -cp thinlet-core/target/classes:thinlet-demos/target/classes thinlet.demo.Calculator

# drafts module
java -cp thinlet-core/target/classes:thinlet-drafts/target/classes thinlet.drafts.Drafts
```

## Notes

- **Icons are not bundled.** The samples reference `/icon/*.gif` resources that were not
  imported with the source, so widgets render **without their icons** (blank) — this is a
  cosmetic gap in the vendored corpus, not a fault of a given run. Text and layout are
  unaffected.
- **These modules are not published.** Only `thinlet-core` publishes; `thinlet-demos` and
  `thinlet-drafts` exist for in-repo examples and the consumer-compat CI job
  (`README.md` → *Project layout*). They will become living test beds for the
  modernization once the maintainer's own apps arrive (Phase 3b).
- The original author's 2005 write-ups for some of these live under `docs/`
  (`showcase.html`, `calculator.html`, `framelauncher.html`, `appletlauncher.html`) —
  Thinlet's *own* documentation, not modernization docs.

## Related

- `README.md` — *Building* and the port-6080 noVNC desktop.
- `DECISIONS.md` **D22** (controlled displays: `:1` interactive vs `:99` test),
  **D27** (docs layout: this file is modernization/project documentation).
