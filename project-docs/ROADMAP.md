# Roadmap

The phase plan for the Thinlet modernization. **`DECISIONS.md` is the
authority** — this is a navigational summary and may lag it; when they disagree,
`DECISIONS.md` (and the repo) win. The original phase plan was an external "plan"
document referenced throughout `DECISIONS.md` (e.g. D11/D12/D14); this file makes
the roadmap a first-class committed artifact.

Status: ✅ done · ⏳ in progress · ⬜ not started

## Phase 0 — Scaffolding ✅

- Maven reactor: `thinlet-core` is the only published module; `thinlet-demos` /
  `thinlet-drafts` are reactor-only (D4, D11).
- Build on JDK 21, Java 8 bytecode via `--release 8` (D5/D14).
- Linters relaxed to a documented 2005 baseline: Spotless/palantir-java-format,
  Checkstyle, SpotBugs (D13).
- Verbatim 2005 import: source, `thinlet.dtd` (D8), XML corpus (D9/D12).
- Dev Container: fonts + fontconfig + Xvfb + AWT X11 libs, two-display model
  (D21/D22); Microsoft base image for now (D16); GHCR layer cache (D23).
- CI skeleton; `v0.0.1-bootstrap` tag (D10/D15).

## Phase 1 — Test harness + golden traces ✅ (released)

- ✅ Golden-trace harness: `TracingGraphics2D` recorder, deterministic JSON
  serializer (D7 tolerance), `LayoutTrace`, Xvfb `:99` ownership; goldens for
  41/42 corpus files; self-consistency + regression tests (D24, PR #9).
- ✅ Quirk-locking: the D13 candidate bugs triaged; the parser null-source NPE
  locked as `KNOWN-QUIRKS` Q1 (PR #10).
- ✅ JDK-8 cross-JDK execution row via `maven-toolchains-plugin`; `file.encoding`
  pinned to UTF-8 for cross-JDK determinism (D14/D25, PR #11).
- ✅ Encoding audit + inventory (D26, PR #13); `project-docs/` established (D27).
- ✅ Release machinery (D28, PR #15) and **`v0.1.0` published** to GitHub Packages
  (2026-06-19) — `thinlet-core` + the `thinlet-parent` POM.
- ✅ japicmp activated against the published `v0.1.0` baseline (D10/D29): the
  `apicheck` profile wires the binary-compatibility gate into `thinlet-core`, and
  a dedicated `api-compat` CI job runs it with GitHub Packages **read** auth (D4).
  It is profile-gated and off by default, so the default `./mvnw verify` stays
  token-free; `v0.1.1+` are now checked for accidental API breaks (PR #17).

## Phase 2 — Cross-JDK test matrix + backend-portability docs ✅

The deliverable stays D1's **single, maximally-portable Java-8 jar** (`--release
8`); what's added is proof it *runs* identically across JDK runtimes. D30's
"one jar per Java version" pivot was reverted: per-version jars are
behavior-identical until the source differentiates, so they wait for Phase 3
(**D31**, supersedes D30).

- ✅ Cross-JDK **test** matrix (runtimes JDK 8/11/17 via toolchains; JDK 21 via
  the base-JVM `build` job): the `crossjdk` profile + `.mvn/toolchains.xml` keep
  the compile at `--release 8` and fork the golden traces onto each target JDK;
  CI's single `test-jdk8` job is now a `fail-fast: false` matrix `test` job (D31).
  JDK 25 is deferred. The test libraries are pinned to JUnit 5.x / AssertJ 3.x
  (they run on the oldest test JDK), guarded by Dependabot `ignore` rules (D31).
- ✅ Cross-JDK trace diff (D33): per-JDK trace dumps (`GoldenTraceDumpModeTest`) +
  artifacts + an informational `trace-diff` CI job aggregating them via
  `CrossJdkTraceDiffTest` into a `report.md`/`report.json` divergence report
  (`project-docs/backend-portability/CROSS-JDK-TRACE-DIFF.md`). Report-only
  `TraceComparator.deltas()`; the regression gate is unchanged.
- Per-signature `trace-tolerance.json` tuning: **posture set, no entries** (D35).
  `perOp` stays empty (`{ "defaultPx": 2.0, "perOp": {} }`) until CI's cross-JDK
  diff surfaces a position that exceeds tolerance; only such a *finding* earns a
  `perOp` entry — never a `defaultPx` widening or a re-record (D7). The hook is
  reserved and report-only; JDK 8/11/17 are absent in the authoring container, so
  no entry can be authored locally.
- ✅ A `trace-curator` agent populates `project-docs/backend-portability/`
  (rendering primitives, layout algorithms, input surface) from the trace JSON
  and the cross-JDK `report.json`. First cut done (D34): `RENDERING-PRIMITIVES.md`
  and `LAYOUT-ALGORITHMS.md` curated from the committed goldens; the agent is
  codified at `.claude/agents/trace-curator.md`. `INPUT-SURFACE.md` is a
  **source-derived first cut** (D35), read from `Thinlet.java`'s event handling
  because the harness records paint + layout only; its **trace-backed** extension
  is tracked as the Phase 2.x deliverable (below).

## Phase 2.x — Input-capture harness (gate before Phase 3) ✅

The Phase 1/2 golden net is **paint + layout only** — it never dispatches input, so
~26% of `Thinlet.java` (the `processEvent`/`handleMouseEvent`/`processKeyPress`/…
event surface) has no automated coverage. A regression net only certifies a refactor
when it captures the baseline **before** the change, so for any input-touching Phase 3
work this net is **now or never**: without it those refactors stay "smoke-tested,"
never "confirmed behavior-preserving" (**D36**).

- **Probe first, then the first real build (MVP), behind an acceptance gate.** The
  feasibility probe has landed (D36): scripted AWT events driven through the real
  `protected processEvent` on headless Xvfb `:99`, targeted by `find(name)`, asserted
  **black-box** via public getters + re-paint `Trace` diffs (reusing the Phase 1
  `TracingGraphics2D`/`TraceComparator`). All green on JDK 21; cross-JDK (8/11/17)
  determinism is delegated to the `crossjdk` CI matrix. Findings + gate:
  `project-docs/backend-portability/INPUT-HARNESS-PROBE.md`.
- **Design is black-box and small.** No dispatch/routing recorder — it would re-lock
  the internals refactoring is meant to change (D36); the cross-JDK input *diff* is a
  later layer on top, not the primary goal (correcting D35's cross-JDK-first framing).
- **MVP landed (D37).** The probe graduated into a named-scenario regression net:
  `InputDriver` (+ keyboard `press`/Arrow-Home-End-Enter, mouse-wheel `scroll`, and an
  `Object[]` `property`/`viewRect` reader) drives `InputSmokeTest` + per-widget
  `InputList`/`Tree`/`ComboBox`/`Scroll` tests (`@Tag("input")`, run by default). Scenarios
  cover list selection (click/Arrow/Home/End/Shift-extend), tree select + keyboard
  expand/collapse/descend, combobox open + keyboard commit, and wheel scrolling — each
  asserted via public getters + an ephemeral same-JVM re-paint `Trace` diff (no input
  goldens). 16 tests green on JDK 21; cross-JDK (8/11/17) determinism is on the `crossjdk`
  CI matrix.
- **Gate:** met — Phase 3 input-touching refactors may proceed against this net; the
  crossjdk 8/11/17 rows have run it green on every slice since. Two findings recorded
  in D37: the Thinlet KEY_PRESSED-vs-KEY_TYPED dispatch split, and the
  `MouseWheelEvent` requirement.
- **Testkit extraction: landed as the `thinlet-core` test-jar (D65).** The standalone
  `thinlet-testkit` module D37 deferred (its `thinlet-core(test) → testkit →
  thinlet-core(main)` reactor cycle) never materializes: `thinlet-drafts`' live-app
  playthrough consumes the harness from the attached tests-classifier jar instead.
  Still deferred (per D36): graduating `INPUT-SURFACE.md` to fully trace-backed,
  scroll-offset item targeting, drag pseudo-events, and keyboard type-ahead.

## Phase 2.y — Broaden the input net + font-scaling dimension ✅ (finished with D64/D65)

The MVP (D37) is deliberately minimal; per D36 the net must cover a widget *before* a
Phase 3 refactor touches it, so 2.y broadens coverage — and adds the simplest deterministic
**scaling** probe (a larger base font) toward the end-goal of "behaves on 2026+ hardware"
(**D39**).

- **Scope:** the remaining interactive widgets — `table`, `tabbedpane`, `spinbox`,
  `slider`, menus/`popupmenu`, text editing (caret/selection), `dialog` focus, and
  `splitpane` — reusing `InputDriver`, getter-asserted, `@Tag("input")`, shippable in
  per-widget slices.
- **New driver gestures (D39):** `dragInside` (divider/scrollbar drags; a drag needs ≥2
  events because `processEvent` sends `MOUSE_EXITED` on the first), `resize` (real
  `COMPONENT_RESIZED`), and a `fontScale` `load` parameter. Harness finding: `validate()`
  defers re-layout via a negative-`bounds.width` dirty flag, so gestures reading `bounds`
  re-`paint()` between steps.
- **Font scaling:** metric-sensitive scenarios run at 1× and a larger font, asserting the
  model outcome is **scale-invariant**. This is the *metric* half of scaling; real
  device/HiDPI rendering stays Phase 3.
- ✅ **Splitpane slice landed** (`InputSplitPaneTest`): keyboard divider, drag (scale-invariant),
  auto-divider scales with font, and the resize quirk pinned as **`KNOWN-QUIRKS` Q2**
  (absolute-pixel divider: non-proportional + destructive clamp), tagged
  `documents-current-behavior` for an Enhanced-Thinlet fix.
- ✅ **Text-editing slice landed** (`InputTextEditTest`, D40): `textfield`/`passwordfield`/
  `textarea` via `processField` — typing, Backspace/Delete, caret nav, Shift-selection +
  replace, Ctrl+A, boundary clamps, password real-text, textarea newline/join. Index-based
  (font-invariant).
- ✅ **Click → caret + drag-select landed** (`InputTextEditTest`, D41): resolves the D40
  deferral. A manual real-desktop probe confirmed a click lands the caret on the nearest
  character boundary; `InputDriver.clickAt`/`size` aim the press, and the tests assert the
  FontMetrics-tolerant invariants (left/right clamps, left→right monotonicity, an interior
  landing, press-drag range selection). Not asserted: pixel-exact indices (D7).
- ✅ **Robot fidelity cross-check landed** (`InputRobotFidelityTest`, `@Tag("robot")`, D40):
  a few gestures run through a real `java.awt.Robot` on Xvfb `:99` and asserted to match the
  synthetic driver — validating the synthesized `FOCUS_GAINED` + the KEY_PRESSED/KEY_TYPED
  split; extended (D41) with a native click→caret clamp check. Base JDK-21 only (excluded
  from the cross-JDK matrix).
- ✅ **Cut-6-surface characterization landed** (D64, three slices): spinbox/slider/
  tabbedpane/scrollbar-mouse, menubar/context-menu, focus-traversal/clipboard/dialog
  drag-resize/tooltip-hide — 58 tests, quirks **Q4–Q7** locked, auto-repeat timers
  neutralized structurally (clamp-adjacent positioning).
- ✅ **Live-`Drafts` playthrough landed** (D65): the testkit consumed as the
  `thinlet-core` test-jar; `DraftsPlaythroughTest` drives the real app over the
  deterministic-page allowlist, **Q8** locked. The Drafts app is the first Phase 3b
  living test bed.

## Phase 3 — Internal refactors / Enhanced Thinlet ⏳ (3a underway)

Sub-phase 3a (modernise internals behind the net) is active — charter:
`project-docs/PHASE-3-GOALS.md`; live state: `.claude/NEXT-STEPS.md`. Cuts 1–3 are
done (D42–D59), Cut 2 fully closed (D63), the Cut 4/6 net prerequisites are in place
(D61/D64), and Cut 4/5/6 seam commitments wait on the fork mapping. Later 3c items:

- Remove SpotBugs exclusions as the code is cleaned, so the linters fail on
  regressions again (D13).
- Deliberately fix the locked quirks per their `KNOWN-QUIRKS` dispositions (e.g.
  the parser null-source NPE; the `FileChooser` fallback null deref).
- HiDPI / alternative rendering backends, informed by the backend-portability
  docs.
