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
- **`KNOWN_QUIRKS.md`** — locked 2005 bugs/quirks (populated during Phase 1).
- **`README.md`** — project posture, build instructions, attribution.
- **`project-docs/ROADMAP.md`** — the phase plan (Phases 0–3, done vs. pending).
- **`.claude/paint-pipeline-map.md`** — engineering reference for
  `Thinlet.java`'s paint/layout pipeline, drawing vocabulary, FontMetrics
  surface, and `Object[]` widget model. Background for the Phase 1 golden-trace
  harness; lives under `.claude/`, outside the product source tree.

## Documentation layout (where docs belong) — see DECISIONS.md D27

- **`docs/`** — Thinlet's *own* documentation: the verbatim 2005 website, and
  later docs reflecting enhancements the maintainer makes to Thinlet. **Never put
  project/modernization or Claude docs here.**
- **`project-docs/`** — modernization/project documentation authored for this
  fork: `ROADMAP.md`, `backend-portability/`, `encoding-inventory.md`.
- **`.claude/`** — Claude orientation/meta only; deletable, tracked in
  `.claude/MANIFEST.md`. Only the root `CLAUDE.md` lives outside it.

## Current work — Phase 2

Cross-JDK test matrix + backend-portability docs (ROADMAP Phase 2). The Phase 1
golden-trace harness has landed: a `TracingGraphics2D` recorder + deterministic
serializer and a `LayoutTrace`, run headless, recording golden traces over the
vendored corpus (`thinlet-core/src/test/resources/corpus/{demo,drafts,amazon}/`).
Phase 2 adds the cross-JDK trace diff (D33) and the `trace-curator` curation of
`project-docs/backend-portability/` (D34, first cut: rendering + layout;
input-surface deferred). The harness design below remains load-bearing.

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
  squash-merge. Every PR leaves `DECISIONS.md` / `KNOWN_QUIRKS.md` accurate
  as-of-merge — no follow-up tidy-up PR.
- Watch CI to green after pushing (webhooks deliver failures, not successes).
- **Precise language:** a non-exhaustive look is a *smoke test*, not
  "verified/confirmed". Reserve "confirmed" for a named source of truth /
  byte-hash / deterministic test / direct observation.
