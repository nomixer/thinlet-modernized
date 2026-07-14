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
  this file and any external/pasted doc disagree, the repo wins. Entries are dated
  records — superseded by later entries, never rewritten — so never read one as
  current state; the live docs carry today's truth and cite the newest D-number (D66).
- **`KNOWN-QUIRKS.md`** — locked 2005 bugs/quirks (populated during Phase 1).
- **`README.md`** — project posture, build instructions, attribution.
- **`project-docs/ROADMAP.md`** — the phase plan (Phases 0–3, done vs. pending).
- **The code documents the pipeline/model in-source (D57):** `Renderer.java`'s
  javadoc carries the paint-pipeline shape; the widget-model schema + reserved
  `:`-key vocabulary sit above `createImpl` in `Thinlet.java`; the DTD/accessor
  contract is `DescriptorContractTest` (sentence-named tests are the spec).

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

## Documentation policy (D57)

- **Single home per fact:** rationale/evidence → `DECISIONS.md`; charter/invariants →
  `project-docs/PHASE-3-GOALS.md`; current state → `.claude/NEXT-STEPS.md`; behavior
  contracts → `KNOWN-QUIRKS.md` + sentence-named tests. Everything else
  cross-references — never recaps.
- **Comments state only:** facts checkable in the code beneath them, facts pinned by a
  named test (cite it), or tagged `// UNVERIFIED:` hypotheses. Fact-density, not
  narrative; grep-stable names, never line numbers or cross-file location claims.
- **New files:** license header + ≤3-line class doc + a `DECISIONS.md D<n>` pointer.
- **Tense rule (D66):** live docs speak in today's tense — history in past tense,
  present-tense claims true as-of-merge, and section labels ("Background") exempt
  nothing. `DECISIONS.md` alone keeps each entry's original tense; changes supersede,
  never rewrite.

## Current work — Phase 3a (God-class decomposition behind the net)

> **Live handoff: `.claude/NEXT-STEPS.md`** — read it first for the current state
> and the ordered next work. Authority: `DECISIONS.md`; charter:
> `project-docs/PHASE-3-GOALS.md`. The Phase 2.x section below is retained as
> background on the harness design.

## Background — Phase 2.x (input-capture harness)

Phase 2.0 is **complete** (cross-JDK test matrix + backend-portability docs): the
Phase 1 golden-trace harness (a `TracingGraphics2D` recorder + deterministic
serializer and a `LayoutTrace`, run headless over the vendored corpus
`thinlet-core/src/test/resources/corpus/{demo,drafts,amazon}/`), the cross-JDK trace
diff (D33), and the `trace-curator` curation of `project-docs/backend-portability/`
(D34 rendering + layout; `INPUT-SURFACE.md` source-derived per D35). The harness design
below remains load-bearing.

**Phase 2.x** (ROADMAP) built the **input-capture harness** as a gate before Phase 3:
the golden net was paint + layout only, leaving `Thinlet.java`'s input surface
untested, and a regression net must capture its baseline *before* any input-touching
refactor (D36). The MVP landed with D37 — `InputDriver` drives scripted AWT events
through the real `processEvent` headless on Xvfb `:99` — and the net has since grown
through the D64 Cut-6-surface characterization suite and the D65 live-`Drafts`
playthrough: `@Tag("input")` suites, run by default, asserting **black-box** via
public getters + ephemeral same-JVM re-paint trace diffs (no input goldens), green on
all JDK rows. Findings + acceptance gate in
`project-docs/backend-portability/INPUT-HARNESS-PROBE.md`. The testkit ships as the
`thinlet-core` **test-jar** (no standalone module — the reactor cycle never
materializes; D65), consumed by the `thinlet-drafts` test tree.

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
  any test deps are test-scope only and never enter the published library jar (the
  separately-attached `tests`-classifier jar is the reusable testkit, D65; its
  consumers are test-scope only).
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
- **Pre-PR Java comment pass (D60), hook-enforced:** `gh pr create` is blocked on
  branches with Java changes until the D57 comment review is attested — run
  `scripts/comment-pass.sh` (checklist + files), review, then `scripts/comment-pass.sh
  done`. Re-attest after new commits.
- **Precise language:** a non-exhaustive look is a *smoke test*, not
  "verified/confirmed". Reserve "confirmed" for a named source of truth /
  byte-hash / deterministic test / direct observation.
