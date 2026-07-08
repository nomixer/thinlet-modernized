# Decisions

Append-only log. Newest entries go at the bottom. Each decision is dated and
states the choice and its rationale. Do not rewrite history here; supersede an
old decision with a new entry that references it.

---

## D1 — Single `main` branch + cross-JDK CI matrix (no JDK-line branches)
**Date:** 2026-06-13

Source verification confirmed `Thinlet.java` uses no `sun.*` APIs, no
`setAccessible`, and no JDK-internal access. A Java-8-targeted JAR therefore
runs unmodified on JDK 8–25. We keep a single `main` branch and prove cross-JDK
behavior with a CI matrix (JDK 8 / 11 / 17 / 21 / 25), not with per-JDK
branches.

## D2 — Divergence playbook (escape hatch, expected unused)
**Date:** 2026-06-13

If a future JDK removes/changes something Thinlet needs *and* the fix cannot be
expressed in Java-8-compatible source, cut branch `java-N` from `main`, record
the trigger here, disable linear-history protection on that branch so it can
receive forward-merges from `main`, and suffix its artifacts `-javaN`. The main
matrix gains a `java-N` column for that JDK row. Expected to remain unused.

## D3 — Attribution discipline
**Date:** 2026-06-13

README opens with credit to Robert Bajzat and links to the original SourceForge
site. The LGPL 2.1 `LICENSE` and the copyright headers in the source files are
preserved verbatim; **no fresh nomixer copyright is claimed** on top of the
original. `AUTHORS` lists Bajzat as original author and the maintainer
separately as "modernization-fork maintainer."

Note: the 2005 archive shipped no plain-text license file — only the LGPL
header notice in the sources plus an *abbreviated* `docs/lgpl.html` (TERMS
0–16, missing the Preamble and the "How to Apply" appendix). `LICENSE` is the
**complete canonical LGPL 2.1** text the source header invokes; the archive's
abbreviated `docs/lgpl.html` is preserved verbatim as the historical artifact.

## D4 — GitHub Packages publication + authentication note
**Date:** 2026-06-13

Publication target is **GitHub Packages only** (not Maven Central).
**Only `thinlet-core` publishes**; `thinlet-demos` and `thinlet-drafts` are
reactor modules (built, tested, CI-gated) with
`<maven.deploy.skip>true</maven.deploy.skip>`. GitHub Packages requires
authentication **even for public reads**, so consumers (including the
consumer-compat CI job and the future Thing project) need a token. The README
documents this.

## D5 — `--release 8` deprecation hedge
**Date:** 2026-06-13

Build JVM is a modern LTS (JDK 21); javac targets Java 8 bytecode via
`--release 8`; surefire runs tests on a real JDK 8 via `maven-toolchains-plugin`.
`--release 8` is deprecated and will eventually be dropped from a future javac.
When that happens, pin an older build JDK or switch to a toolchains-driven
javac 8. Tracked as an open item.

## D6 — `AppletLauncher` lives in `thinlet-demos`, not `thinlet-core`
**Date:** 2026-06-13

Once `v0.1.0` ships, japicmp locks `thinlet-core`'s public surface.
`AppletLauncher extends java.applet.Applet`; if it were in core, the eventual
removal of `java.applet.Applet` (JDK 26+) would break `thinlet-core`'s compile
with no exit but the divergence playbook — for a launcher almost nobody runs.
Moving it to `thinlet-demos` pre-`v0.1.0` keeps core's public surface
applet-free. `FrameLauncher` (extends `Frame`, durable indefinitely) is the
only launcher in `thinlet-core`.

## D7 — Trace tolerance model (the central cross-JDK guarantee)
**Date:** 2026-06-13

Pinned fonts fix the glyph source but **not** the JDK's pixel-metric math
(`FontMetrics.getAscent()`, `stringWidth()`, etc. can return different integers
across JDKs). The cross-JDK guarantee is therefore "behavior identical *within
a defined metric tolerance*," not "byte-identical." The trace diff is:

- **Structural-exact:** method-name and arg-type/arity sequence compared
  exactly — any new/missing/reordered call is a real regression.
- **Categorical-exact:** booleans, color components, strings, enums compared
  exactly.
- **Value-tolerant:** numeric coordinate/size args compared within a configured
  pixel tolerance (**default ±2 px**), per call signature in
  `trace-tolerance.json`, reviewed when added.
- **Hash-iteration ordering normalized** at the serializer for groups
  originating at known `Hashtable`-iteration call sites.
- Side metadata (call-sites, stack traces, timestamps) is a sidecar CI
  artifact, excluded from the diff. The same ±2 px discipline applies to
  `getPreferredSize` layout assertions in surefire.

## D8 — `thinlet.dtd` kept byte-verbatim
**Date:** 2026-06-13

`thinlet-core/src/main/resources/thinlet.dtd` is byte-identical to the 2005
archive (sha256 `fd1bc3ae4f422e3608adf18d1074775917f3fd5483f752375b6720bc63bf8bac`).
It is explicitly excluded from the Spotless XML target and from `*.dtd`
whitespace/EOL rules in `.editorconfig`, and marked `binary` in
`.gitattributes`. The "verbatim" claim is honored mechanically, not by promise.

## D9 — Test corpus vendored into `thinlet-core`
**Date:** 2026-06-13

The demo + draft XML corpus used by `thinlet-core`'s parser and trace tests is
vendored into `thinlet-core/src/test/resources/corpus/{demo,drafts,amazon}/`
rather than basedir-referenced from the sibling demos module. Keeps
`thinlet-core` standalone-buildable; the consumer-compat job needs no sibling
directory.

## D10 — `v0.0.1-bootstrap` is a git tag only
**Date:** 2026-06-13

`v0.0.1-bootstrap` is a plain annotated git tag marking the end of Phase 0
scaffolding. It is **never** published to Maven and **never** a japicmp
baseline. japicmp is configured but inactive during Phase 0 and activates from
`v0.1.0` onward (Phase 1), comparing against `v0.1.0`.

## D11 — `thinlet-drafts` depends on `thinlet-demos`, not just `thinlet-core`
**Date:** 2026-06-13

The plan's module diagram had both `thinlet-demos` and `thinlet-drafts`
depending only on `thinlet-core`. The actual 2005 code disagrees:
`thinlet-drafts`/`Choosers.java` imports `thinlet.common.*` and uses
`thinlet.common.FileChooser`, which lives in `thinlet-demos`. Rather than edit
the 2005 source to satisfy the diagram, `thinlet-drafts` declares a dependency
on `thinlet-demos` (and gets `thinlet-core` transitively). Neither demos nor
drafts is published, so this changes no published artifact.

## D12 — Spotless XML scope excludes the vendored 2005 corpus
**Date:** 2026-06-13

The plan called for Spotless to format `**/*.xml`. In practice the 2005 XML
under `src/main/resources/` (demos, drafts) and the vendored corpus under
`thinlet-core/src/test/resources/` are **excluded** from the Spotless XML
target, for two reasons: (1) they are behavior-relevant test/demo inputs we
preserve as vendored 2005 artifacts (parser + golden-trace fidelity), and
(2) some carry legacy non-UTF-8 encodings (e.g.
`thinlet-drafts/.../internationalization.xml`) that Spotless cannot process as
UTF-8. Spotless XML hygiene therefore applies only to project-authored XML
(POMs, `config/`). `thinlet.dtd` remains excluded and byte-verbatim (D8).

## D13 — Linters relaxed to a documented legacy baseline (Phase 0)
**Date:** 2026-06-13

`mvn verify` runs Spotless + Checkstyle + SpotBugs and must pass on the
**unmodified** 2005 source (plan Phase 0 step 6: config/suppression changes
only, zero production-code edits). Concretely:

- **Checkstyle** (Google-derived) drops `NeedBraces`, `EmptyBlock`,
  `MissingSwitchDefault`, and `FileTabCharacter` (the last fires on tab-indented
  code inside a *commented-out* method that palantir correctly leaves alone),
  and raises `LineLength` to 120 to match palantir's column limit.
- **SpotBugs** accepts a baseline of ~20 idiomatic 2005 patterns across
  `thinlet.*` (boxing via constructors, interned-string `==`, broad
  `catch (Exception)`, default-less switches, dead stores, demo GC/stream
  idioms, etc.), enumerated in `config/spotbugs/exclude.xml`.
- A few SpotBugs findings are **candidate genuine bugs** (null-param deref and
  unclosed-stream paths in the XML parser, `FileChooser` null path). They are
  accepted for the Phase-0 baseline and earmarked for `KNOWN-QUIRKS.md` entries
  locked by tests in Phase 1 — not fixed in Phase 0.

As internal refactors (Phase 3) and Enhanced Thinlet address these, exclude
entries are removed so the linters fail on regressions again.

Status (2026-06-15): Phase 1 triage done (see KNOWN-QUIRKS.md). The parser
null-source NPE is locked as Q1 with tests. The parser "unclosed-stream"
findings (`OBL_*`, `OS_OPEN_STREAM`) were judged non-reproducible — the parser's
`Reader` is closed in a `finally` on every practical path — so they are tracked,
not behavior-locked. The `FileChooser` null deref lives in a demos fallback path
(private inner class, used only when Swing's `View2` fails to load) and is
documented rather than test-locked. All remain SpotBugs suppressions for
Enhanced Thinlet.

## D14 — Phase 0 CI runs Maven on JDK 21; "jdk 8" is a target, not a runtime
**Date:** 2026-06-13

The plan's build arrangement (Maven on a modern LTS, Java 8 *target*) and its
cross-JDK matrix are in tension for the JDK-8 row: the modern toolchain
(palantir-java-format needs JDK 17+, Checkstyle 10 and SpotBugs need JDK 11+)
cannot run under JDK 8. Resolution:

- The Phase-0 CI job runs `./mvnw -B verify` inside the JDK-21 Dev Container.
  Java 8 *bytecode* is guaranteed by `--release 8`, exercised in this job.
- The per-JDK *execution* matrix (run the test suite/trace on JDK 8/11/17/21/25
  via `maven-toolchains-plugin` + surefire) is inert until tests exist; it lands
  with Phase 1 and expands one row at a time in Phase 2. The JDK-8 *toolchain*
  drives surefire while Maven itself stays on JDK 21.
- Maven local repo: CI points `maven.repo.local` at `.m2/repository` inside the
  bind-mounted workspace and caches that path, so the Dev Container writes into
  the cached directory (addresses the Dev Containers Maven-cache note).

Status (2026-06-14): resolved — the workflow has since run in real GitHub
Actions runners. The first runs surfaced three env-specific fixes (see D17);
`./mvnw -B verify` is now green on `main`.

## D15 — `v0.0.1-bootstrap` is tagged on `main` after merge, not from the bootstrap branch
**Date:** 2026-06-14

Supersedes the *placement/timing* of the tag in D10 (D10's semantics —
git-tag-only, never published, never a japicmp baseline — still hold). Two
constraints forced this:

- **The bootstrap session's git proxy rejects tag pushes (HTTP 403).** It
  permits pushing only the designated feature branch ref
  (`claude/amazing-cannon-3vpwfz`); any tag ref push returns a hard 403, not a
  transient error. So the tag *cannot* be published from the session that did
  the scaffolding. A local annotated tag was created there but is ephemeral
  (the container is reclaimed) and should be treated as a no-op.
- **A bootstrap milestone belongs on mainline history.** If the branch is
  squash-merged, a tag on the branch HEAD would point at a commit not reachable
  from `main`. Tagging the *merge commit on `main`* keeps `v0.0.1-bootstrap`
  reachable from `main`.

Action when this branch merges: create `v0.0.1-bootstrap` as an annotated tag
on the resulting `main` commit — via the GitHub UI (Releases → new tag on the
merge commit) or `git tag -a v0.0.1-bootstrap <merge-sha> && git push origin
v0.0.1-bootstrap` from a clone with push rights. Nothing in Phase 0 depends on
it (japicmp activates at `v0.1.0`, D10).

Status (2026-06-14): done — the annotated tag `v0.0.1-bootstrap` exists on the
remote at `4d5fe17` (reachable from `main`).

## D16 — Stay on the Microsoft dev-container base image for now (defer a self-controlled image)
**Date:** 2026-06-14

The dev image is `FROM mcr.microsoft.com/devcontainers/java:1-${JDK}-bookworm`.
The first real CI run hit a break inherited from that base (an unsigned yarn
apt source, see D17). We considered moving to a base we fully control
(`eclipse-temurin:<exact>-jdk-<one-OS>` per JDK + our font/Xvfb layer + a
non-root user), which would (a) carry no inherited third-party apt sources and
(b) let us pin **exact** JDK builds — relevant to the cross-JDK font-metric
determinism guarantee (D7), since `1-${JDK}-bookworm` floats patch versions.

Decision: **stay on the MS base for now.** The upfront cost (re-create the
non-root user/sudo/tooling MS provides, install each matrix JDK ourselves,
validate font rendering across all five rows) is a bounded one-time effort, but
not worth spending before it buys something. **Revisit triggers:** (1) D7 font
work needs exact JDK/freetype pinning the MS floating tags can't give, or
(2) inherited-base breakage recurs. When revisited, pin the base by digest and
let Dependabot (already configured) propose bumps.

Input (2026-06-15, from wiring the JDK-8 row, D25) — to weigh when this is
revisited, not a decision now:

- **The cross-JDK *toolchains* model needs several JDKs in one image.** The MS
  base ships a single JDK, so JDK 8 was layered in by hand (`/opt/jdk8`). It
  worked cleanly, but the MS base's "one JDK per image" convenience is largely
  moot for us — we hand-install the extra JDK(s) regardless, and the full Phase 2
  matrix (8/11/17/21/25) means installing several.
- **Determinism cuts toward a self-owned base.** We now float *two* JDK sources
  (the MS `1-21` tag floats patch versions; the Temurin 8 install uses Adoptium
  "latest 8 GA", also floating). A self-controlled base pinned by digest per JDK
  would give the exact-build pinning D7/D16 care about across all rows.
- **Locale.** The JDK-8 default-charset gotcha (US-ASCII when `LANG` is unset)
  was handled per-test via `-Dfile.encoding=UTF-8` (D25); a self-owned image
  could pin `LANG`/locale at the image level instead.
- **Build-time external dep.** Installing JDK 8 via a network download on every
  image rebuild adds an Adoptium dependency; a baked multi-JDK base would be more
  reproducible.

Net: the more JDK rows we add, the less the MS single-JDK base buys and the more
a self-owned multi-JDK base buys (exact pinning) — but the current approach works,
so no urgency.

## D17 — First real CI run hardened three env-specific failures
**Date:** 2026-06-14

`main` and the `v0.0.1-bootstrap` tag triggered the workflow's first execution
in a real runner (the gap flagged in D14). Three failures surfaced that local
`mvn verify` never exercised, because it does not build the dev-container image.
All three are toolchain/config fixes — **zero production-source changes** — and
landed squashed in one commit on `main`:

- **Dev Container build:** the base image ships an unsigned yarn apt source
  (`dl.yarnpkg.com`); `apt-get update` aborts (exit 100). Drop any yarn source
  (matched by content) before updating. We do not use yarn.
- **Maven wrapper home:** the wrapper writes its distribution under
  `${MAVEN_USER_HOME:-$HOME/.m2}/wrapper`, and `~/.m2` is the root-owned
  `thinlet-m2` named volume → permission denied. Point `MAVEN_USER_HOME` at the
  writable workspace `.m2` (where `-Dmaven.repo.local` already writes).
- **Spotless scope:** with the wrapper now under the workspace `.m2`, the XML
  target scanned Maven's own bundled `toolchains.xml`. Exclude `.m2/**`.

Result: `./mvnw -B verify` is green in CI on `main`.

## D18 — Doc pages normalized to LF + UTF-8; Spotless gates both
**Date:** 2026-06-14

Two cleanups to the 2005 `docs/` website, each gated so it cannot regress:

- **Line endings → LF.** Five pages
  (`docs/{calculator,events,i18n,overview,showcase}.html`) carried mixed CRLF
  *and* stray lone-CR bytes, so a fresh clone warned "CRLF will be replaced by
  LF". Normalized to pure LF (byte-confirmed: only end-of-line bytes changed),
  matching the ~40 docs already stored as LF. `.gitattributes`
  (`* text=auto eol=lf`) auto-normalizes CRLF on commit, but these predated it
  and git's CRLF→LF filter does not strip lone CRs, so a one-time pass was
  needed.
- **Encoding → UTF-8.** `docs/index.html` (windows-1252 `™`) and
  `docs/showcase.html` (windows-1252 accented names) were the only non-ASCII
  docs; transcoded cp1252 → UTF-8 (lossless round-trip verified; no
  `<meta charset>` existed to update). All `docs/` files are now UTF-8/ASCII.

Gate: a Spotless `<format>` enforces LF + a final newline on
`docs/**/*.{html,css}` and `**/*.md`, and — because Spotless reads UTF-8 — also
guards the docs' UTF-8 encoding (a non-UTF-8 byte fails `spotless:check`, which
is exactly how the first attempt here caught index.html while it was still
windows-1252). Scope is line-ending / newline / encoding only — no whitespace
trimming or markup restructuring. An encoding-agnostic byte-grep gate was
considered and dropped in favor of converting the docs to UTF-8 so standard
tooling can lint them.

## D19 — Dev container: writable ~/.m2, `mvn`→wrapper shim, working pre-commit
**Date:** 2026-06-14

CI was already handled by `MAVEN_USER_HOME` (D17); these fix the *interactive*
VS Code dev-container experience:

- **Writable Maven cache.** The `thinlet-m2` named volume mounts at
  `/home/vscode/.m2` root-owned, so the `vscode` user could not create
  `~/.m2/repository` — both `mvn` and `./mvnw` failed locally with
  `LocalRepositoryNotAccessibleException`. `postCreateCommand` now `sudo chown`s
  the mount to `vscode`; it runs on every create/rebuild, so it also repairs an
  already-root-owned volume (no manual `docker volume rm` needed).
- **`mvn` on PATH = the wrapper.** A `/usr/local/bin/mvn` shim execs the
  project's `./mvnw` (wrapper resolved via `git rev-parse --show-toplevel`, so it
  works from any subdirectory). Interactive `mvn` is therefore byte-for-byte the
  pinned Maven version CI runs — no separate SDKMAN Maven that could drift.
- **pre-commit actually works.** It was `pipx`-installed as root (not on the
  `vscode` PATH) and had no config, so it never ran. Now installed to
  `/usr/local/bin` (system-wide), with a `.pre-commit-config.yaml` whose single
  `local` hook runs `./mvnw -q -B spotless:apply` — the same formatter/config as
  the CI Spotless gate, so local commits and CI agree. Skippable per-commit with
  `git commit --no-verify`.

These touch only the dev-container tooling; no production source or CI build
behavior changes (CI still uses `./mvnw` directly with the workspace `.m2`).

## D20 — Dev Container is for clones, not linked git worktrees
**Date:** 2026-06-14

A linked `git worktree`'s `.git` is a pointer file into the *main* repository's
`.git/worktrees/<name>` — a host path outside the folder the Dev Container
mounts. Inside a worktree-based container, git is therefore non-functional
(`fatal: not a git repository`), which breaks Source Control, commits, and
`pre-commit` (the latter is what surfaced it). Making a worktree work would
require bind-mounting the main repo's `.git` at its exact host path —
host-specific and non-portable, so it is **not** added to the shared
`devcontainer.json`.

Resolution: open the Dev Container on a normal clone (documented in `README.md`,
"Building"). Plus graceful degradation so a worktree open doesn't throw a
traceback during create: `postCreateCommand` runs `pre-commit install` only when
git works (`git rev-parse --git-dir >/dev/null 2>&1 && pre-commit install ||
true`). The `mvn` shim (D19) already degrades gracefully — its failed
`git rev-parse` falls back to `$PWD/mvnw`.

## D21 — Dev image includes AWT's X11 client libraries
**Date:** 2026-06-14

Running any AWT program in the dev container (a demo, or the Phase 1 headless
trace tests) failed with `UnsatisfiedLinkError: libXtst.so.6: cannot open
shared object file`. The JDK's `libawt_xawt.so` dynamically links several X11
client libraries at `Toolkit` init, and the base image shipped Xvfb + fonts but
not those libs. The image now also installs `libxtst6 libxi6 libxrender1
libxext6 libx11-6 libxrandr2`. This is on Phase 1's critical path (AWT must
initialize for the trace tests), independent of the demos.

Library vs. display — distinct layers: this fixes only the missing *library*.
AWT still needs a running X server to open a window. Headless run/tests use
Xvfb on `:99` (`DISPLAY` is set in `devcontainer.json`); the Phase 1 harness
owns starting Xvfb. *Seeing* a demo window needs a real display — run it on the
host, or add an in-container noVNC desktop (e.g. the `desktop-lite` feature),
which is deferred and not required for the trace-based verification (D7).

## D22 — In-container noVNC desktop for visual development (display model)
**Date:** 2026-06-14

A GUI toolkit needs a code → run → *see* loop inside the dev container, not just
build & test — so the deferral noted in D21 is taken up early (by request). The
`desktop-lite` dev-container feature adds a lightweight Fluxbox desktop served
over noVNC (browser, forwarded port 6080; default password `vscode`).

Two-display model, deliberately separate so eyeballing never affects the
golden-trace metrics (D7):

- **`:1` — viewable desktop (desktop-lite/noVNC).** The interactive default
  `DISPLAY` (`devcontainer.json` `containerEnv`); demos launched from the editor
  or terminal appear in the browser desktop.
- **`:99` — controlled headless Xvfb.** Owned/started by the Phase 1 trace
  harness, set explicitly for those runs (fixed resolution, pinned fonts, no
  window manager) so WM chrome never perturbs pixel metrics. The harness sets
  `DISPLAY=:99` for surefire regardless of the interactive default.

Cost/scope: desktop-lite measured at **~1 GB** added (2.49 → 3.51 GB). The CI
build overrides the container entrypoint, so the desktop never *starts* in CI,
but CI rebuilds the dev image from scratch every run (no persistent layer
cache), so that ~1 GB would be installed on every run for zero CI benefit.

Resolved by **splitting the config**:
- `.devcontainer/devcontainer.json` — full dev image (desktop-lite, ports, `:1`);
  VS Code auto-uses it.
- `.devcontainer/ci/devcontainer.json` — lean image (same Dockerfile, no desktop
  feature / ports / mounts); the CI workflow points `devcontainers/ci` at it via
  `configFile`. (It lives in a `ci/` subfolder because the devcontainer CLI
  requires the file be named `devcontainer.json`; `dockerfile: ../Dockerfile` +
  `context: ..` reach back up so the `COPY fonts/...` resolves.) CI image stays
  ~2.5 GB and builds as fast as before.

Both share the one `Dockerfile` (JDK, fonts, Xvfb, AWT X11 libs, mvn shim,
pre-commit), so the build/test environment can't drift between them; only the
desktop layer differs.

The dev image's `postCreateCommand` runs `.devcontainer/dev-postcreate.sh`
(chown `~/.m2`, `pre-commit install` when git is usable, and install a
one-time-per-terminal hint into `/etc/bash.bashrc` printing the noVNC port/URL).
The lean CI config has no `postCreateCommand`, so none of that runs in CI.

## D23 — CI caches the lean dev-container image layers in GHCR
**Date:** 2026-06-14

CI rebuilds the lean (D22) dev-container image from scratch every run. On a
public repo that costs only time, not money (Actions minutes are free and the
image is never stored), but it slows feedback and would draw down the free quota
if the repo ever went private. Caching layers across the ephemeral runners needs
a store; the free, GitHub-native one is GHCR.

The workflow logs in to GHCR and passes `imageName` + `cacheFrom`
(`ghcr.io/nomixer/thinlet-modernized/devcontainer-ci`) to `devcontainers/ci`,
with `push: filter` — push the updated cache only on `main`, not on PRs. Most
runs then rebuild only changed layers; when the Dockerfile is untouched, image
setup is a fast pull. Requires `permissions: packages: write`. (First effect is
deferred: the cache image only exists after the first `main` run pushes it.)

Rejected alternatives: the registry-free `type=gha` buildx cache would mean
dropping `devcontainers/ci` and re-implementing the uid/workspace-mount handling
it does for us (the source of the D17/D19 fixes). Base-image digest pinning is
deferred — the Dockerfile takes `JDK_VERSION` as a build arg for the future
cross-JDK matrix, so one digest can't pin all rows; revisit with the base-image
decision (D16).

## D24 — Golden-trace harness, slice 1 (recorder + serializer + first goldens)
**Date:** 2026-06-15

First Phase 1 slice: a golden-trace harness in `thinlet-core` (test scope),
proving the pipeline end to end before scaling across the corpus and JDK matrix.

- **Hook with zero `Thinlet.java` edits.** `TracingGraphics2D extends Graphics2D`
  delegates every call and records the drawing vocabulary; it is passed into
  Thinlet's public `paint(Graphics)`, capturing the whole draw stream. A fresh
  `BufferedImage` graphics has a null clip and Thinlet's paint dereferences clip
  bounds, so the driver sets the clip on the raw graphics before wrapping (not
  recorded).
- **Trace shape = D7.** Each call is `op` + categorical args (colors `#RRGGBBAA`,
  fonts, strings, shape names — compared exactly) + numeric args (compared within
  `trace-tolerance.json`, default ±2 px). `getFontMetrics` is delegated but not
  recorded; its JDK variance is absorbed by the coordinate tolerance. `LayoutTrace`
  walks the `Object[]` widget tree (`"bounds"`/`:comp`/`:next`) in definition
  order. Serialization is a hand-rolled deterministic JSON writer+reader — no JSON
  dependency, so `thinlet-core` stays runtime-dependency-free; JUnit 5 + AssertJ
  are test scope only.
- **Display (D22).** `XvfbDisplayExtension` owns Xvfb `:99`, launched **detached**
  (`sh -c "Xvfb … &"`) — a direct child Xvfb process breaks surefire's fork
  lifecycle ("error occurred in starting fork" even on passing tests); detaching
  avoids it, and the server is reused by later forks. Surefire sets `DISPLAY=:99`;
  not `java.awt.headless`.
- **Corpus coupling and coverage.** The vendored corpus XML is handler-coupled:
  `finishParse` resolves event-handler/`init` method references (e.g.
  `showDialog`, `resultSelected`, `closeDialog`) against the handler by reflection
  and throws when absent; those methods live in `thinlet-demos`, not core. The
  harness parses with `CorpusHandler`, a **no-op stub** exposing every method
  signature the corpus binds (init hooks therefore run as no-ops — the trace is a
  deterministic *static* render, not the demo's live data). This brings coverage
  to **41 of 42** files. The one exclusion is `drafts/chart.xml`, which embeds a
  `thinlet.drafts.ChartBean` *class* (not a handler method) and so can't be
  stubbed; it is skipped and reported.
- **Determinism fix.** `setSize` posts an async `COMPONENT_RESIZED` event whose
  handler computes the content bounds; a direct `paint()` raced the EDT and
  intermittently produced an empty render. The driver flushes the AWT event queue
  (`EventQueue.invokeAndWait`) after `setSize`, so layout is always applied before
  painting. `setColor(null)` is recorded as the categorical `"null"` (Thinlet
  resets the color this way; a fresh `Color` would NPE).
- **Tests** (both `@ExtendWith(XvfbDisplayExtension.class)`): a self-consistency
  test (render twice → tolerant diff empty, through a JSON round trip) and a
  golden regression test (each committed golden re-rendered, matched within
  tolerance). Same-JDK for now; the per-JDK execution matrix (D14) is a later
  slice. Goldens are (re)written only with `-Dtrace.record=true`.

## D25 — JDK-8 execution row lands via toolchains, with a pinned test charset
**Date:** 2026-06-15

The first cross-JDK row of the execution matrix (D14): Maven still runs on JDK 21
(the lint/format plugins need 11+), but surefire forks the test suite — including
the golden traces — on **JDK 8**.

- **Toolchains, not a per-JDK container.** The dev image installs a second JDK
  (Temurin 8) at `/opt/jdk8` alongside the base JDK 21; `.mvn/jdk8-toolchains.xml`
  points at it; the `jdk8-tests` profile + `-t` make `maven-toolchains-plugin`
  select it so surefire forks tests on JDK 8. CI gains a separate `test-jdk8`
  job (the existing JDK-21 `build` job — and its check name — is unchanged).
- **Charset pin (the load-bearing fix).** Thinlet's parser reads XML with a
  platform-default `InputStreamReader`. JDK 18+ defaults to UTF-8 (JEP 400);
  JDK 8 uses a locale-dependent default — **US-ASCII when `LANG` is unset**, as in
  the CI container. Without a pin, non-ASCII corpus text (e.g.
  `drafts/internationalization.xml`, one label in `drafts/widgets.xml`) decoded
  differently on JDK 8, diverging the goldens far beyond the ±2 px tolerance
  (categorical string mismatches and ~15 px layout cascades — not metric jitter).
  surefire now sets `-Dfile.encoding=UTF-8` (a no-op on JDK 21), an environment
  pin in the same spirit as pinned fonts/Xvfb. This is *not* a `Thinlet.java`
  change — the 2005 platform-default behavior is preserved; the harness just fixes
  the environment so traces are comparable.
- **Result.** With the charset pinned, all 41 goldens + self-consistency + quirk
  tests pass on JDK 8, validating the D7 cross-JDK tolerance guarantee for the
  first time. Exact JDK-8 version pinning (vs the floating Adoptium "latest 8 GA"
  download) stays the open item from D16.

## D26 — Text-encoding inventory + policy (UTF-8 authored; legacy artifacts annotated)
**Date:** 2026-06-15

Two charset hiccups (Spotless on a non-UTF-8 file, D12; the JDK-8 default-charset
trace divergence, D25) shared a root cause: no declared, discoverable record of
which file uses which encoding. A one-time audit (`file --mime-encoding` over all
tracked files) found every file is US-ASCII or valid UTF-8 **except the two
byte-identical copies of the i18n demo**, which are **ISO-8859-2** (declared in
their XML prolog):
`thinlet-core/src/test/resources/corpus/drafts/internationalization.xml` and
`thinlet-drafts/src/main/resources/thinlet/drafts/internationalization.xml`.

Policy:

- **Authored files are UTF-8** (Spotless already enforces this for Java, project
  XML, and `docs/**` + `**/*.md`; `.gitattributes` normalizes EOL).
- **Vendored 2005 artifacts stay byte-verbatim** (D8/D9). The non-UTF-8 ones are
  behavior-relevant — Thinlet's parser reads XML with the platform-default
  charset, so the raw bytes drive rendering and the goldens — and are **not**
  transcoded. They are now annotated `-text` in `.gitattributes` (no EOL/encoding
  normalization) and catalogued in `project-docs/ENCODING-INVENTORY.md`, which also
  documents how to re-run the scan and how to determine any file's codeset.

Deliberately *not* doing a bulk UTF-8 conversion: transcoding the ISO-8859-2 i18n
files would make the parser render them "correctly" and silently change the
locked 2005 behavior (the `-Dfile.encoding=UTF-8` pin from D25 makes the
legacy-bytes-as-UTF-8 reading deterministic across JDKs — that *is* the behavior
under test). A standing CI guard (fail on a new non-UTF-8, non-allowlisted file)
is noted as a possible follow-up in the inventory doc.

## D27 — Documentation directory layout (`docs/` vs `project-docs/` vs `.claude/`)
**Date:** 2026-06-16

Three documentation homes, kept strictly separate so each has one clear purpose:

- **`docs/` — Thinlet's *own* documentation.** The verbatim 2005 website
  (preserved) and, later, docs reflecting enhancements the maintainer makes to
  Thinlet itself. **No project/modernization or Claude docs go here** — this
  directory is the toolkit's documentation, period.
- **`project-docs/` — modernization/project documentation** authored for this
  fork: `ROADMAP.md` (the phase plan, previously only an external/uncommitted
  doc), `backend-portability/` (porting reference, populated by the trace-curator
  agent — moved here from `docs/`), and `ENCODING-INVENTORY.md` (D26, moved here
  from `docs/`). Durable; not Claude-meta.
- **`.claude/` — Claude orientation/meta only.** Deletable, tracked in
  `.claude/MANIFEST.md`; only the root `CLAUDE.md` lives outside it.

This supersedes the earlier placement of `backend-portability/` and
`ENCODING-INVENTORY.md` under `docs/`. References updated (`.gitattributes`,
`.claude/PAINT-PIPELINE-MAP.md`, D26). The rule is also recorded in `CLAUDE.md`
so future sessions keep `docs/` for Thinlet's own documentation.

## D28 — Release/publish mechanism: tag-driven deploy to GitHub Packages
**Date:** 2026-06-16

How `v0.1.0` (the first published artifact, D4/D10) and later releases publish:

- **Tag-driven.** A `Release` workflow (`.github/workflows/release.yml`) triggers
  on a `v*` tag, derives the release version from the tag (`vX.Y.Z` → `X.Y.Z` via
  `versions:set`), and runs `mvn deploy`. `main` stays on `-SNAPSHOT`; the release
  version exists only in the tagged build — no release-commit churn on `main`.
- **Auth.** `actions/setup-java` writes the `settings.xml` for server id
  `github-nomixer` (matching `distributionManagement`) from the workflow's
  `GITHUB_TOKEN` (`permissions: packages: write`). The Maven wrapper reads that
  `settings.xml` by default.
- **Scope.** The deploy publishes **`thinlet-core` and the parent POM**
  (`thinlet-parent`) — the parent must be published for consumers to resolve
  core. `thinlet-demos`/`thinlet-drafts` keep `maven.deploy.skip=true` (D4). A
  future refinement could use `flatten-maven-plugin` to inline the parent and
  publish core alone.
- **Tests skipped, gates kept.** The tagged commit was already verified green on
  `main`, so the release job runs `-DskipTests` (also avoids needing the JDK-8
  toolchain / Xvfb on a plain runner); Spotless/Checkstyle/SpotBugs still run on
  JDK 21.
- **The tag is a maintainer action.** This session's git proxy cannot push tags
  (D15), so a maintainer pushes `v0.1.0`.
- **japicmp timing.** Stays skipped through `v0.1.0` (no prior baseline); it is
  activated afterwards so `v0.1.1+` compare against the published `v0.1.0` (D10).

Validated locally with a dry-run `deploy` to a `file://` staging repo: only
`thinlet-parent` + `thinlet-core` artifacts are produced; demos/drafts skip
deployment.

Status (2026-06-19): `v0.1.0` published — a maintainer pushed the `v0.1.0` tag,
the Release workflow ran, and `com.nomixer.thinlet:thinlet-core:0.1.0` (with the
`thinlet-parent` POM) is live on GitHub Packages. japicmp activation against this
baseline (D10) is the remaining follow-up; it needs CI-only GitHub Packages read
auth (D4) and should be profile-gated so default `verify` stays token-free.
Done in D29.

## D29 — japicmp activation: profile-gated, CI-only read auth, baseline `v0.1.0`
**Date:** 2026-06-19

Closes the D28 follow-up: the binary-compatibility gate (D10) is now live against
the published `v0.1.0` baseline, so `v0.1.1+` builds fail on accidental public-API
breaks in `thinlet-core`.

- **Profile-gated (`apicheck`), off by default.** The japicmp execution and the
  GitHub Packages `<repository>` that resolves the baseline live only in the
  `apicheck` profile in `thinlet-core/pom.xml`; the profile flips the parent's
  `japicmp.skip` (default `true`) to `false`. The plain `./mvnw verify` therefore
  never reaches GitHub Packages and **needs no token** — the load-bearing
  constraint from D4 (reads require auth) and D28 (keep default builds token-free).
- **Baseline.** `oldVersion` is pinned to `com.nomixer.thinlet:thinlet-core:0.1.0`
  (jar); `newVersion` is the freshly built artifact. The
  `breakBuildOnBinaryIncompatibleModifications` / `onlyModified` gate is inherited
  from the parent `pluginManagement` config — the profile adds only the execution,
  the baseline, and the repository.
- **CI auth path.** A dedicated `api-compat` job (`.github/workflows/ci.yml`) runs
  on a plain runner (japicmp is a pure JDK-21 bytecode diff — no Xvfb/fonts/JDK-8)
  with `permissions: packages: read`. `actions/setup-java` writes the
  `settings.xml` for server id `github-nomixer` (matching the profile's
  repository) from `GITHUB_TOKEN`, mirroring the Release workflow (D28). It runs
  `-Papicheck -DskipTests -pl thinlet-core -am verify`: `-DskipTests` skips the
  Xvfb-dependent surefire suite (covered by the `build`/`test-jdk8` jobs), while
  `package` (via `verify`) still builds the jar japicmp diffs.
- **Reactor version-collision trap (load-bearing).** japicmp's `oldVersion` is a
  normal Maven dependency: if the project's own version ever *equals* the baseline
  coordinate (`0.1.0`), Maven resolves `oldVersion` to the **reactor artifact**
  (the jar just built) instead of the published baseline, so the gate silently
  compares the build against itself, reports "No changes", and **passes no matter
  what** — a false green. The gate is meaningful only because `main` always
  carries a `-SNAPSHOT` version (D28), which never equals the `0.1.0` release
  coordinate, so the CI `api-compat` job (building `0.1.0-SNAPSHOT`) gets a real
  comparison. Implication: do **not** run `-Papicheck` against a build whose
  version has been `versions:set` to the baseline release; and when the baseline
  is later advanced (e.g. to `0.1.1`), keep it strictly below the current
  `-SNAPSHOT` line.
- **Validation.** Verified locally (JDK 21) that the gate actually *breaks* the
  build: installed the current build as the `0.1.0` baseline, reduced
  `Thinlet.find(String)` to `protected` on a throwaway `0.1.0-SNAPSHOT` build, and
  the `api-compat` execution failed with
  `thinlet.Thinlet.find(java.lang.String):METHOD_LESS_ACCESSIBLE` (source then
  restored byte-identical). A first attempt that set the project to `0.1.0`
  produced a false green — that is how the collision trap above was found. Also
  confirmed japicmp resolves and analyzes the AWT-heavy `thinlet-core` jar with no
  missing-class errors, and that the default `verify` still passes and makes no
  GitHub Packages request. The GitHub Packages **read** auth itself is exercised
  only in CI (no token locally, by design).

## D30 — Per-version artifacts: build+test the matrix now, publish Java 8 only
**Date:** 2026-06-20

**Supersedes D1.** D1 framed the deliverable as a *single* maximally-portable
Java-8 jar proven to run unchanged on JDK 8–25 via a cross-JDK matrix. The
release axis is now **one jar per Java version** (8 / 11 / 17 / 21 / 25) — each a
real build for that version, eventually compiled on / differentiated for its own
JDK. This promotes D2's `-javaN` artifact from an unused escape hatch to the
normal release axis and dissolves the "`--release 8` single target" framing of
D5/D14 (the single Java-8 jar is now just the first row of the matrix, not the
whole story).

Because the library source is still fully Java-8-compatible, every per-version
jar is **behavior-identical today** — only the bytecode level differs — until
Phase 3 ("Enhanced Thinlet") differentiates them. So the chosen sequencing is
**build the capability now, publish later**:

- **Stand up the per-version build+test matrix now.** Each of JDK 8/11/17/21/25
  compiles and passes the golden traces. A `crossjdk` profile (generalizing the
  old `jdk8-tests`, D25) + a consolidated `.mvn/toolchains.xml` listing all five
  `/opt/jdkN` (installed in `.devcontainer/Dockerfile`) make
  `maven-toolchains-plugin` select the row's JDK so surefire forks the traces on
  it. CI's single `test-jdk8` job becomes a `fail-fast: false` matrix `test` job.
- **Keep publishing only the Java 8 jar** (`release.yml` / D28 untouched). Five
  functionally identical jars would be redundant; start publishing 11/17/21 (then
  25) once Phase 3 differentiates them.

**Compile model = A (compile on the JVM-21 javac, run on the target JDK).** Each
row compiles with `--release N` on the base JDK-21 `javac` (genuine version-N
bytecode + API surface via `ct.sym`), then surefire forks the tests onto JDK N.
The build never invokes the target JDK's own `javac`. This is exactly what the
green `jdk8-tests` row already did — it compiled `--release 8`, which is only
possible because compilation stayed on the JVM-21 `javac` (`javac 8` rejects
`--release`). Model A keeps one clean parameterized profile with no JDK-8
`source/target`-vs-`release` special case. (The stronger "run each JDK's own
`javac`" — Model B — is deferred; it becomes relevant when Phase 3 source
diverges per version. The JDK-8 row remains the canary: if a future
`maven-compiler-plugin` started honoring the jdk toolchain for *compilation*, the
8 row's `--release 8` would break on `javac 8` and that row would go red.)

**JDK-25 caveat (load-bearing).** `javac` can only target releases **≤ its own
version**, so the JDK-21 build `javac` cannot emit `--release 25`. Under Model A
the 25 row compiles at **`--release 21`** (the build JVM's max) and runs the
golden traces on the **JDK 25 runtime**. That validates the real question for the
row — does the 2005 behavior hold on the newest JDK's runtime within the D7 ±2px
tolerance — but the "25 jar" is Java-21 bytecode, **not** genuine class-file-69
Java-25 bytecode. Genuine `--release 25` requires a JDK-25 `javac` (bumping the
build JVM, or compiling that row on JDK 25 — a Model-B exception), deferred to
Phase 3 when per-version jars actually differentiate and publish. Acceptable now
because we publish only the Java 8 jar and all per-version jars are
behavior-identical today.

**Build/lint JVM stays JDK 21 — and why.** The build/lint JVM is deliberately
**decoupled** from library compatibility: compatibility now comes from the
per-version build+test matrix, not from a single `--release 8` target. JDK 21 is
kept for toolchain maturity — palantir-java-format (needs 17+), Checkstyle 10,
SpotBugs, japicmp, and the MS `:1-21-bookworm` base image — none of which run on
JDK 8 (cross-ref D5/D14). Using *later*-than-8 language features in the source is
a Phase 3 concern, not this slice.

**Determinism / open items.** The image now installs **five** floating Adoptium
`latest/N/ga` JDKs (plus the floating MS `:1-21` base) — more floating sources,
which strengthens the D16 case for a self-owned, digest-pinned multi-JDK base;
not fixed here. The 11/17/21/25 rows are first-time golden runs against the
single baseline (D7 — no per-JDK goldens): a row exceeding ±2px gets a documented
`perOp` tolerance entry (implementing the reserved `TraceComparator` hook), not a
re-record or a widened `defaultPx`.

## D31 — Revert to one portable Java-8 jar + a cross-JDK *test* matrix; pin the test libs
**Date:** 2026-06-20

**Supersedes D30; restores D1's single-artifact framing.** D30 made the release
axis "one jar **per Java version**" (8/11/17/21/25). On reflection that was the
wrong call *for this phase*, and this decision reverts it. The deliverable is
again a **single, maximally-portable Java-8 jar** (compile `--release 8` on the
JDK-21 javac), validated to behave identically — within the D7 ±2px tolerance —
across JDK **runtimes**. The valuable axis is runtime coverage, not bytecode
level.

**Why per-version jars were redundant now.** From one Java-8-compatible source,
`--release 8/11/17/21` produce **behavior-identical** artifacts that differ only
in the class-file version header (plus invisible codegen such as `invokedynamic`
string-concat). A higher-`--release` jar is strictly **less** portable — it
refuses to load on older JVMs — for **zero** behavioral or performance gain on
any given JVM. So building five jars from one source yields one useful jar plus
four that are identical-but-less-portable. Per-version *jars* only earn their
keep once the **source** differs per version, i.e. Phase 3 ("Enhanced Thinlet").
Until then the portable Java-8 jar already runs everywhere the others would, and
runs the same.

**What we keep from D30.** The cross-JDK *test* machinery built for D30 stays and
is simply re-pointed from "build N jars" to "run the one jar's tests on N
runtimes": the `crossjdk` profile + `.mvn/toolchains.xml` still make
maven-toolchains-plugin fork surefire (golden traces included) onto each target
JDK, while Maven stays on the base JDK 21. The compile is no longer
parameterized — every row compiles the same `--release 8` output; only the
*test* JVM varies. So D30's Model-A discussion and its JDK-25 `--release` caveat
are moot here (we don't emit per-version bytecode), and they return only with
Phase 3.

- **Test runtimes: 8, 11, 17 via toolchains; 21 via the `build` job.** CI's
  `test` matrix forks runtimes 8/11/17; the `build` job (Maven on JDK 21) is
  itself the JDK 21 runtime row, so it isn't repeated. Together they cover
  8/11/17/21. **JDK 25 is deferred** — kept off the validated set for now (it was
  always a runtime-only row under D30 anyway). `.devcontainer/Dockerfile` now
  installs three extra JDKs (8/11/17), not five.
- **Publishing is unchanged.** Still the single Java-8 jar (`release.yml` / D28
  untouched, japicmp / D29 untouched). D30 already published only Java 8, so this
  reversion changes nothing downstream.

**Build/lint JVM stays JDK 21 (unchanged from D5/D14/D30).** The build JVM is
decoupled from library compatibility: portability comes from `--release 8` plus
the cross-JDK *test* matrix, and the modern tooling (palantir-java-format,
Checkstyle, SpotBugs, japicmp, the MS `:1-21` base) needs 17+. The key
distinction this decision leans on: **build-JVM tooling may modernize freely;
only the *test-runtime* libraries are constrained**, because they execute on the
oldest test JDK.

**Pin the test libraries to the Java-8/11-compatible majors.** JUnit 6 and
AssertJ 4 both require Java **17+** at runtime, so they cannot run on the JDK
8/11 test rows. JUnit is therefore pinned to the **5.x** line and AssertJ to
**3.x** (minor/patch bumps within those majors are fine). This is enforced
mechanically by Dependabot `ignore` rules (`.github/dependabot.yml`) on
`version-update:semver-major` for `org.junit:junit-bom`, `org.junit.jupiter:*`,
and `org.assertj:*`. The pin lifts on its own terms only when the cross-JDK test
floor rises above 11 (a future decision), not as a silent dependency bump.

**Relationship to PR #20 (the Dependabot group bump that surfaced this).** PR #20
bundled ten plugin bumps, nine of them safe build-JVM tooling, plus the one
load-bearing problem: `junit-bom 5.11.4 → 6.1.0`, which would have broken the JDK
8/11 test rows. (Its `assertj-core 3.27.3 → 3.27.7` was a safe in-major minor.)
The ignore rules above prevent the JUnit-6 proposal from recurring; the safe
plugin bumps are taken separately so each gets its own CI pass — notably
Checkstyle 10→13 and Spotless 2→3, which run on the build JVM but can trip new
rules / reformatting against the verbatim 2005 source (D13). PR #20 is closed as
superseded; Dependabot reproposes the safe set against the new ignore rules.

**Net effect.** Simpler tooling (three extra JDKs, not five; no `--release`
parameterization), fully modern build tooling, the same cross-JDK runtime
fidelity D30 bought, and a published artifact that is maximally portable rather
than one of five behavior-identical jars. Per-version *artifacts* return in Phase
3 when the source actually differentiates — at which point D30's Model-A/Model-B
and `--release 25` analysis is the right starting point. (Cross-ref
D1/D2/D5/D7/D14/D25/D28/D29/D30.)

## D32 — Build-plugin bumps (deferred safe set from #20); SpotBugs 4.10 AT_ baseline
**Date:** 2026-06-20

Applies the safe plugin bumps that were carved out of the closed Dependabot
group PR #20. Bumped, all via `pom.xml` `<properties>`: Checkstyle tool
10.21.0→**13.6.0**, Spotless plugin 2.43.0→**3.7.0**, SpotBugs plugin
4.8.6.6→**4.10.2.0**, japicmp 0.23.1→**0.26.1**, maven-compiler 3.13.0→**3.15.0**,
maven-jar 3.4.1→**3.5.0**, maven-deploy 3.1.3→**3.1.4**, maven-surefire
3.5.2→**3.5.6**, AssertJ 3.27.3→**3.27.7** and JUnit 5.11.4→**5.14.4** — both
in-major minors that stay within the D31 pin (JUnit on 5.x, which still runs on
the JDK-8/11 test floor; the pin only blocks the 6.x major). This PR folds in and
**supersedes Dependabot's regenerated PR #23**: #23 proposed the same versions
(incl. the JUnit 5.14.4 minor) but, being version-only, lacked the SpotBugs
baseline edit below and so went red on `build` + `api-compat`. **Held:**
`version.palantir.format`
2.50.0 (kept fixed so the Java formatting output — and thus the 2005 source's
on-disk form — does not move), `maven-checkstyle-plugin` 3.6.0, and
`maven-toolchains-plugin` 3.2.0 (not in #20, and 3.6.0 drives the 13.6.0 tool
fine).

**The two majors land clean; only SpotBugs needed a baseline edit.** Verified on
the JDK-21 build JVM (`./mvnw -B -DskipTests verify`): Checkstyle **13.6.0** runs
the existing `config/checkstyle/checkstyle.xml` ruleset with 0 violations (no
module renames bit us), and Spotless **3.7.0** with palantir 2.50.0 passes
`spotless:check` unchanged — so neither major touched the source. SpotBugs
**4.10.2.0** ships a new "Atomicity" (`AT_`) detector family absent from 4.8,
which flagged 12 findings in `Thinlet.java` — all unsynchronized access to shared
primitive fields (`mousex`/`mousey`, `referencex`/`referencey`, `focusinside`,
`block`) shared between the EDT and the blink/scroll timer thread.

**Disposition: accept in the legacy baseline, do not fix (D13).** These are the
same 2005 single-threaded-by-convention threading idiom already accepted via
`IS2_INCONSISTENT_SYNC` / `NN_NAKED_NOTIFY` / `LI_LAZY_INIT_STATIC`; the
modernization rule is config/suppression changes only, zero production-code edits
(D13). So `config/spotbugs/exclude.xml` gains `AT_STALE_THREAD_WRITE_OF_PRIMITIVE`
and `AT_NONATOMIC_OPERATIONS_ON_SHARED_VARIABLE` under the existing `thinlet.*`
`<Match>`. Like the rest of that baseline, these exclusions are removed (and the
underlying concurrency reviewed for real) when Enhanced Thinlet revisits the
threading model in Phase 3. **Net source-diff: none** — only `pom.xml` and the
SpotBugs filter changed. japicmp 0.26.1 is profile-gated (`-Papicheck`), so its
behavior is validated by CI's `api-compat` job, not the local build. (Cross-ref
D13/D29/D31.)

## D33 — Cross-JDK trace diff: persist per-JDK traces, report (don't gate) the drift
**Date:** 2026-06-20

The D31 test matrix proves each JDK runtime renders within ±2px of the single
committed baseline golden, but only as pass/fail — the actual per-JDK render is
computed in memory and discarded, so we have no view of *where* / *how much* the
runtimes drift (the `FontMetrics` sub-pixel variance D7 absorbs). This decision
adds the **cross-JDK trace diff**: persist each runtime's trace, then aggregate
into a divergence report. It is the Phase-2 roadmap item and the data source the
later `trace-curator` / backend-portability docs will curate. Engineering
reference: `project-docs/backend-portability/CROSS-JDK-TRACE-DIFF.md`.

**The regression gate is left untouched (the "are we discarding data?" answer).**
`TraceComparator.compare()` emits only *over-tolerance* numeric mismatches — the
correct contract for the regression gate, which we do not change. The gap is not
that the gate drops data but that the per-JDK `Trace` is never *persisted*
(nothing is irretrievable — renders are deterministic, per the self-consistency
test). So the slice is purely additive: (a) a dump mode persists each runtime's
full trace, and (b) a new report-only `TraceComparator.deltas()` enumerates
*every* numeric difference (incl. sub-tolerance) plus any structural/categorical
mismatch, used only by the report. The gate's behavior and output are byte-identical.

**Informational, not a second gate (chosen).** The report never fails CI. The
per-JDK golden tests already enforce ±2px-vs-baseline on every runtime; a separate
cross-JDK gate would be both redundant and *stricter* in a way we don't want to
assert (two runtimes can each sit +2/−2 vs baseline — 4px apart — yet both are
"identical within tolerance" by D7). The report's job is to *surface* drift, and a
position exceeding tolerance is a finding to triage into a `perOp`
`trace-tolerance.json` entry (D7's reserved hook), not to silence.

**Report is a regenerable artifact, not committed (chosen).** Consistent with
D7's "side metadata is a sidecar CI artifact." `CrossJdkTraceDiffTest` writes
`report.md` + `report.json` to `target/`; CI publishes them as the
`trace-diff-report` artifact. Committed curation is deferred to the `trace-curator`
slice, which will read `report.json`.

**CI data flow.** The runtimes run as separate jobs with no shared filesystem, so
traces move as artifacts. `build` (21) and `test` (8/11/17) add
`-DtraceDumpDir=target/trace-dump/jdk-N` (enabling the otherwise-inert
`GoldenTraceDumpModeTest`) and upload `trace-dump-jdk-N`. A new `trace-diff` job
(`needs: [build, test]`) downloads them and runs the aggregator on a **plain
JDK-21 runner** — it renders nothing, so it needs no dev container / Xvfb /
toolchain.

**Two surefire gotchas the wiring has to respect (learned the hard way).** (1)
**Discovery:** the gated modes only run if surefire *discovers* them, which means
their class names must match the default include patterns (`*Test`), hence
`GoldenTraceDumpModeTest` / `CrossJdkTraceDiffTest` — a `…Mode`/`…Diff` name is
silently never run in the full-suite `test`/`verify` (the way the dump rows
invoke it), only via an explicit `-Dtest=`. (2) **Fork delivery:** the gating
value must reach surefire's *forked* JVM — especially the `crossjdk` toolchain
fork. A bare CLI `-Dtrace.dump.dir` does not reliably cross that boundary, and
`systemPropertyVariables` refuses to forward a name colliding with a CLI user
property, so the toggles travel via surefire **`argLine`** (the same channel the
D25 `file.encoding` pin already uses). To avoid a Maven-property-vs-system-
property name clash, the Maven knobs are named distinctly (`traceDumpDir`,
`traceDiffInputDir`, `traceDiffOut`, `traceRecord`) and argLine maps them to the
`trace.*` system properties the tests read; empty defaults keep them inert in a
normal build.

**Validation.** End-to-end locally on JDK 21: the dump wrote 41 traces (the 1 skip
is `chart.xml`, which has no golden), and the aggregator reported 0px spread vs the
goldens (the baseline was recorded on a JDK-21-equivalent). A fault-injection check
(synthetic `jdk-8`/`jdk-11` dumps perturbed +3px / +1px at one `drawString`)
confirmed the report flags the 3px position as over-tolerance, keeps the 1px
sub-tolerance drift in the per-runtime column, and reports structural/categorical
identical. `./mvnw -B -DskipTests verify` stays green (new test sources pass
Spotless/Checkstyle 13/SpotBugs; goldens and the gate output unchanged). True
multi-JDK data comes from CI. (Cross-ref D7/D24/D25/D31.)

## D34 — trace-curator: first-cut backend-portability docs from the committed goldens

**Date:** 2026-06-20. **Status:** accepted. **Phase:** 2.

**Context.** Phase 2's last item after D33 (which produces the per-JDK trace
dumps and the informational cross-JDK divergence report) is the `trace-curator`
work the ROADMAP reserves: populate `project-docs/backend-portability/` from the
trace data. The three docs there
(`RENDERING-PRIMITIVES.md`, `LAYOUT-ALGORITHMS.md`, `INPUT-SURFACE.md`) had been
Phase-0 stubs awaiting this slice; `CROSS-JDK-TRACE-DIFF.md` (D33) was already
complete. This decision records *how* the curation was started and its scope.

**Decision.**

1. **Realize the curator as a reusable agent + a first-cut population.** The
   ROADMAP literally calls `trace-curator` an "agent", so the repeatable
   procedure is codified at `.claude/agents/trace-curator.md` (a deletable
   `.claude/` meta artifact, registered in `.claude/MANIFEST.md`), *and* the two
   trace-backed docs are authored now from the committed goldens
   (`thinlet-core/src/test/resources/trace/{demo,drafts,amazon}/*.json`):
   `RENDERING-PRIMITIVES.md` from the `calls` vocabulary (the 11 observed
   `Graphics2D` ops), `LAYOUT-ALGORITHMS.md` from the `layout` widget bounds (26
   widget classes) cross-referenced to `doLayout` (`Thinlet.java:193`) via
   `.claude/PAINT-PIPELINE-MAP.md`.

2. **`INPUT-SURFACE.md` is deferred, not written.** The golden-trace harness
   records the **paint stream** (`TracingGraphics2D`) and **resolved layout**
   (`LayoutTrace`) only — it captures **no AWT input events**, so there is no
   trace to curate an input inventory from. Writing it from memory would violate
   the precise-language agreement. The stub is refined to state this and to name
   the two future paths (extend the harness to record an input-event trace, or a
   source-derived pass over `Thinlet.java`'s listeners). Tracked as remaining
   Phase-2 work.

3. **Cross-JDK drift is cited by mechanism, not by number.** The docs reference
   `CROSS-JDK-TRACE-DIFF.md` and the D7 ±2 px `FontMetrics` absorption but commit
   **no per-JDK figures**: the real multi-runtime `report.json` is produced only
   in CI (JDK 8/11/17 are not present in the authoring container — the toolchains
   point at `/opt/jdk{8,11,17}`, image-provided). Any position that exceeds
   tolerance is a `perOp` `trace-tolerance.json` candidate (D7's reserved hook),
   not prose and not a reason to widen `defaultPx` or re-record (D33).

**Observed vs. implemented (a curation rule worth recording).** The doc spine is
the *observed* surface (what the corpus actually paints), not every primitive in
source. Example: `Thinlet.java` contains `drawRect` (4 call sites) but **no
golden emits `drawRect`** — that path is unexercised by the static corpus render.
The agent definition encodes this: enumerate from the goldens, flag
source-only primitives explicitly, never invent an op/class.

**Scope / non-goals.** Documentation only — **zero** product or behavior change:
no `Thinlet.java` edits, no golden re-record, no `trace-tolerance.json` change,
no test changes. Build is unaffected (`thinlet-core` Java/goldens untouched);
`./mvnw -B -DskipTests verify` stays green.

**Validation.** Every op and widget class named in the two docs was derived from,
and re-checked against, the committed goldens
(`grep -ho '"op"…' / '"class"…' | sort -u`); cited `Thinlet.java` line refs
spot-checked against the verbatim import; the docs contain no per-JDK numeric
drift claim. (Cross-ref D7/D27/D33.)

## D35 — INPUT-SURFACE.md as a source-derived first cut; matrix close-out; perOp posture

**Date:** 2026-06-21. **Status:** accepted. **Phase:** 2.

**Context.** D34 left `INPUT-SURFACE.md` deferred because the golden-trace harness
records the paint stream (`TracingGraphics2D`) and resolved layout (`LayoutTrace`)
only — there is no input-event trace to curate from. D34 named two future paths:
extend the harness, or a source-derived pass over `Thinlet.java`'s listeners. With
the cross-JDK **test** matrix (D31) and trace diff (D33) both landed, this is the
last open Phase-2 documentation item. The maintainer chose the source-derived pass
now, with the trace-backed route explicitly acknowledged as later work.

**Decision.**

1. **Write `INPUT-SURFACE.md` from source, labelled as such.** The doc inventories
   Thinlet's AWT input surface read directly from
   `thinlet-core/src/main/java/thinlet/Thinlet.java`: `enableEvents` (`:124`) and
   the `processEvent` dispatcher (`:3605`) over mouse (`handleMouseEvent` `:4673`),
   the synthetic `DRAG_ENTERED`/`DRAG_EXITED` popup events (`:70`–`:71`), the
   reflection-guarded mouse wheel (`:3796`), keyboard (`processKeyPress` `:3907`)
   including Thinlet's reflective focus-traversal takeover
   (`setFocusTraversalKeysEnabled(false)` `:117`), focus (`:3873`/`:3879`), and
   component-resize (`:3886`). It also records the *enabled-but-ignored* ids
   (`KEY_RELEASED`, `MOUSE_CLICKED`). Its provenance banner states plainly that it
   is **source-derived, not trace-backed, not cross-JDK-validated**, and that the
   D7 ±2 px tolerance model is N/A for input (categorical/structural, not pixels).

2. **Close out the cross-JDK test-matrix item.** The `crossjdk` profile +
   `.mvn/toolchains.xml` + the `fail-fast: false` matrix `test` job (JDK 8/11/17,
   plus JDK 21 via the base `build` job) are in place (D31), so the ROADMAP bullet
   moves ⏳ → ✅.

3. **Fix the `perOp` posture without inventing entries.** `trace-tolerance.json`
   stays byte-unchanged (`{ "defaultPx": 2.0, "perOp": {} }`). `perOp` remains
   empty until CI's cross-JDK diff surfaces an over-tolerance position; only such a
   *finding* earns an entry — never a `defaultPx` widening or a re-record (D7).
   JDK 8/11/17 are absent in the authoring container, so no entry can be authored
   locally; the posture is now recorded on the ROADMAP rather than left ambiguous.

**Records the need, builds nothing.** A source-derived doc cannot show whether
input *behavior* diverges across JDKs. `INPUT-SURFACE.md` and the ROADMAP therefore
name the **input-capture harness** as the prerequisite and an explicit Phase-3
deliverable: an input driver (scripted AWT events into a headless Thinlet on Xvfb
`:99`), a dispatch recorder (the input counterpart to `TracingGraphics2D`/
`LayoutTrace`, serializing handler routing + resulting focus/selection/caret/scroll
state into golden input-traces), and replay fixtures fed through D33's per-JDK dump
+ `CrossJdkTraceDiffTest`. This slice builds none of it.

**Scope / non-goals.** Documentation only — **zero** product or behavior change: no
`Thinlet.java` edits, no golden re-record, no `trace-tolerance.json` change, no test
changes. Build is unaffected (`thinlet-core` Java/goldens untouched);
`./mvnw -B -DskipTests verify` stays green. This entry *resolves* D34's
`INPUT-SURFACE.md` deferral.

**Validation.** Every `Thinlet.java` line ref cited in `INPUT-SURFACE.md` was
spot-checked against the verbatim import at authoring; the doc commits no per-JDK
numeric drift claim; `trace-tolerance.json` is unchanged. (Cross-ref D7/D27/D33/D34.)

## D36 — Input-capture harness resequenced to a Phase 2.x gate; reframed as a refactor-safety net; feasibility probe landed

**Date:** 2026-06-21. **Status:** accepted. **Phase:** 2.

**Context.** D35 named an input-capture harness as a future **Phase 3** deliverable,
framed cross-JDK-first (the input counterpart to the trace diff). Reviewing the gap it
fills surfaced a sequencing problem: the golden net is **paint + layout only** (it
dispatches no input), so ~26% of `Thinlet.java` — `processEvent` (`:3605`),
`handleMouseEvent` (`:4673`), `processKeyPress` (`:3907`), `processField`,
`processScroll`, `findComponent` — has zero automated coverage. A regression net only
certifies a refactor when it records the baseline **before** the change; built after an
input refactor it can only certify the post-refactor behavior. So for input-touching
Phase 3 work the net is **now or never**, and without it those refactors stay
"smoke-tested," never "confirmed behavior-preserving" (the project thesis; CLAUDE.md
precise-language rule).

**Decision.**

1. **Resequence to Phase 2.x, gating Phase 3.** Phase 2.0 is closed (✅); the
   input-capture harness becomes Phase 2.x and **Phase 3 does not start until it is
   accepted**. This *amends* D35's Phase-3 placement.

2. **Reframe as a same-JDK refactor-safety net.** The primary purpose is catching
   behavior change across a refactor on one JDK; the cross-JDK input *diff* is a later
   layer on top, not the primary goal (correcting D35's cross-JDK-first framing).

3. **Black-box design; cut the dispatch recorder.** Drive the real `protected
   processEvent` funnel; target widgets by `find(name)`; assert outcomes **black-box**
   via public getters (`getBoolean`/`getString`/`getSelectedIndex`/`getInteger`) and
   **re-paint `Trace` diffs**, reusing the Phase 1 `TracingGraphics2D`/`TraceComparator`
   (no new serializer). The dispatch/routing recorder sketched in D35 is **dropped** —
   recording internal handler routing would re-lock the very internals refactoring is
   meant to change, so it is hostile to the net's purpose.

4. **Probe first, then the first real build (MVP), behind an acceptance gate** that may
   legitimately conclude *infeasible*.

**Probe result (this slice).** A test-scope feasibility probe landed under
`thinlet-core/src/test/java/thinlet/trace/` (`InputProbeDriver`/`InputProbeTest`/
`InputProbeHandler`) with a `probe.xml` fixture. On headless Xvfb `:99`, JDK 21, all
seams are green and deterministic: mouse click → checkbox toggle (getter), click →
handler action, re-paint trace diff + run-to-run determinism, and — the seam most
likely to fail headless — **keyboard + synthetic focus** (typing into a focused field).
Findings (incl. the priming `MOUSE_MOVED`, paint-time bound computation, and synthetic
`FOCUS_GAINED`) and the gate are recorded in
`project-docs/backend-portability/INPUT-HARNESS-PROBE.md`. Recommendation there:
**feasible — proceed to the MVP**; cross-JDK (8/11/17) determinism is delegated to the
`crossjdk` CI matrix (those JDKs are absent in the authoring container).

**Scope / non-goals.** Adds **test-scope code only** — no `Thinlet.java` change, no
golden re-record, no `trace-tolerance.json` change; `thinlet-core` stays
runtime-dependency-free and the existing golden tests are unaffected. The probe lives in
package `thinlet.trace` (not `thinlet.input`) to reuse the package-private trace types
without widening Phase 1 visibility. The MVP (broader fixtures/scenarios; graduating
`INPUT-SURFACE.md` to trace-backed) is **not** built here — it waits on acceptance of
this gate. Deferred regardless: list/tree/combo scroll-offset targeting, drag
pseudo-events, tooltip/auto-repeat timers, keyboard type-ahead timing.

**Validation.** `./mvnw -B verify` green on JDK 21 (0 Checkstyle, 0 SpotBugs; probe +
existing goldens pass). Same-JDK feasibility is confirmed by deterministic test and
direct observation; cross-JDK is explicitly pending CI. (Cross-ref D7/D22/D31/D33/D34/D35.)

## D37 — Input-capture regression MVP: named-scenario gate (getters + ephemeral re-paint diff); probe graduated; library extraction deferred

**Date:** 2026-06-21. **Status:** accepted. **Phase:** 2.

**Context.** D36 landed the feasibility probe and recommended *proceed to the MVP*.
This slice builds that MVP — the actual regression net that gates Phase 3 — turning the
single-fixture probe into a named-scenario suite over the previously uncovered input
surface (`processEvent`/`handleMouseEvent`/`processKeyPress`/`processScroll`).

**Decision.**

1. **Named-scenario gate, not corpus replay.** The net is a curated set of black-box
   scenarios covering `list` selection (click, Arrow/Home/End, Shift-extend multi-select),
   `tree` selection + keyboard expand/collapse + descent, `combobox` popup-open + keyboard
   commit, and mouse-wheel `scroll`. (Driving the vendored corpus through input was
   rejected — the corpus binds demo handlers and asserts nothing about input.)

2. **Assertions = public getters (primary) + ephemeral re-paint `Trace` diff
   (corroboration); no committed input goldens.** The getter
   (`getSelectedIndex`/`getSelectedItem(s)`/`getBoolean`/`getString`/`getCount`) is the
   exact, JDK-invariant assertion; a same-JVM before/after `TraceComparator.compare(…,0.0)`
   corroborates "something visibly changed," and run-to-run determinism is proven once in
   the smoke layer. There is **no input golden file** to re-record — input state is read
   live, so the net cannot drift the way a stored baseline could.

3. **Probe folded into the suite.** `InputProbeDriver`→`InputDriver` (adds `press(keyCode,
   modifiers)` + Arrow/Home/End/Enter helpers, `scroll`, and a generalized `property`/
   `viewRect` `Object[]` reader); `InputProbeHandler`→`InputHandler`; `probe.xml`→
   `smoke.xml`; the four probe cases become `InputSmokeTest`. New per-widget classes:
   `InputListTest`, `InputTreeTest`, `InputComboBoxTest`, `InputScrollTest`. All carry
   `@Tag("input")` and **run by default** in `./mvnw -B verify` (the net must gate every
   build); since no `<excludedGroups>` exists, default execution needs no pom change — the
   tag is a manual selector only (`-Dgroups=input`).

4. **Two driver findings beyond D36.** (a) **Keyboard dispatch split:** Thinlet runs
   `processKeyPress` only when `control == (id == KEY_PRESSED)` (`:3827`), so
   navigation/control keys (Arrows/Home/End/PageUp-Down/Enter/Esc) must be **KEY_PRESSED**
   with `CHAR_UNDEFINED`, while printable characters **including the space bar** (0x20 is
   not a control char) go through **KEY_TYPED** — hence space stays out of the `press`
   helpers. (b) **Wheel scroll** needs a real `java.awt.event.MouseWheelEvent` (Thinlet
   reads `getWheelRotation()` reflectively, `:3802`) plus the same priming `MOUSE_MOVED`
   as `click`. Neither scroll offset (`:view`) nor combobox open-state (`:combolist`) has
   a public getter, so both are read off the `Object[]` model exactly as `LayoutTrace`
   reads `"bounds"`; scroll is asserted on **direction**, never an exact pixel.

5. **Library extraction deferred (was floated this slice).** The harness stays in package
   `thinlet.trace`, test scope, on the current layout. A standalone `thinlet-testkit`
   Maven module was rejected *for now*: it must depend on `thinlet-core` (it subclasses
   `Thinlet` for `processEvent`), so any consumer creates a `thinlet-core(test) → testkit
   → thinlet-core(main)` reactor cycle — breaking it forces relocating the Phase 1 golden
   suite + the Xvfb/`crossjdk` CI wiring into the new module, far larger than this MVP.
   Revisit in Phase 3 when a second consumer actually exists.

**Scope / non-goals.** **Test-scope only** — no `Thinlet.java` change, no golden
re-record, no `trace-tolerance.json` change; `thinlet-core` stays
runtime-dependency-free; existing golden tests unaffected. Cross-JDK input determinism
(8/11/17) is delegated to the `crossjdk` CI matrix (those JDKs are absent in the authoring
container). Still deferred (per D36): list/tree/combo scroll-offset *item* targeting, drag
pseudo-events, tooltip/auto-repeat timers, and **keyboard type-ahead** (wall-clock +
text-width dependent → non-deterministic and FontMetrics-sensitive, so excluded). No
`KNOWN-QUIRKS` change — no scenario surfaced a locked quirk.

**Validation.** Input group green on JDK 21 — 16 tests across `InputSmokeTest` (4),
`InputListTest` (4), `InputTreeTest` (4), `InputComboBoxTest` (2), `InputScrollTest` (2);
`./mvnw -B verify` green (0 Checkstyle, 0 SpotBugs, Spotless clean; input suite + existing
goldens pass). Same-JDK confirmed by deterministic test + direct observation; cross-JDK
pending CI. (Cross-ref D7/D22/D31/D36.)

## D38 — Markdown filename casing convention (`UPPERCASE-WITH-HYPHENS.md`)

**Date:** 2026-06-21. **Status:** accepted. **Phase:** 2 (housekeeping).

**Context.** Project-authored markdown had drifted in casing — most docs were lowercase
(`encoding-inventory.md`, the `backend-portability/` set, `.claude/paint-pipeline-map.md`)
and `KNOWN_QUIRKS.md` used an underscore separator. The maintainer's convention is a single
consistent style for authored markdown.

**Decision.**

1. **Authored markdown filenames are `UPPERCASE-WITH-HYPHENS.md`** — uppercase, hyphen
   separators (no underscores, camelCase, or spaces).
2. **Exceptions** kept as-is because an ecosystem or the harness fixes the name:
   `README.md`, `CLAUDE.md` (auto-loaded by Claude Code), and Claude Code **agent files**
   under `.claude/agents/*.md` (the `subagent_type` is tied to the lowercase filename — so
   `.claude/agents/trace-curator.md` stays lowercase).
3. **Renames applied** (`git mv`, history preserved): `KNOWN_QUIRKS.md` →
   `KNOWN-QUIRKS.md`; `project-docs/encoding-inventory.md` → `ENCODING-INVENTORY.md`;
   `project-docs/backend-portability/{cross-jdk-trace-diff,input-surface,input-harness-probe,layout-algorithms,rendering-primitives}.md`
   → their `UPPER-CASE` forms; `.claude/paint-pipeline-map.md` → `.claude/PAINT-PIPELINE-MAP.md`.
   The ephemeral `.claude/SESSION-HANDOFF.md` (a chat seed file) was deleted in the same
   pass and its `.claude/MANIFEST.md` row removed.
4. **References updated repo-wide**, including **inside historical D1–D37 entries** — a
   deliberate, maintainer-approved exception to this log's append-only discipline (chosen so
   no cross-reference dangles). No file is loaded by code/build by name; every reference is
   prose or a comment (docs, config XML comments, test Javadoc), so the renames are
   functionally inert. The convention is also recorded in `CLAUDE.md`.

**Scope / non-goals.** Docs/comments only — no `Thinlet.java` change, no test behavior
change, no golden re-record. The `.claude/agents/trace-curator.md` *file* is not renamed
(exception above); its internal doc links are handled separately. (Cross-ref D27 doc layout.)

## D39 — Phase 2.y: broaden the input net (splitpane slice) + a font-scaling dimension

**Date:** 2026-06-22. **Status:** accepted. **Phase:** 2.y.

**Context.** The input MVP (D37) is deliberately minimal (list/tree/combobox/scroll +
smoke). Per D36, the net's value is capturing a baseline *before* an input-touching
Phase 3 refactor, so widgets must be covered *now*, not mid-refactor. The maintainer also
set the end-goal explicitly: the 2005 toolkit must behave **on 2026+ hardware**, of which
the simplest deterministic slice is **font scaling** (a larger base font scales every
FontMetrics-driven dimension without a real HiDPI device transform). Phase numbering stays
**2.y** (not renumbered to a top-level phase: renumbering would re-point ~5 historical
"Phase 3" references to "Phase 4" for little gain — the append-only log is a convention,
not a hard rule, but there's no reason to churn it here).

**Decision.**

1. **Phase 2.y broadens the input net** to the remaining interactive widgets — `table`,
   `tabbedpane`, `spinbox`, `slider`, menus/`popupmenu`, text editing
   (`textfield`/`passwordfield`/`textarea` caret/selection), `dialog` focus, and
   **`splitpane`** — reusing `InputDriver`, getter-asserted + ephemeral re-paint diff,
   `@Tag("input")` (run by default). Shippable in **per-widget slices**, not one PR.
2. **New driver gestures:** `dragInside` (divider/scrollbar drags), `resize` (real
   `COMPONENT_RESIZED` re-layout), and a **`fontScale`** `load` parameter (the scaling
   proxy). Two findings encoded in the driver: (a) Thinlet's `validate()` defers
   re-layout by flagging a component dirty via a **negative `bounds.width`** — so gestures
   whose handler reads `bounds` need a `paint()` between them (the test models the EDT's
   inter-keystroke repaint); (b) `processEvent` dispatches `MOUSE_EXITED` on the *first*
   drag event that leaves the grabbed component and only routes `MOUSE_DRAGGED` to it on
   the next, so `dragInside` emits the destination drag **twice** (the OS streams many).
3. **Font-scaling dimension:** at least the metric-sensitive widgets run at 1× and a
   larger font (parameterized), asserting the **model outcome is scale-invariant**.
   Honest scope: this is the metric half of scaling, **not** real device/HiDPI rendering
   (the `GraphicsConfiguration` transform) — that stays Phase 3.
4. **Quirk discipline (unchanged):** behaviors that are wrong-but-2005 are pinned with
   `@Tag("documents-current-behavior")` + a `KNOWN-QUIRKS` entry and triaged for Enhanced
   Thinlet — 2.y **characterizes/locks**, Phase 3 **fixes**.
5. **First slice landed — splitpane** (`InputSplitPaneTest`, fixture `input/splitpane.xml`):
   keyboard divider (F8-focus → Home/End/Left/Right), drag (divider = cursor − the 2px
   handle-grab centering; verified scale-invariant at 1×/2×), auto-divider scales with
   font, and the **resize quirk → `KNOWN-QUIRKS` Q2** (divider is absolute pixels:
   non-proportional on grow, destructive clamp on shrink). Note checked-and-*not*-a-quirk:
   the 2px drag offset centers the cursor on the 5px handle (correct), and the transient
   negative `bounds.width` is the dirty-flag idiom (correct), not corruption.

**Scope / non-goals.** Test-scope only — no `Thinlet.java` change, no golden re-record, no
`trace-tolerance.json` change. Cross-JDK input determinism delegated to the `crossjdk`
matrix. Still deferred: type-ahead, drag-reorder/drag-select, tooltip/auto-repeat timers,
`thinlet-testkit` extraction, fully trace-backed `INPUT-SURFACE.md`, and **real HiDPI/device
rendering** (Phase 3). (Cross-ref D7/D22/D36/D37.)

**Validation.** `InputSplitPaneTest` — 5 tests green on JDK 21 (keyboard, drag ×2 scales,
auto-divider scaling, resize quirk). `./mvnw -B verify` green (Spotless/Checkstyle/SpotBugs,
full suite); cross-JDK 8/11/17 via CI.

## D40 — Text-editing slice + a `java.awt.Robot` fidelity cross-check for the input net

**Date:** 2026-07-02. **Status:** accepted. **Phase:** 2.y.

**Context.** Two things landed together. (1) The next Phase 2.y widget slice — **text
editing** (the largest untested input path, `processField`). (2) The maintainer asked
whether `java.awt.Robot` would give more faithful outcomes than the synthetic driver.

**Analysis (Robot).** The synthetic `InputDriver` builds `MouseEvent`/`KeyEvent` and calls
Thinlet's real `protected processEvent` — and **Thinlet's entire input logic begins at
`processEvent`**. What Robot adds (native OS input → AWT pump → focus/activation → the OS
keymap turning keycodes into keychars) is the layer *below* Thinlet — exactly the
JDK/OS-variable part the D7 tolerance model is meant to *absorb*, not assert. So Robot
would not exercise any Thinlet path the synthetic driver misses; its value is **validating
the driver's shortcuts** (the synthesized `FOCUS_GAINED`, the KEY_PRESSED/KEY_TYPED split,
the priming `MOUSE_MOVED`) against a genuine native path. Robot's costs here: it needs a
realized/shown/focused Frame at screen coords, async focus/timing, OS-keymap keychars, and
it does not run under true `-Djava.awt.headless=true` — all corrosive to the determinism
that is the net's whole point.

**Decision.**

1. **Keep the synthetic `processEvent` driver as the primary net.** It is the right tool
   for deterministic, cross-JDK, headless characterization of *Thinlet's* behavior.
2. **Add a small Robot fidelity cross-check** (`InputRobotFidelityTest`, `@Tag("robot")`):
   run representative gestures (native click → checkbox toggle; native focus + typing →
   textfield) through a real `Robot` on a shown undecorated `Frame` on Xvfb `:99`, and
   assert the model outcome **equals the synthetic driver's**. It runs on the **base
   JDK-21 build** and is **excluded from the cross-JDK matrix** (`-DexcludedGroups=robot`
   in `ci.yml`) — native focus/timing is out of scope there.
3. **Findings from building it:** native focus **works** on WM-less Xvfb (the typing
   outcome matches — confirming the synthesized `FOCUS_GAINED` is faithful); one gotcha
   handled — **X keyboard auto-repeat** inflates a held key, so Robot presses+releases with
   zero delay. Robot is ~5 s/test (real frame) vs sub-millisecond synthetic — another
   reason it is a thin cross-check, not the net.
4. **Text-editing slice** (`InputTextEditTest`, fixture `input/textedit.xml`): typing at
   caret, Backspace/Delete, Home/End/arrow caret nav, Shift-selection + type-to-replace,
   Ctrl+A select-all, selection delete, boundary clamps (no-ops), `passwordfield` stores
   the real text (masking is paint-only), and `textarea` Enter-inserts-newline +
   backspace-joins-lines. All index-based, hence font-invariant (no scaling dimension
   needed here — splitpane carries it). **Deferred:** mouse click → caret index
   (`getCaretLocation` needs the field's `:offset`/`referencex` state a bare synthetic
   press doesn't prime) — a candidate for the Robot cross-check to validate.

**Scope / non-goals.** Test-scope only (+ the `ci.yml` `excludedGroups` line); no
`Thinlet.java` change, no golden re-record. Robot is a *validation layer around* the net,
not a second driver; expanding it (or switching the net to Robot) is explicitly not done.
(Cross-ref D7/D22/D36/D37/D39.)

**Validation.** `InputTextEditTest` 10 green; `InputRobotFidelityTest` 2 green, on JDK 21.
`./mvnw -B verify` green (Spotless/Checkstyle/SpotBugs, full suite). Cross-JDK 8/11/17 runs
the input suite but not `@Tag("robot")`.

## D41 — Resolve the D40 click→caret deferral (mouse click repositions the caret)

**Date:** 2026-07-05. **Status:** accepted. **Phase:** 2.y.

**Context.** D40 deferred one text path — mouse **click → caret index**
(`getCaretLocation`) — on the hypothesis that a synthetic press could not reproduce it
without priming the field's `:offset`/`referencex` state. A **manual probe on a real
desktop** (scratch branch `manual/caret-probe`, not merged) settled it by direct
observation: a real click lands the caret on the character boundary nearest the click
(`hello world` → clicking before `llo`/`wor`/after `d` gives caret 2 / 6 / 11, with
`start==end`).

**Root cause of the deferral (corrected).** The hypothesis was wrong. `processField`'s
MOUSE_PRESSED branch **self-primes** its reference — it calls `setReference(component,
2+left, 2)` and reads `:offset` with a **0 default** (`Thinlet.java:5136-5148`) — so the
caret math needs no pre-existing state. The synthetic click failed earlier only because
`InputDriver.click` always aims the **widget centre**, which for a short string in a wide
field lands past the text and clamps to `text.length()` (read at the time as "caret didn't
move"). The real gap was aiming, plus the `validate()` dirty-flag idiom (an edit/caret
click negates `bounds.width` until the next paint; a stale negative width makes the
hit-test miss the field — the same artifact the splitpane keyboard tests handle with
`paint()` between steps).

**Decision.**

1. **`InputDriver` gains `clickAt(widget, xOffset[, yOffset])`** (primary click at a chosen
   spot, same MOUSE_MOVED prime as `click`) and **`size(widget)`** (bounds width/height,
   read off the `Object[]` chain like the other geometry). No `Thinlet.java` change.
2. **`InputTextEditTest` covers click→caret** with **FontMetrics-tolerant** assertions
   (D7): a left-edge click collapses the caret to `0`; a click past the short text clamps
   to the length; a left→right sweep is **monotonic non-decreasing**, every single click
   **collapses the selection** (`start==end`), and **some interior click lands strictly
   inside** the text (proves real positioning, not just the two clamps). A companion test
   asserts a **press-drag selects** the press→release range. Exact per-pixel indices are
   FontMetrics-dependent and deliberately **not** asserted.
3. **Robot fidelity gains a native click→caret case** (`InputRobotFidelityTest`,
   `@Tag("robot")`): native type + native clicks at the two edges, asserting the caret
   clamps (`0` / length) **equal the synthetic driver's** — confirming `clickAt` reproduces
   a genuine click. Interior indices are not matched natively (pixel-fragile under Robot).

**Scope / non-goals.** Test-scope only; no `Thinlet.java` change, no golden re-record. The
`manual/caret-probe` scratch branch stays unmerged (it is a manual harness, not a build
artifact). (Cross-ref D7/D22/D37/D39/D40.)

**Validation.** `InputTextEditTest` 12 green; `InputRobotFidelityTest` 3 green (native
caret case ran twice, stable), on JDK 21. `./mvnw -B verify` green (Spotless/Checkstyle/
SpotBugs, full suite). Cross-JDK 8/11/17 runs the input suite but not `@Tag("robot")`.

## D42 — Phase 3 opens: modernise the library internals behind the net; CI-autonomous workflow

**Date:** 2026-07-06. **Status:** accepted. **Phase:** 3 (3a). **Supersedes:** the
"modernize the toolchain, not the library" posture and the "consult before opening a PR"
note (both in `CLAUDE.md`) for Phase 3 onward.

**Context.** Phases 0–2 deliberately held *modernize the toolchain, not the library —
preserve 2005 observable behavior exactly*. That was the right posture **while building the
safety net** (golden-trace paint+layout + input-capture). The maintainer's actual goal is
**modernise, then enhance**: restructure the 7,779-line `Thinlet.java` God class into
idiomatic modern Java (Java 8 floor) on a clean base, then re-implement — cleanly — prior
production enhancements (two custom `Thinlet.java` forks + apps, battle-tested supporting a
global bank ~2006+; not runnable here, but diffable against the 2005 baseline). A full
idiomatic rewrite / new public API is a later step, once real apps run on the modern base.
Phase 3 is where the net finally gets *spent*.

**Decision.**

1. **Modernise the library internals** behind the net, holding **2005 observable behavior
   AND the public API constant** (both non-negotiable). Supersedes "toolchain not library"
   for Phase 3+. Enhancements (fixing quirks Q1/Q2, HiDPI, the fork functionality) are
   deferred to a later phase, after 3a's clean base exists and real apps exercise it.
2. **Sequencing is driven by net strength** (readiness assessment, this session). The
   dominant obstacle is the **interned-String `==` contract** (~418 identity-compares vs
   string literals — classname dispatch, part tokens, enum-like values), which leaks across
   all subsystems and silently breaks any typed refactor with no compile error. Order:
   **Cut 1** neutralise `==` behind semantics-preserving helpers → **Cut 2** paint → typed
   Renderer (net captures the full primitive stream) → **Cut 3** DTD → typed descriptors +
   accessor façade → **Cut 4** layout (a hub; second) → **Cut 5** `Object[]` model → typed
   Widget (late) → **Cut 6** event/input/focus **last** (thinnest net; backfill
   characterization tests first).
3. **CI is the autonomous behavior net; the maintainer is not a manual dependency.**
   `ci.yml` fires only on `pull_request`→`main` (push→`main` is blocked), so **Claude opens
   the PRs** to run the golden+input net across JDK 8/11/17/21, running compile +
   Spotless/Checkstyle/SpotBugs locally pre-push and driving each PR to green. This
   supersedes the "consult before opening a PR" note for Phase 3. **Behavior-preserving cuts
   must produce no golden/input diff** within ±2 px (D7). **Merge to `main` remains the
   maintainer's** 1-click gate on the trunk unless explicitly delegated (opt-in: GitHub
   auto-merge, squash-on-green). A faithful **local CI** loop (dev container) is a later
   joint task.

**Scope / non-goals.** 3a is behavior- and API-preserving refactoring only — no user-visible
change, no golden re-record. No enhancement or quirk-fix lands in 3a (the parse-NPE→
`IOException` fix, KNOWN-QUIRKS Q1, is the earmarked *first* enhancement, later).

**Validation.** Per-cut, via the CI net: golden `GoldenTraceRegressionTest` + `@Tag("input")`
suite green across JDK 8/11/17/21, no diff within tolerance for behavior-preserving cuts;
local compile+lint pre-push. Cut 1's result lands in its own PR. Cross-ref D7, D31, D36/D37,
KNOWN-QUIRKS Q1/Q2.

## D43 — Fable review of the Phase 3 plan: Cut 1 verified; interning tripwire; 3a visibility discipline

**Date:** 2026-07-07. **Status:** accepted. **Phase:** 3 (3a).

**Context.** Before Phase 3 work began in earnest, the maintainer asked for an independent
review of the D42 plan and of Cut 1 (`.claude/FABLE-NEXT-STEPS.md` §5). The review re-verified
the Cut 1 sweep mechanically and audited the net / japicmp / CI structure. Outcome: **plan
endorsed** — the refinements below, no resequencing.

**Findings.**

- **Cut 1 verified behavior/API-preserving.** Zero raw `== "…"` comparisons remained in live
  code except one (below); wrapping is mechanically correct (operand order, parenthesization,
  `!=` → `!is`) in all sampled regions; the model-core `entry[0] == key` compares are
  correctly untouched. Corrected figures: **449** wrapped sites (396 `is` + 53 `!is`), not
  ~418; the goldens cover **41/42** corpus files, not "40/41"; **7** comment lines were
  cosmetically rewritten, not 3 (`.claude/FABLE-NEXT-STEPS.md` corrected in place).
- **One seam escapee:** `(getString(component, "selection", "single") != "single")` in the
  Ctrl+A select-all handler (~L4473) — a `!=` with a call-expression left operand the scripted
  pass skipped; its siblings were wrapped. No behavior impact; wrapped in this slice.
- **japicmp already gates public + protected** (no `<accessModifier>` configured → plugin
  default `protected`; D29's `find(String)` demotion validation exercised exactly that), so
  the subclass surface is covered with no config change. Reminder: it gates binary *breaks*
  only — not source-incompatibilities and not *additions* (see Decision 3).
- The `trace-tolerance.json` `perOp` hook is reserved but **not implemented** in
  `TraceComparator` (it reads `defaultPx` only) — to be built if/when a cross-JDK finding
  earns an entry; the D35 posture is unchanged.

**Decision.**

1. **Interning tripwire in `is()`.** The helper preserves the `==` contract but not its
   silent failure mode: a refactor that breaks the interning chain (DTD literal pool,
   `create()` re-canonicalization) flips comparisons to `false` with no compile error and no
   test signal. With the `thinlet.strictIntern` system property `true`, `is()` now throws
   `IllegalStateException` on a token that is `equals`-equal to the literal but not identical.
   **The test net always runs strict** (surefire argLine — the D25/D33 channel; Maven knob
   `strictIntern`, named distinctly per the D33 shadowing gotcha). Production default is off:
   the `static final` flag makes the branch dead code and semantics byte-identical to `==`
   (verified by direct observation, flag on and off). Deliberately a system-property flag,
   **not** `assert`: a downstream app running `-ea` must see zero behavior change.
   `InternTripwireTest` guards both the argLine delivery into (toolchain-forked) test JVMs
   and the firing behavior; it runs behind `XvfbDisplayExtension` because `Thinlet` class
   init reaches AWT and a failed init would poison the class for the whole fork. `is()` is
   widened private → package-private for the test (invisible to japicmp). **A tripwire hit
   in CI is a finding to triage** — a legitimate 2005 equals-but-not-interned path would be a
   `KNOWN-QUIRKS` candidate — never a failure to silence. Armed *before* Cut 3, which touches
   the interning chain itself.
2. **Seam completeness.** The ~L4473 escapee is wrapped as `!is(...)`.
3. **3a visibility discipline.** japicmp gates breaks, not additions: any new public type
   published in a v0.1.x release becomes de-facto frozen API, and the Java-8 floor (no JPMS)
   means subpackages force public types. Therefore **every class/member extracted during 3a
   stays in package `thinlet`, package-private**; the clean subpackage layout belongs to the
   later new-API phase.
4. **Sequencing refinements** (charter `project-docs/PHASE-3-GOALS.md` updated). Cuts 2 and 3
   are *overlappable*: Cut 2's prerequisites (the dev-container local CI loop — promoted to a
   blocking prereq for Cut 2 iteration, since the bare host cannot run goldens faithfully —
   and the interaction-state golden work) take real time, and Cut 3's descriptor-table core
   can proceed behind `getDefinition` meanwhile; design the Renderer dispatch anticipating
   typed descriptor keys. Interaction-state goldens need a **determinism design** first
   (caret blink is timer-phase-dependent; hover/press are held-state captures). A fork
   **catalog diff** runs as soon as the two fork sources arrive (expected 2026-07-08), to
   *verify* — not assume — that Cuts 2–4 don't overlap the enhancement surface. Cut 6's
   "backfill characterization tests" prerequisite **is** finishing Phase 2.y (verified
   uncovered: menus/popupmenu, spinbox, slider, tabbedpane, tooltip, dialog drag/resize,
   scrollbar mouse drag/track-click, Tab focus traversal, clipboard).

**Scope / non-goals.** Production diff is minimal and inert by default: the tripwire branch
(dead code unless the property is set), one wrapped comparison, `is()` private →
package-private. No golden re-record, no `trace-tolerance.json` change, no quirk fix, no
public-API change. Maintainer follow-up noted: confirm GitHub branch protection *requires*
the gating CI checks (a server-side setting, not visible in-repo).

**Validation.** Local gates green (Spotless, Checkstyle 0, SpotBugs, compile `--release 8`).
Tripwire semantics verified by direct observation (headless probe: flag on → throws on
de-interned token, all other cases unchanged; flag off → byte-identical to `==`).
`InternTripwireTest` (3 tests) guards wiring + firing in every CI fork. The PR is itself the
empirical check that no legitimate 2005 path feeds an equals-but-not-interned token through
`is()` on JDK 8/11/17/21. (Cross-ref D7/D25/D29/D33/D35/D42.)

## D44 — Faithful local CI loop: run the net inside the published CI container image

**Date:** 2026-07-08. **Status:** accepted. **Phase:** 3 (3a enabling infrastructure).

**Context.** Local golden runs on the bare host are unfaithful — host fonts/hinting differ
from the container the goldens were recorded against, producing false ±2 px diffs — so every
cut so far verified behavior only via CI round-trips. D42 deferred a "faithful local CI loop"
as a later joint task; D43 promoted it to a blocking prerequisite for Cut 2's iteration.

**Decision.** Run the net locally **inside the exact dev-container image CI publishes**
(`ghcr.io/nomixer/thinlet-modernized/devcontainer-ci:latest`, pushed by main-branch CI runs
per D23 and anonymously pullable), rather than rebuilding the image or approximating the
environment. `.devcontainer/ci/local-ci.sh` wraps it:

- No argument → mirrors ci.yml's `build` job: full JDK-21 `verify` (lint gates + golden +
  input + robot), minus the D33 trace-dump knob.
- `8`/`11`/`17` → mirrors a `test` matrix row: `-Pcrossjdk -Djdk.target=N
  -DexcludedGroups=robot -t .mvn/toolchains.xml test`, **scoped `-pl thinlet-core -am`**
  (gotcha below).
- Maven writes to the workspace `.m2` exactly as CI does (the host `~/.m2` is untouched);
  the container user `vscode` is uid/gid 1000, matching the common single-user host, so
  workspace file ownership is preserved.

**Gotcha recorded (the one CI/local divergence found).** CI's unscoped reactor `test` works
only because each CI row starts from a clean checkout: in a local workspace whose `target/`
directories are already populated, surefire in the test-less `thinlet-demos` module gets past
its no-tests early-exit and fails hard on `excludedGroups` requiring a JUnit engine on the
module classpath. The script therefore scopes the crossjdk row to `-pl thinlet-core -am` —
faithful to intent, since the entire suite lives in `thinlet-core` (demos/drafts are
`src/main`-only).

**Validation (direct observation, maintainer host).** Base row via the script: BUILD SUCCESS,
89 tests, 0 failures (41 goldens + full input suite + 3 robot + tripwire; the 2 skips are the
gated dump/diff modes), ~33 s cold including dependency download into the workspace `.m2`.
JDK-8 row via the script: toolchain resolved `/opt/jdk8`, 86 tests, 0 failures (robot
excluded). Cut 2's per-iteration golden verification is now local; **CI remains the
authoritative gate on PRs (D42)** — the local loop informs, the PR net decides.

**Scope / non-goals.** Tooling + docs only — no `Thinlet.java` change, no golden re-record,
no CI change. The `:latest` tag tracks CI's cache image; re-`docker pull` after Dockerfile
changes. Exact image-digest pinning remains D16's open item. This resolves the "later joint
task" wording in D42/`CLAUDE.md`. (Cross-ref D16/D22/D23/D31/D42/D43.)
