# Thinlet (modernized) — Claude orientation

Modernization fork of the 2002–2005 Thinlet GUI toolkit (Robert Bajzat).
Goal: **modernize the toolchain, not the library** — preserve the 2005
observable behavior exactly while making it build and run on modern JDKs.
Public artifacts, unsupported.

> Claude's orientation/reference files are tracked in `.claude/MANIFEST.md` and
> are all safe to delete. Only this `CLAUDE.md` lives outside `.claude/`;
> everything else Claude adds for reference goes under `.claude/`.

## Read these first (source of truth = the repo)

- **`DECISIONS.md`** — append-only decision log (D1–D…). **The authority.** When
  this file and any external/pasted doc disagree, the repo wins.
- **`KNOWN-QUIRKS.md`** — locked 2005 bugs/quirks (populated during Phase 1).
- **`README.md`** — project posture, build instructions, attribution.
- **`project-docs/ROADMAP.md`** — the phase plan (Phases 0–3, done vs. pending).
- **`.claude/PAINT-PIPELINE-MAP.md`** — engineering reference for
  `Thinlet.java`'s paint/layout pipeline, drawing vocabulary, FontMetrics
  surface, and `Object[]` widget model. Background for the Phase 1 golden-trace
  harness; lives under `.claude/`, outside the product source tree.

## Documentation layout (where docs belong) — see DECISIONS.md D27

- **`docs/`** — Thinlet's *own* documentation: the verbatim 2005 website, and
  later docs reflecting enhancements the maintainer makes to Thinlet. **Never put
  project/modernization or Claude docs here.**
- **`project-docs/`** — modernization/project documentation authored for this
  fork: `ROADMAP.md`, `backend-portability/`, `ENCODING-INVENTORY.md`.
- **`.claude/`** — Claude orientation/meta only; deletable, tracked in
  `.claude/MANIFEST.md`. Only the root `CLAUDE.md` lives outside it.
- **Markdown filenames** — author project docs as `UPPERCASE-WITH-HYPHENS.md` (D38).
  Exceptions kept lowercase/fixed by ecosystem or the harness: `README.md`, `CLAUDE.md`,
  and Claude Code agent files under `.claude/agents/*.md`.

## Current work — Phase 3a (God-class decomposition behind the net)

> **Live handoff: `.claude/NEXT-STEPS.md`** — read it first for the current state
> (Cut 2 slices merged through PR #57, `Renderer.java` growing, 26 interaction
> goldens), the ordered next work, and the capture/extraction discipline.
> Authority: `DECISIONS.md` through **D51**; charter: `project-docs/PHASE-3-GOALS.md`.
> The Phase 2.x section below is retained as background on the harness design.

## Background — Phase 2.x (input-capture harness)

Phase 2.0 is **complete** (cross-JDK test matrix + backend-portability docs): the
Phase 1 golden-trace harness (a `TracingGraphics2D` recorder + deterministic
serializer and a `LayoutTrace`, run headless over the vendored corpus
`thinlet-core/src/test/resources/corpus/{demo,drafts,amazon}/`), the cross-JDK trace
diff (D33), and the `trace-curator` curation of `project-docs/backend-portability/`
(D34 rendering + layout; `INPUT-SURFACE.md` source-derived per D35). The harness design
below remains load-bearing.

**Phase 2.x** (ROADMAP) is the **input-capture harness**, a gate before Phase 3: the
golden net is paint + layout only, so the input surface of `Thinlet.java` is untested,
and a regression net must capture that baseline *before* any input-touching refactor
(D36). The **input regression MVP has landed (D37)**: `InputDriver` drives scripted AWT
events through the real `processEvent` headless on Xvfb `:99` (mouse click/wheel,
keyboard `press`/type), and `InputSmokeTest` + per-widget `InputList`/`Tree`/`ComboBox`/
`Scroll` tests (`@Tag("input")`, run by default) assert list/tree/combobox selection,
expand/collapse, and scrolling **black-box** via public getters + ephemeral same-JVM
re-paint trace diffs (no input goldens). Green on JDK 21; cross-JDK (8/11/17) is on the
`crossjdk` CI matrix. Findings + acceptance gate in
`project-docs/backend-portability/INPUT-HARNESS-PROBE.md`. Deferred (D37): extracting the
harness into a standalone `thinlet-testkit` module (reactor-cycle constraint — waits for a
second consumer).

Load-bearing design = **D7 trace-tolerance model**: method-name + arg
type/arity **structural-exact**; booleans/colors/strings/enums
**categorical-exact**; numeric coord/size args **tolerant within ±2 px**
(`trace-tolerance.json`). `FontMetrics` is JDK-variable, so its effect is
absorbed by the coordinate tolerance, not asserted directly.

## Key constraints

- **Build:** `./mvnw -B verify` (Spotless/palantir-java-format + Checkstyle +
  SpotBugs gate verify). **Build/lint JDK 21**, decoupled from library
  compatibility (the modern lint/format plugins need 17+; compatibility comes from
  `--release 8` plus the cross-JDK test matrix, not the build JVM — D31).
  **Single-jar + cross-JDK test model (D31, supersedes D30):** one portable
  Java-8 jar (`--release 8` on the JVM-21 javac), whose test suite the `crossjdk`
  profile forks onto JDK 8/11/17 via `.mvn/toolchains.xml` (JDK 21 runtime = the
  base-JVM build job). Per-version *jars* are deferred to Phase 3 when the source
  differentiates. **Build-JVM tooling may modernize freely; the *test-runtime*
  libraries are pinned** to the majors that run on the oldest test JDK — JUnit
  5.x / AssertJ 3.x (enforced by Dependabot `ignore` rules). `thinlet-core` stays
  **runtime-dependency-free** —
  any test deps are test-scope only and never enter the published jar.
- **Display (D22):** the harness runs on a controlled **Xvfb `:99`** (pinned
  fonts, no window manager) so chrome never perturbs pixel metrics; never the
  interactive `:1`. Surefire sets `DISPLAY=:99`.
- **Verbatim 2005 artifacts:** `thinlet.dtd` is byte-identical (D8); the source
  and XML corpus are preserved as imported (D9/D12). Don't reformat them.

## Working agreements

- Develop on a feature branch; **PR into `main`** (direct pushes are blocked);
  squash-merge. Every PR leaves `DECISIONS.md` / `KNOWN-QUIRKS.md` accurate
  as-of-merge — no follow-up tidy-up PR.
- **PR workflow — Phase 3+ is CI-autonomous (D42).** CI's behavior net runs only on
  PRs→`main`, and the maintainer is **not** a manual verification dependency, so **Claude
  opens PRs itself** to run the net and drives them to green (compile + Spotless / Checkstyle
  / SpotBugs run locally pre-push). **Merge to `main` stays the maintainer's** 1-click gate on
  the trunk unless delegated (opt-in "auto-merge" ⇒ GitHub squash-on-green). Supersedes the
  earlier "consult before opening a PR" norm for Phase 3. Faithful **local CI**:
  `.devcontainer/ci/local-ci.sh` runs the net inside the exact CI container image (D44) —
  never trust golden diffs from the bare host.
- **Remote branch deletion is the maintainer's to do.** The Claude-on-the-web
  sandbox can push branches but cannot delete remote refs (the git proxy returns
  403, and no API/MCP tool is exposed), so don't retry remote-branch deletion from a
  session — the maintainer prunes merged branches (GitHub Branches page), and the
  repo's "Automatically delete head branches" setting handles it on merge.
- Watch CI to green after pushing (webhooks deliver failures, not successes).
- **Precise language:** a non-exhaustive look is a *smoke test*, not
  "verified/confirmed". Reserve "confirmed" for a named source of truth /
  byte-hash / deterministic test / direct observation.
