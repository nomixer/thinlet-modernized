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

## D45 — Interaction-golden determinism design: no time dependence; the caret does not blink (corrects a D43 premise)

**Date:** 2026-07-08. **Status:** accepted (design; the capture build follows). **Phase:** 3
(Cut 2 prerequisite).

**Context.** D43 required a determinism design before recording interaction-state paint
goldens, on the stated premise that "caret blink is timer-phase-dependent." A full source
survey of `Thinlet.java`'s interaction-state paint reads (post-Cut-1 file, 7,812 lines)
settles the design — and disproves that premise.

**Findings.**

- **The caret does not blink.** No blink-phase state exists in the file; the single `timer`
  thread (`run()`, L3540–3567) dispatches only scrollbar auto-repeat, spinbox auto-repeat,
  and the 750 ms tooltip delay (L3551–3560). The caret is painted unconditionally whenever
  `focus` holds (`paintField` L2512–2514; textarea L2901–2904).
- **A paint frame is a pure function** of the widget model plus seven transient fields —
  `{mouseinside, insidepart, mousepressed, pressedpart, focusinside, focusowner,
  tooltipowner}` (declarations L47–67; the master locals L1635–1637; the part-level gate
  L3232–3233). No clock control is needed anywhere in the capture design.
- The only timer-coupled paint state is the **tooltip** (`tooltipowner`, desktop paint
  L2114–2119) — deferred.
- Re-confirmed the `:lead` **paint-time write** (L2962–2963) — the stray write Cut 2
  relocates first. Interaction goldens must be recorded *before* that relocation, which must
  then be golden-neutral.

**Decision.** Adopt the capture design in `project-docs/INTERACTION-GOLDENS-DESIGN.md`:
states established through the existing `InputDriver` (hover = held `MOUSE_MOVED` — one new
test-scope gesture; press = `MOUSE_PRESSED` without release; focus = the D36-proven
click/synthetic-`FOCUS_GAINED` path; model states via fixtures/setters), traced with the
existing `TracingGraphics2D`, committed under
`thinlet-core/src/test/resources/trace/interaction/` as `<fixture>-<scenario>.json` written
only with `-Dtrace.record=true` (the D24 lifecycle), and gated by a
`GoldenInteractionTraceTest` mirroring the static-golden test at the D7 ±2 px tolerance,
running by default and on the `crossjdk` matrix. Gesture aiming stays bounds-based, never
text-metric-based (the D41 lesson). **This does not reverse D37's no-input-goldens
posture:** these are *paint* goldens captured under a held input-state tuple — the same
artifact class as the 41 static goldens; input outcomes stay getter-asserted live.

**Scope / non-goals.** Documentation + design only this slice — no harness code, no goldens
recorded, no `Thinlet.java` change. Deferred: the tooltip paint golden, drag-in-progress
visuals, menus beyond the combolist (they join the net in the remaining Phase 2.y slices),
and any Robot-driven capture (D40 keeps Robot a thin fidelity cross-check).
(Cross-ref D7/D24/D36/D37/D39/D40/D41/D42/D43.)

**Validation.** Every line reference in the design note was read directly from the current
`Thinlet.java` during the survey; the design is validated empirically when the first
goldens are recorded (next slice).

## D46 — `main` branch protection enforced: PR-only + required checks (closes the D43 follow-up)

**Date:** 2026-07-08. **Status:** accepted. **Phase:** 3 (infrastructure).

**Context.** D43 flagged a follow-up: confirm branch protection actually *requires* the
gating CI checks — a server-side setting invisible in-repo. Inspection (2026-07-08, via the
GitHub API) found `main` had **no protection at all**: the "direct pushes are blocked"
statements in `CLAUDE.md`/D42 described the Claude-web sandbox's git-proxy behavior, not
GitHub enforcement. Nothing prevented a direct push to `main` or merging a red PR, and
GitHub refused to *arm* auto-merge on pending-check PRs ("Protected branch rules not
configured") — which is why PRs #44/#45 were merged manually after green.

**Decision (maintainer-configured in the GitHub UI; verified via API).** Ruleset
**`protect-main`** — active, targeting the default branch:

- **Require a pull request** before merging, **0 approvals** (solo-maintainer flow — the
  point is forcing every change through the CI net, not review ceremony).
- **Required status checks** = the five gating jobs, exact contexts:
  `build (Maven JDK 21 / target Java 8) (21)`, `tests (JDK 8 via toolchains)`,
  `tests (JDK 11 via toolchains)`, `tests (JDK 17 via toolchains)`,
  `API compatibility (japicmp vs v0.1.0)`. `Cross-JDK trace diff (informational)` is
  deliberately **not** required, preserving D33's non-gating design.
- **Block force pushes** and **block deletion**. `strict_required_status_checks_policy` is
  off (no up-to-date-rebase churn in a serial one-PR-at-a-time flow).

**Effect.** Red PRs are unmergeable by anyone; direct pushes to `main` are refused (the
`CLAUDE.md` claim is now enforced server-side); and `gh pr merge --auto --squash` arms
while checks are still pending, making delegated squash-on-green fully unattended (the D42
opt-in delegation + the #43 allow rule + this ruleset are the three pieces of "full auto").

**Validation.** Effective rules on `main` read back via
`/repos/…/rules/branches/main` match the list above exactly. The PR landing this entry is
the end-to-end test: auto-merge was armed while its checks were pending and GitHub
performed the squash on green without manual action. (Cross-ref D33/D42/D43.)

## D47 — First interaction-state goldens: capture harness + 10 scenarios (validates D45)

**Date:** 2026-07-09. **Status:** accepted. **Phase:** 3 (Cut 2 prerequisite, first slice).

**Context.** D45 fixed the determinism design for interaction-state paint goldens; this
slice builds the capture and records the first goldens, closing the "interaction-state
paint is untraced" blind spot for the Cut 2 pilot widgets.

**Decision.**

1. **Held-state gestures** (test scope, `InputDriver`): `hover`/`hoverAt` (a bare
   `MOUSE_MOVED`, held) and `pressAndHold` (`MOUSE_PRESSED` without release). Each scenario
   uses a fresh driver, so an un-released press never leaks between tests.
2. **Scenario registry + tests.** `InteractionScenarios` maps each golden to a fixture +
   gesture script; `GoldenInteractionTraceTest` replays and compares at the D7 ±2 px
   tolerance (plus an orphan-golden hygiene check); `GoldenInteractionRecordMode`
   (re)writes goldens under `-Dtrace.record=true` — the D24 lifecycle. Recording is run
   **inside the CI container image (D44)** so pinned fonts match what CI compares, and
   **scoped** (`-Dtest=GoldenInteractionRecordMode`) so the static-corpus record mode is
   never co-triggered and cannot rewrite the 41 static goldens.
3. **Golden layout.** `trace/interaction/<fixture>-<scenario>.json`;
   `GoldenTraceRecorder.collectFiles` skips `interaction/` — those goldens map to
   scenarios, not corpus XML (`corpusResourceFor` would resolve them to nonexistent files).
4. **Ten scenarios:** button hover / press; checkbox hover / press (with the pressed
   check-preview) / focus-toggle; empty-field caret; field selection (`type` +
   Shift+Home over a 5-char word); textarea two-line caret; focused list selected+lead
   row; open combolist with the lead moved. **Scenario discipline (D45/D41):** carets are
   keyboard-placed, never pixel-aimed (a pixel-aimed caret lands on a FontMetrics-dependent
   character index); aiming is bounds-based; selection text stays short.

**Validation.** The hover and press goldens differ exactly at the `c_hover`/`c_press` tint
(`#EDEDED` vs `#B9B9B9`) — the held state provably reaches paint (a broken capture would
have recorded the static render twice). Full container verify: **100 tests green** (was
89). Cross-JDK rows via `local-ci.sh`: **JDK 8/11/17 all green** — the goldens hold within
±2 px across runtimes, including the FontMetrics-sensitive selection-highlight and caret
scenarios, empirically validating D45 ahead of CI.

**Scope / non-goals.** Test-scope code + goldens only — no `Thinlet.java` change, no
static-golden re-record, no tolerance change. Remaining scenarios (scrollbar/spinbox
arrows, tab hover, menubar, tooltip) follow in later slices as their fixtures land
(Phase 2.y). (Cross-ref D7/D24/D37/D41/D44/D45.)

## D48 — Fork shape revealed (multi-file decompositions): plan validated, three refinements; Cut 2 opens with the paint-write hoists

**Date:** 2026-07-09. **Status:** accepted. **Phase:** 3 (Cut 2 opening).

**Context.** The maintainer clarified that the two production forks are **not** single-file
`Thinlet.java` edits but **multi-file decompositions** separating layers (paint,
layout-inducing actions, …), built with two decoupling styles: Fork A made methods
`public static` so utility libraries call toolkit functionality without a subclassed
Thinlet instance; Fork B added explicit `Thinlet` instance parameters to new methods.
Sources + the apps built on them arrive the week of 2026-07-13. Assessment requested:
plan change or blocker?

**Assessment: not a blocker — convergent validation.** The maintainer already decomposed
Thinlet by layer in production, along the same boundaries as the D42 cut structure; the
"clean-architecting the wrong seams" risk (D43) *shrinks* with this information. Behavior +
API stay frozen through 3a regardless. Three refinements:

1. **Seam style for Cuts 2–4: stateless, explicit-context extraction.** Both fork styles
   point the same way — decouple behavior from the God-object instance. Extracted classes
   hold no state and receive everything explicitly (`render(Thinlet t, Object component,
   Graphics g, …)`), making either fork ergonomic (public-static or instance-parameter) a
   thin 3c wrapper later. Package-private through 3a per D43; any public surface is a
   3c/new-API decision.
2. **The fork task is a *mapping*, not a file diff** (a diff against one 2005 file is
   impossible against a multi-file fork): map fork files → subsystems; compare the
   battle-tested split boundaries against the Cut 2–6 seams; extract the functional
   enhancement backlog; and read the set of successfully-static-ified methods as an
   empirical **state-coupling map** of Thinlet. Runs on arrival, before the Cut 4/5/6 seam
   commitments. The apps join as the 3b test beds.
3. **Cut 2 proceeds now** — the seam-style question the early fork review was meant to
   answer is answered by the fork shape itself; waiting the week buys Cut 2 nothing.

**Correction to the D42 brief ("relocate the two stray paint writes"): hoist, don't
relocate.** Relocating the writes *in time* changes observable behavior: e.g. assigning
`:lead` at focus-gain instead of paint time flips a race — a Down key processed before the
async focus repaint sees a null lead in 2005 (selects the first item), but a pre-assigned
lead (selects the second). The correct, behavior-preserving move is extracting each
mutation into a named method invoked at the **identical** point in the paint sequence.

**Decision (landed this slice).** In `Thinlet.java`: the lazy-layout kick (paint entry;
negative-width dirty flag → `doLayout`) hoisted into `layoutIfDirty(component, bounds)`,
and the `:lead` adoption (list/table/tree item loop) hoisted into
`ensureLeadForPaint(component, focus)` called once before the loop — semantically identical
because the original write could fire only on the first iteration, and nothing between the
two points touches `:lead`. The recursive paint's own text is now mutation-free, the
precondition for a read-only Renderer extraction (Cut 2 pilot: label + button, next slice).

**Validation.** Full net in the CI container (D44): 100 tests green — including the D47
`list-selected-lead-focus` interaction golden that guards the `:lead` write — and crossjdk
rows 8/11/17 green; zero diff on all four runtimes. japicmp unaffected (new members are
default-visibility). (Cross-ref D42/D43/D44/D45/D47.)

## D49 — Cut 2 pilot: label + button branches extracted to `Renderer` (first product-source decomposition)

**Date:** 2026-07-09. **Status:** accepted. **Phase:** 3 (Cut 2 pilot).

**Context.** With the paint-side mutations hoisted (D48), the recursive paint became
read-only in its own text — the precondition for lifting widget branches out. This slice is
the D42 "typed Renderer" pilot: prove the extraction pattern on the simplest,
best-guarded branches before scaling it across the ~30 widget classes.

**Decision.**

1. **`thinlet/Renderer.java`** — the first product source file added beside the 2005 pair
   (`Thinlet.java`, `FrameLauncher.java`). Package-private, `final`, stateless; static
   methods in the D48 explicit-context seam style: `label(Thinlet t, Object component,
   Rectangle bounds, Graphics g, clip…, enabled)` and `button(…, classname, …, pressed,
   inside, focus, enabled)`. The bodies are the 2005 paint branches **moved verbatim**
   (comments included, e.g. the commented-out default-button fragment). Dispatch — the
   classname chain — stays in `Thinlet.paint`.
2. **Three visibility widenings** (`private` → package-private; japicmp-invisible;
   commented at the site): the 22-arg icon+text paint dispatcher, `static get(Object,
   Object)`, and `getBoolean(Object, String, boolean)`. This is the expected mechanical
   cost of decomposition under the D43 single-package discipline, and mirrors what the
   maintainer's Fork B did (instance/context passed to relocated methods).
3. **License/attribution:** the new file's header derives from `Thinlet.java`'s LGPL
   header and keeps the Bajzat copyright — the method bodies are his 2005 code relocated
   (D3: no fresh copyright claimed).

**Validation.** Full net in the CI container (D44): **100 tests green**, and crossjdk rows
**8/11/17 green — zero diff**. The moved branches are exercised by nearly all 41 static
goldens plus the D47 button hover/press and checkbox/focus goldens, so the run is a genuine
equivalence proof for both the static and interaction-state paint of the moved widgets.
Checkstyle 0, SpotBugs 0; japicmp unaffected.

**Scope / non-goals.** Two widget branches only — the pattern, not the migration. The
remaining branches follow the same shape slice by slice; branches whose interaction paint
is not yet golden-guarded (scrollbar/spinbox arrows, tabs, menubar — see D47 remaining
scenarios) get their goldens **before** their extraction slice, per "net before refactor".
(Cross-ref D3/D42/D43/D44/D47/D48.)

## D50 — First scheduled independent self-review (Opus): decisions hold, four guardrails adopted

**Date:** 2026-07-09. **Status:** accepted. **Phase:** 3 (Cut 2, ongoing).

**Context.** The maintainer granted a standing lull-time workflow: when a work package
completes with no concerns and no response, continue with the next recorded step, and at
lulls run a **self-review on an independent model (Opus, not the session model)** with
permission to document the outcome, open a PR, and merge it. The first such review audited
D42–D49 plus the checkbox slice (PR #50) adversarially, from the committed artifacts
(full report: `.claude/SELF-REVIEWS.md`).

**Outcome.** All six audited choices **hold**; **no change** to the cut order, the
3a/3b/3c staging, or the fork-mapping gate before the Cut 4/5/6 seams. Four guardrails
adopted — all refinements *within* the existing plan:

1. **Shared-helper gate.** `paintScroll`/`paintArrow` (and any shared paint helper)
   carry unguarded transient states (scrollbar/spinbox arrow hover+press, tab hover,
   menubar). They stay in `Thinlet` — called via the explicit `t.` context — until those
   interaction goldens land. Upcoming field/textarea/list slices move only their own
   branch plus the already-guarded `paintField`; a slice must not smuggle a shared
   helper out.
2. **Combobox is *partially* guarded**, not unguarded: `combobox-open-lead` covers the
   open popup + lead highlight, but the arrow/body hover+press transients and the
   editable-field caret path are uncaptured. Its extraction slice waits for those
   goldens (charter blind-spot list corrected).
3. **Paint goldens cannot discriminate hoist-vs-relocate.** A held-state golden sets the
   transient tuple explicitly, so a paint-side write hoisted (D48) and one relocated to
   an earlier event produce identical traces — the D48 correction rested on source
   reasoning, not the net. Adopted: "hoist, don't relocate" stays a review-enforced
   invariant, **and** the `:lead` Down-before-repaint race is now pinned by two input
   tests (`InputListTest`): Down with a never-painted focused list selects the *first*
   item (null lead at keypress); Down after a focused paint selects the *second* (paint
   adopted the lead). Relocating the write to focus-gain fails the first; dropping the
   paint-time adoption fails the second.
4. **3a-closing checklist item.** Before 3a closes, re-narrow any package-private member
   the decomposition widened but no longer uses, so the later subpackage split inherits
   no phantom surface (follow-up to the D49/PR-#50 widenings, which are all currently
   consumed or scheduled).

Also recorded (review precision notes): D43's "dead-code-eliminated" phrasing is
imprecise — `STRICT_INTERN` is a runtime `final` (a `Boolean.getBoolean` call, not a
compile-time constant), so the strict branch is runtime-gated; the behavioral guarantee
(byte-identical to `==` when unset) is unaffected. The net's structural blind spots were
inventoried (repaint timing/ordering, tooltip, remaining transient states, unasserted
input paths, JDK 25+, serialization form) and match the charter's blind-spot list; the
upcoming field/textarea/list slices touch none of them beyond guardrail 1.
(Cross-ref D42/D43/D44/D45/D47/D48/D49.)

## D51 — Scrollbar/spinbox arrow goldens via no-op presses (auto-repeat neutralized by construction)

**Date:** 2026-07-09. **Status:** accepted. **Phase:** 2.y interleaved with 3 (net-strengthening).

**Context.** D47 deferred the scrollbar/spinbox arrow hover+press scenarios, and D50's
shared-helper gate blocks extracting `paintScroll`/`paintArrow` until they are guarded.
The blocker was a determinism question: unlike every D45/D47 state, **pressing** a
scroll/spin arrow arms the auto-repeat timer (300/375 ms), so a held press mutates the
model on a wall-clock schedule — a plain held-state capture would be racy.

**Decision — no-op presses.** Press captures aim at an arrow whose action is impossible:
the scroll view already at that extreme, the spinbox already at its bound. At source, both
`processScroll` (clamped delta → `return false` *before any model write*) and
`processSpin` (bound check fails → `return false`) then never reach `setTimer(...)` — the
timer is **never armed**, and the pressed tint still renders because
`mousepressed`/`pressedpart` are set by the press handling regardless of the action's
success. The held frame is time-independent by construction, not by winning a race.
Hover holds never arm the timer and need no trick.

**Landed.** `InputDriver.pressAndHoldAt(widget, x, y)` (part-aimed press);
`input/arrows.xml` (a horizontally-overflowing list + two spinboxes pinned at
`maximum`/`minimum` — set explicitly, since `processSpin`'s *model* defaults are
`Integer.MIN/MAX_VALUE`, not the DTD's 0/100); nine new goldens: vertical scrollbar
up/down hover + up-press-at-top + down-press-at-bottom (wheel-overshoot, clamped to the
exact bottom on every JDK — the golden draws Row-59 and not Row-00), horizontal left
hover/press-at-left, spinbox up hover + up-press-at-max + down-press-at-min. Aim points
derive from the `:vertical`/`:horizontal` part rectangles and widget size (bounds-based,
D41 discipline). **19 interaction goldens green on JDK 8/11/17/21** in the CI container;
the re-record also rewrote the 10 D47 goldens **byte-identical** (an incidental
determinism re-proof).

**Deliberate omissions** (recorded, not silent): the horizontal *right* arrow (tint gate
is the symmetric code path of "left"); pressed states of *actionable* arrows (auto-repeat
in flight — same deferral class as the tooltip, D45); knob-drag visuals. **Gate effect
(D50 g1):** `paintScroll`/`paintArrow`'s own transient states (scrollbar + spinbox
arrows) are now guarded, so those helpers are extraction-eligible; the tabbedpane and
menubar *branches* stay gated on their own goldens, and the combobox branch on D50 g2.
(Cross-ref D45/D47/D50.)

## D52 — Second self-review (Opus): one real regression caught (`"t.font"`), fixed + net-guarded

**Date:** 2026-07-09. **Status:** accepted. **Phase:** 3 (Cut 2, paint-branch extraction complete).

**Context.** With every widget paint branch extracted to `Renderer` (#48–#66; only the
tooltip-coupled `desktop` branch left in `Thinlet`), a scheduled independent-model review
(Opus, per the 2026-07-09 grant) audited the mechanical stretch for what a zero-diff net
*cannot* see: phantom package-private surface, over-eager regex substitution, attribution,
and next-phase readiness (full report: `.claude/SELF-REVIEWS.md`, Review 2).

**Finding — one real behavioral regression.** The Package B extraction (#57) used a
python text-scanner that blanket-prefixed field names; its `\bfont\b → t.font` rule
over-reached into a **string-literal key** in the port-content painter's textarea path:
`get(component, "font")` became `get(component, "t.font")`. Since `"t.font"` is never a
stored attribute, a textarea carrying a per-widget `font` attribute silently rendered in
the default font (its paint-time `setFont` skipped) — a genuine deviation from 2005.

**Why the net missed it (the net gap).** The only custom-font textareas in the 41-golden
corpus (`drafts/looks.xml`, `drafts/widgets.xml`) all sit on **non-selected tabs**, and
`Renderer.tabbedpane` paints only the selected tab's content — so the textarea-custom-font
path was never exercised by any golden. Exactly the class of defect a behavior-preservation
net is blind to: it proves *what is painted* stays identical, not that *every path* is
painted.

**Resolution (this PR).**
1. **Fix:** `Renderer.java` line 722 restored to the verbatim 2005 key `"font"` (plus a
   cosmetic comment revert `t.font`→`font`). Mandatory on verbatim-fidelity grounds alone
   (D3/D49): the 2005 original and every sibling read `"font"`.
2. **Guard:** new `input/fonttext.xml` (a standalone `font="24"` textarea — a point-size
   change, not `bold`, so the difference clears the D7 ±2 px gate and shows as a
   categorical `setFont` op) + the `textarea-custom-font` interaction scenario. **Proven
   to guard:** recorded on the fixed code, the golden *fails* when the key is re-broken
   (clean-compiled) and passes when fixed — the net now catches this regression.
   Determinism caveat learned: `font="bold"` is too weak a signal (bold-vs-plain metrics
   fall within ±2 px); a size change is required.
3. **Other review items — clean.** All 19 package-private widenings are referenced by
   `Renderer` (no phantom surface, D50 g4 holds); the other moved methods
   (`content`/`container`/`tabbedpane`/`popup`) are literal-faithful; the LGPL attribution
   is coherent. Next-phase note: folding the classname dispatch needs three more identical
   widenings (`mouseinside`/`focusowner`/`focusinside`) and cannot be *fully* stateless
   until the deferred tooltip path is addressed — no blocker.

**Process lesson (recorded).** The blanket-regex extraction recipe is fast but can corrupt
string/char literals; future mechanical moves must diff literal sequences (or exclude
quoted spans from field-prefix substitution). The independent review — not the net — is
what caught this; it justifies the standing lull-time self-review. (Cross-ref
D3/D49/D50; net-gap class also noted against the D45 survey.)

## D53 — Corpus-driven interaction goldens: paint the tab/tree blind spots (close the D52 class)

**Date:** 2026-07-10. **Status:** accepted. **Phase:** 2.y net-strengthening (interleaved with 3).

**Context.** D52 was a real regression the static golden net could not see: a
`font="bold"` textarea on a **non-selected tab** is never painted (a tabbedpane paints
only its selected tab), so a corrupted paint path went uncaught. The general gap: the net
proves *what is painted* stays identical, but is blind to paint code reachable only after
interaction — content on non-selected tabs, inside collapsed trees, behind closed popups.
The maintainer proposed driving the drafts demo (click non-default tabs, expand collapsed
trees) to paint those paths and capture goldens over them.

**Decision — reuse the vendored corpus as interaction fixtures.** The drafts demo's page
content *is* the vendored `corpus/{drafts,demo}/*.xml`, which already renders
deterministically through the stub `CorpusHandler`. So the interaction harness now drives
those corpus files (read-only; unmodified, D9/D12), selecting non-default tabs and
expanding collapsed nodes, then capturing held-state paint goldens — the same D45/D47
artifact class, just reaching interaction-revealed content.

**Distinguish from D37.** D37 rejected "driving the vendored corpus through input" — but
for the *getter-assertion* input net, reason: "the corpus asserts nothing about input."
This is *paint-trace* capture on the interaction net, purely to reach unpainted code, and
the exact gap D52 proved real. D37 does not foreclose it.

**Determinism basis.** `CorpusHandler` stubs all demo action/init methods (no dynamic
content), there is no timer-coupled state in these frames (D45), and gestures leave a held
state before a single paint. Proven: the 14 tab + 2 tree goldens re-record byte-identical
and pass cross-JDK 8/11/17/21; the record pass left all pre-existing goldens byte-identical.

**Landed (PRs #70–#72).**
- *Bridge:* `Scenario` gains a per-scenario handler factory (corpus scenarios pass
  `CorpusHandler::new`; the minimal `InputHandler` would throw, since Thinlet resolves the
  corpus's bound methods at parse time); `InputDriver.root()` + `first(classname)` (DFS)
  reach unnamed containers. No `Thinlet` change, no new gesture (`click(tab)` already
  selects; `arrowRight` already expands).
- *Coverage:* 16 corpus scenarios — `looks`/`widgets`/`demo`/`tabbedpane`/`eventlogger`
  non-default tabs (list/tree/table, menubar/popup, splitpane, `font="bold"`, sliders/
  spinboxes), plus `demo` "Tree node C" and `drafts` "System" node expansion.
- *Proof it closes D52:* re-breaking the `"font"`→`"t.font"` key (clean compile) fails
  `corpus-looks-tab2` — the corpus method catches the regression class on real content.

**Deliberately deferred (opt-in, needs its own step).** The **live-`Drafts` app
playthrough** — navigating the nav tree into pages (System→Colors) — requires extracting
`InputDriver` into the `thinlet-testkit` module D37 deferred (its second consumer) and an
**allowlist** of pages proven deterministic across the JDK matrix (excluding
`SystemProperties`/`FolderBrowser`/`Choosers`/`DesktopProperties`/etc., which read the
system/filesystem/locale). Corpus-driven scenarios can expand a nav node's child rows but
cannot follow a click into a page (the navigation handler is stubbed). (Cross-ref
D37/D45/D47/D52.)

## D54 — Restore the 2005 icon assets; re-baseline the icon-bearing goldens (fidelity fix)

**Date:** 2026-07-10. **Status:** accepted. **Phase:** 2.y net-strengthening (interleaved with 3).

**Context.** The vendored corpus (D9) references **25 distinct** icons as
`icon="/icon/<name>.gif"` — 42 XML scenes, ~300 references — but the GIFs themselves were
never vendored. `Thinlet.getIcon` (`Thinlet.java:6212-6249`) resolves them via
`getClass().getResource(...)` and swallows every miss in empty `catch (Throwable e) {}`
blocks: no log, no throw, returns `null` (now KNOWN-QUIRKS **Q3**). A null icon contributes
width/height = 0 to layout and emits **zero** `drawImage` calls (every paint site is guarded
`if (icon != null)`). So every golden trace was captured with all icons blank, and the icon
paint/layout path was exercised by **no** golden — a silent failure hiding a real coverage
hole.

**Fidelity framing — this restores 2005 behavior, it does not change it.** In 2005 the icons
shipped on the classpath inside the demo/draft jars (`amazon.jar`/`demo.jar`/`drafts.jar` in
the archive `thinlet-2005-03-28/lib/`), so the faithful 2005 baseline **has** icons; the
no-icon goldens were the *infidelity* (an accident of not vendoring those jars), not a
deliberate choice. This is distinct from D9/D12 "don't modify the vendored corpus": no corpus
XML is edited — we supply the resources the XML always referenced.

**Decision.**
- **Vendor the authentic bytes.** 24 of the 25 GIFs, extracted byte-verbatim from the archive
  jars (archive commit `6ad9565`), sha256-provenanced in `project-docs/ICON-PROVENANCE.md`.
  Icons appearing in more than one jar are byte-identical across them (one canonical stream).
- **`volume.gif` is a genuine 2005 gap — left absent.** It is referenced once
  (`drafts/widgets.xml`, a table column header) but exists in **no** jar of **any** archive
  version; `drafts.jar` (its 2005 classpath) never shipped it, so that column was a silent-null
  in 2005 too. Preserving it icon-less is the faithful behavior; fabricating a substitute would
  be an infidelity. It is allowlisted in the guard test (below).
- **Placement (three classpath roots, full 24 in each, byte-identical):**
  `thinlet-core/src/test/resources/icon/` (drives the harness; test-scope, **not** in the
  published core jar — that stays `thinlet.dtd`-only), `thinlet-demos/src/main/resources/icon/`
  and `thinlet-drafts/src/main/resources/icon/` (runtime). `*.gif` marked `binary` in
  `.gitattributes`.
- **Guard against future silent failures.** New `thinlet.trace.CorpusResourceResolutionTest`
  (always-on, display-independent) sweeps every corpus XML for resource references and fails the
  build on any that does not resolve on the classpath (plus a decode check that each resolves to
  a real ≥1×1 image), exempting the documented `KNOWN_ABSENT_2005 = {/icon/volume.gif}`. This is
  test-only — it does **not** change the library's silent-null semantics.
- **Preserve + pin the library behavior.** The empty catches stay verbatim; the silent-null is
  quirk-locked as **Q3** (`thinlet.quirks.GetIconSilentNullQuirkTest`).

**Re-baseline (recorded in the CI container per D44, `clean` per D52).** Restoring the icons is
an out-of-D7-tolerance change (new `drawImage` calls + ~16px layout shifts), so the record modes
(`-DtraceRecord=true`) rewrote the icon-bearing goldens: **21 static** (amazon ×10, demo ×2,
drafts ×9) + **10 interaction** (`corpus-looks-*` ×5, `corpus-widgets-{three,fonts}`,
`corpus-demo-{texts,values,tree-expand}`). The other **20** static goldens and all `/input/*` +
`corpus-{eventlogger,tabbedpane}-*` + `corpus-drafts-tree-expand` interaction goldens came back
**byte-identical** — the icons are the only cause of change.

**Determinism basis.** All 24 icons are 16×16 with frame-independent intrinsic dimensions
(`loading.gif` is 4-frame animated, but the trace records only geometry, never pixels). Two
independent record runs produced **byte-identical** goldens (all 89), and
`GoldenTraceRegressionTest` passes **41/41 on JDK 8/11/17/21** against the new goldens — so the
GIF dims are decoder-stable and the re-baseline is cross-JDK-portable.

**Cross-ref** D7 (tolerance), D9/D12 (verbatim corpus — unchanged), D22 (Xvfb :99), D33
(cross-JDK), D44/D52 (record-in-container + `clean`), D45/D47/D53 (interaction goldens).

## D55 — Fold the classname dispatch chain into `Renderer.paint` (Cut 2 closes)

**Date:** 2026-07-11. **Status:** accepted. **Phase:** 3 (Cut 2, dispatch fold).

**Context.** Cut 2 extracted every widget paint branch to `Renderer` (#48–#67), but the
**dispatch itself** — `Thinlet.paint(Graphics, int×4, Object, boolean)`'s recursive
per-component body: visibility/bounds gate, clip-reject, translate, the
`if (is(classname, …))` ladder, un-translate — still lived in `Thinlet`, and `Renderer`'s
class doc pinned that as the contract ("dispatch stays in `Thinlet.paint`"). D52 forecast
this fold: three more D48 widenings, and not fully stateless until the tooltip path is
handled. This is the handoff's next-work item 1.

**Decision.**
- **`Renderer.paint(Thinlet t, Graphics g, int clipx, int clipy, int clipwidth,
  int clipheight, Object component, boolean enabled)`** now holds the full body, moved
  verbatim. **`Thinlet.paint` becomes a one-line shim** delegating to it, so all six
  existing call sites stay untouched: `Thinlet.paint(Graphics)` (top-level AWT entry),
  `paintReverse` (desktop z-order recursion), and the recursive child-paint calls already
  inside `Renderer` (`container`, `tabbedpane`, `splitpane` ×2, `spinbox`). Rewiring those
  to intra-class calls would be cosmetic; deferred.
- **`desktop` stays in `Thinlet` behind a callback.** Its body — the one net-invisible
  paint path (timer-coupled tooltip, D45) — was first hoisted (D48 hoist-don't-relocate)
  into a package-private `Thinlet.paintDesktop(…)` at the identical call point; the folded
  ladder calls `t.paintDesktop(…)`. `paintReverse`/`tooltipowner`/`content` stay private.
  Extraction waits for the tooltip capture (deferred, low priority).
- **`separator` and `bean` move with the ladder.** Reconciling D52's "only the
  tooltip-coupled `desktop` branch left" phrasing: these two trivial inline branches were
  not counted there. Both are stateless — no `pressed`/`inside`/`focus` use, only
  already-widened members (`c_border`/`c_disable`/`evm`/static `get`) — so the D50 gate
  (which targets *interaction-state* helpers) does not apply. Coverage: `<separator>`
  appears across the static-golden corpus; `<bean>` in `drafts/chart.xml`.
- **Widenings: exactly the three D52 forecast** — `mouseinside`, `focusowner`,
  `focusinside` — each with the standard seam comment. No method widenings were needed
  (`is`/`layoutIfDirty` already package-private; `getClass` public static).

**Mechanical discipline (per the D52 lessons).** Python move with boundary assertions;
token rewrites applied only *outside* string literals, never a blanket prefix regex; the
quoted-literal sequence of the moved region asserted byte-identical before/after
(30 literals). Compiler caught nothing residual (no stray `this`/unprefixed members).

**Verification gate.** A pure move: **zero golden diffs required** — the container net
(D44) on JDK 21 plus the crossjdk 8/11/17 rows must pass with `git status` clean (41
static + 48 interaction goldens byte-identical), plus the input suite. Any golden diff
would mean the move changed behavior and is a defect, never a re-baseline.

**Cross-ref** D42/D43 (Cut 2 charter), D45 (tooltip = net-invisible), D48 (seam style,
hoist, widening comment), D50 (guardrails), D52 (forecast + regex trap), D44 (container).

## D56 — Type the drawing vocabulary: `IconTextSpec` for the icon+text paint dispatcher

**Date:** 2026-07-13. **Status:** accepted. **Phase:** 3 (Cut 2 tail — "type the drawing
vocabulary").

**Context.** With the dispatch folded (D55), the remaining Cut 2 clause was typing the
drawing vocabulary. The wart: the icon+text `paint` dispatcher — **23 formals** (D49's
"22-arg" was off by one; pinned here as the sibling of the documented 11-arg-decoy trap) —
called positionally from **15 sites, all in `Renderer`**: box ×4 + `Graphics` + clip ×4 +
border edges ×4 + padding ×4 + `focus` + `char mode` + `String alignment` + `mnemonic` +
`underline`, plus the component. The handoff's "two 22-arg overloads" premise was wrong:
there is one 23-formal dispatcher that *delegates* border+background to the separate
**11-arg** overload (its first body line).

**Decision.**
- **New package-private `thinlet.IconTextSpec`** — the fork's first parameter-object
  class: fluent mutable spec (the builder is its own product; Java-8, one allocation per
  call). Constructor carries the required box + `char mode`; `clip`/`border`/`padding`/
  `focus`/`align`/`mnemonic`/`underline` are fluent with defaults (no borders, 0 padding,
  no focus, `"left"`, no mnemonic/underline). No getters — same-package field reads.
  **Rule: fresh instance at every call site, never cache or reuse one** — a reused mutable
  spec is the shared-state hazard the D48 style exists to avoid. D48 note: this is a
  transient *data carrier*, not a stateful subsystem — the "stateless, explicit-context"
  discipline governs behavior classes; the spec is compatible with it. japicmp-invisible
  (D43); new-code header (not Bajzat-attributed — nothing in the file is 2005 source).
- **Dispatcher re-signatured** to `paint(Object component, Graphics g, IconTextSpec s)`
  with an **unpack-prologue** (locals with the exact 2005 parameter names), so the method
  body below the prologue is **byte-identical 2005 code** — including its `alignment`
  reassignment and the 11-arg delegation. No shim: the dispatcher had zero
  Thinlet-internal callers.
- **Evaluation-order note.** The constructor evaluates `mode` *before* the clip/border/
  padding expressions that positionally preceded it. Safe because every argument at all
  15 sites is pure (`is`/`get`/`getBoolean`/`getString`/field reads/arithmetic) — the
  conversion script *asserted* this with a call allowlist; any future impure argument
  must not rely on group evaluation order.
- **Scope cut — recorded honestly:** `char mode` keeps the 2005 12-value char vocabulary
  (an enum would rewrite the verbatim `switch` bodies in both overloads); the 11-arg
  border overload, the 7-arg dialog-glyph `paint`, `paintRect` (25 sites) and `drawFocus`
  (8 sites) stay untyped. "Type the drawing vocabulary" is delivered for the wart, not
  the whole surface; the rest can follow the same recipe if wanted.

**Mechanical discipline.** 13 sites converted by script (paren-balanced top-level split,
arity-23 match, purity allowlist), 2 comment-bearing sites by hand (`// TODO disabled`
preserved). **Round-trip audit**: every emitted chain re-parsed and the 23-tuple
reconstructed (defaults applied for elided groups) and compared token-for-token against
the pre-conversion originals from git — **15/15 identical**, both before and after the
elision pass (55 literal-default fluent calls removed). This audit closes the
conversion-bug classes the golden net cannot see.

**Net gap closed first (new golden, no re-record).** `underline` is non-literal at exactly
one site (link button) and is drawn only while hovered; no golden hovered a link — an
underline regression was provably zero-diff. New `input/link.xml` + `link-button-hover`
interaction scenario, recorded in the CI container (D44), two runs byte-identical, the 48
existing goldens untouched (now 49). The golden's single `drawLine` is the underline.

**Verification.** Container net (D44): JDK-21 base row green (41 static + 50 interaction
tests + input suite; Checkstyle/SpotBugs/Spotless clean) after both the typing and the
elision commits, **zero golden diffs**; crossjdk rows 8/11/17 green on the final tree.

**Cross-ref** D42/D43 (Cut 2 charter, visibility), D44 (container), D45 (net-invisible
paths), D48 (seam style; parameter-object clarification above), D49 (the "22-arg" naming),
D52 (mechanical-move discipline), D55 (dispatch fold).

## D57 — Documentation policy: single-home facts, pinned in-source annotations; retire the code-explaining maps

**Date:** 2026-07-14. **Status:** accepted. **Phase:** 3 (process/documentation; no product behavior).

**Context.** After Cut 3 planning, the maintainer flagged the recurring per-session cost of
reconciling the doc surface, and supplied an external documentation-philosophy thread whose
lens was applied to the repo's ~3,900 markdown lines. Findings: the same fact was living in
three places (the D49 "22-arg" miscount survived precisely because it was recapped in
DECISIONS, NEXT-STEPS, *and* the PHASE-3-GOALS cuts table — D56 had to correct it in each);
`.claude/PAINT-PIPELINE-MAP.md` self-declared "stale on locations" (an out-of-source map of
code that moved — negative-value navigation); `.claude/FABLE-NEXT-STEPS.md` self-declared
transient, folded into D43 on 2026-07-07 and still present a week later. Meanwhile the
repo's primary reader of `Thinlet.java` is an agent re-reading sections cold every session:
spatially-indexed facts (comments at the point of use) beat date-indexed decision entries
for that reader, and the standard comment-rot objection is structurally weak here — the
2005 semantics are frozen by charter, so a comment on a verbatim body cannot rot from
under itself; only *locations* rot, which is exactly what killed the out-of-source map.

**Decision.**

1. **Single-home rule.** Every fact has exactly one home: rationale/evidence (including
   alternatives considered and verification results) → `DECISIONS.md`;
   charter/invariants → `project-docs/PHASE-3-GOALS.md`; current state →
   `.claude/NEXT-STEPS.md`; behavior contracts → `KNOWN-QUIRKS.md` + sentence-named
   tests. Everything else cross-references by D-number/test name — never recaps.
2. **In-source annotation layer** over the frozen 2005 core, governed by three comment
   rules: **(a) pin-or-tag** — a comment states only facts mechanically checkable in the
   code directly beneath it, facts pinned by a named test (cite it, e.g. `// pinned:
   DescriptorContractTest`), or hypotheses explicitly tagged `// UNVERIFIED:`;
   **(b) fact-density, not narrative** — terse schema/invariant blocks, vocabularies not
   counts, no essays (an agent re-reading pays per token); **(c) names, not locations** —
   grep-stable member names only, never line numbers or cross-file location claims (the
   one thing that still rots across extractions). Growth is **evidence-gated**: each cut
   annotates what its tests just proved, rather than a big-bang annotation PR writing
   ~100 unpinned claims at once. First anchor landed with this entry: the widget-model
   schema + reserved `:`-key vocabulary above `createImpl` (pinned by
   `DescriptorContractTest` and the golden net).
3. **New files:** license header + ≤3-line class doc + a `DECISIONS.md D<n>` pointer; no
   design-narrating javadoc (typed field/method names are the documentation). Existing
   multi-paragraph javadoc (`Renderer`, `IconTextSpec`, `is()`) is trimmed
   opportunistically when a PR already touches the file — comments are
   bytecode-invisible (goldens, japicmp, and the tripwire are all indifferent), so
   trimming carries zero behavior risk; no churn PR.
4. **Retire the code-explaining reference docs.** `PAINT-PIPELINE-MAP.md` deleted — the
   decomposition made the code the map (`Renderer.java`'s javadoc carries the pipeline
   shape; `TracingGraphics2D`'s recorded overrides *are* the drawing vocabulary; the
   model schema moved in-source per Decision 2). `FABLE-NEXT-STEPS.md` deleted (folded
   into D43); inbound references retargeted (PHASE-3-GOALS, `trace-curator.md`,
   two test javadocs, `CLAUDE.md`). Mentions inside prior D-entries stay — this log is
   append-only and its references are accurate as-of-writing. PHASE-3-GOALS cuts-table
   cells and NEXT-STEPS thinned to status + D-pointers; `CLAUDE.md` carries the
   operational summary (the auto-loaded file is where a rule must live to steer
   sessions).

**Scope / non-goals.** Markdown + comments only — no product-source semantics, no golden
re-record, no API change. Audit scripts/round-trip reports for mechanical cuts stay
uncommitted (scratchpad artifacts; results one line in the PR description, method in the
cut's D-entry).

**Validation.** Full container base row (D44) green after the edits; `git status` clean of
golden diffs (comments are bytecode-invisible). japicmp untouched (no signature changes).
(Cross-ref D27 doc layout, D38 filenames, D42/D43 charter + visibility, D49/D56 the
miscount that motivated single-home, D53 tests-as-spec precedent.)

## D58 — Cut 3 core: the definition table and its consumers typed (`AttributeDescriptor`/`WidgetDescriptor`/`DescriptorTable`)

**Date:** 2026-07-14. **Status:** accepted. **Phase:** 3 (3a, Cut 3).

**Context.** Cut 3 (charter D42/D43: "DTD → typed descriptors + accessor-façade cleanup")
opened after the fork-sources check came back empty (expected ~2026-07-17/19; Cuts 1–3
don't depend on the fork shape). Net-before-refactor: `DescriptorContractTest` (#81)
pinned the lookup walk, defaults, storage asymmetries, canonicalization, and exact error
messages first. The in-code table was a flat triple-stride `Object[]`
(`{classname, parentClassname, Object[][] attrs}`; 35 widgets, 109 attribute rows, the 18
`"method"` rows 2-element) behind the private chokepoint `getDefinition`, consumed by 15
sites (14 callers + `finishParse`, which receives rows deferred through the parse-time
Vector).

**Decision (the typed design).** Three package-private classes in package `thinlet` (D43
visibility discipline; Java 8):

- **`AttributeDescriptor`** `{String type, String name, String invalidate, Object
  defaultValue}` — strictly the 2005 slot order `[0..3]`; a 2-arg `(type, name)`
  constructor maps the method rows (no 2005 code read `[2]`/`[3]` on them — one reachable
  exception below). `name` is the interning anchor: the canonical key object the model
  layer stores and compares by identity.
- **`WidgetDescriptor`** `{String name, String parent, AttributeDescriptor[] attributes}`
  — `parent` stays a **name** re-looked-up by identity each hop, not an object reference:
  this transliterates the 2005 walk exactly, preserving its quirk (a classname absent
  from the table loops forever — unreachable, `":class"` is always canonical via
  `create`; commented inline per D57, not "fixed", not test-pinned).
- **`DescriptorTable`** — the relocated 2005 data (Bajzat extraction header), typed
  `WIDGETS` built in a static block that keeps the 2005 initializer shape (shared locals,
  comments, `new Integer(...)` boxing) so the transform stays strictly positional.

**Why interning survives the move (the D43 tripwire question).** The canonical objects
are compile-time string literals; per JLS 3.10.5 same-content literals resolve to the
same interned object across classes and files, so moving them from `Thinlet`'s static
block into `DescriptorTable` yields the identical objects the `is(…)` sites, `Renderer`
reads, and the model's `entry[0] == key` compares already use. The three
re-canonicalization points are preserved structurally (`create` returns
`WIDGETS[i].name`; `addAttribute` re-keys via `definition.name`; setters store under
`definition.name`), and `Renderer`'s classname dispatch needs **zero re-keying** (the
model still stores `":class"` → interned String; typed widgets are Cut 5). Empirical:
the strict-intern tripwire (armed by D43 for exactly this cut) ran in every test JVM of
every row, green.

**`getDefinition` retyped in one step** (returns `AttributeDescriptor`; it is private, so
japicmp-free): no transitional `Object[]` — the compiler becomes the completeness check
(any surviving `definition[n]` is a compile error). `update(Object component, Object
mode)` narrowed to `String mode` in the same commit, so a
`.name`/`.invalidate`/`.defaultValue` transposition at the nine
`update(component, definition.invalidate)` sites cannot compile.

**Commit-split deviation from the plan.** The planned move → type-table → convert split
collapsed to move → (type + convert): a typed table cannot compile behind an
`Object[]`-returning `getDefinition` without bridge scaffolding that would rebuild the
rows at init (new object identities, twice-changed runtime structures — worse than the
bigger commit). Commit 1 (verbatim move, 10 reads retargeted) still isolated the
interning question for the tripwire before any typing.

**One recorded divergence (exception type on a malformed-input path).** A method-binding
argument naming a **method-type** attribute (e.g. `setMethod(b, "action",
"doIt(this.action)", …)` or the same via parse) threw
`ArrayIndexOutOfBoundsException` in 2005 — `definition[3]` on the 2-element row — before
`getMethod`'s type ladder could reject it. The typed row has no out-of-bounds to hit:
`defaultValue` reads `null` and the ladder throws `IllegalArgumentException("method")`.
Reachable only from malformed binding strings; still a crash-on-error, different class.
Accepted rather than fabricating an AIOOBE; pinned by
`methodTypedBindingParameterIsRejectedWithTheTypeToken` (added in the same PR). This is
the cut's only known observable change.

**Scope cuts (recorded, D56-mirroring).** Type tokens and invalidate tokens stay interned
`String`s — **no enums**: enum-ification would rewrite three verbatim `is(…)` ladders
(`addAttribute`, `update` — also fed by non-DTD `"validate"` literal sites — and
`getMethod`) for zero net-strength gain; revisit at Cut 5. `defaultValue` stays one
`Object` slot holding the allowed-values `String[]` for choice rows (slot-faithful; the
three `(String[])` casts stay). No new raw `getFont`/`getComponent` overloads; `Renderer`
untouched (this cut touches no paint code — that is what makes goldens-zero-diff
trivially arguable).

**Mechanical discipline (D52/D56 recipe).** Move audit: old-vs-new table parsed to
normalized token trees — 35 widgets + 109 rows + shared locals identical. Typing
transform scripted (comment-preserving text surgery); same token audit against the
committed legacy table — identical. Consumer conversion scripted with per-pattern count
assertions (13 `Object[] definition =` decls; 52 `definition[N]` index expressions
{16, 16, 9, 11} mapped positionally digit→field — a transposition is impossible by
construction; 7 now-redundant `(String)` casts dropped; the singleton edits by
exact-substring match asserting count 1). A first-run assertion failure (cast count 10
vs actual 7) aborted before writing — the file is written only after every assertion
holds. Audit scripts stay uncommitted (D57); method recorded here.

**Validation.** Container (D44): base row **171 tests, 0 failures** (41 static + 50
interaction golden tests + input suite + robot + tripwire + 25 contract pins), **zero
golden diffs** (`git status` clean post-run); crossjdk rows 8/11/17 green (168 each,
robot excluded). japicmp trivially green: new types package-private, every changed
method private, public methods body-only. (Cross-ref D42/D43 charter + tripwire, D44
container loop, D48 seam style, D52/D56 mechanical discipline, D57 documentation policy,
PR #81 net, this PR.)

## D59 — Cut 3 closes: accessor-façade cleanup (the dead default-parameter helper inlined)

**Date:** 2026-07-14. **Status:** accepted. **Phase:** 3 (3a, Cut 3 close).

**Context.** The last Cut 3 item after D58: the private 4-arg
`setString(component, key, value, defaultvalue)` whose `defaultvalue` parameter was dead
2005 code (`return set(component, key, value); // use defaultvalue`) — the reason string
setters, unlike boolean/integer, never omit-at-default (pinned by
`DescriptorContractTest` before any of this moved).

**Decision.** **Inline the helper away** rather than drop the parameter: removing the
4th parameter would collide with the public `setString(Object, String, String)` overload
(same erasure, different return type). Its callers become direct model writes —
behavior-identical since the helper ignored the parameter:

- public `setString` → `set(component, definition.name, value)`;
- `addAttribute`'s string branch → `set(component, key, value)` (`key` already
  re-canonicalized);
- `processSpin` → `set(component, "text", value)` — a **third caller** the plan's
  inventory missed (it passed a literal `null` default, so the
  `definition.defaultValue`-shaped greps never saw it); the compiler surfaced it on the
  first build, which is the point of doing such removals compile-gated.

The storage-asymmetry contract now lives as a fact-dense comment at the raw-setter
cluster (D57 rules; cites the pinning test): boolean/integer setters remove the entry at
the declared default; string setters always store; choice stores the default on null;
parse stores integers even at default but omits booleans at theirs.

**Cut 3 is closed.** Scope cuts and the one recorded divergence are in D58; non-goals
held: no public/protected signature change (japicmp green), `Renderer` untouched, no
token enums, no new raw overloads. The D50 closing-checklist note stands for 3a's end
(re-narrow unused widenings before the subpackage split) — nothing to re-narrow from
this cut: no visibility was widened.

**Validation.** Container base row green (**171 tests, 0 failures** — goldens + input
suite + robot + tripwire + the 25 contract pins, spinbox input tests exercising the
inlined `processSpin` path), zero golden diffs; crossjdk rows 8/11/17 green. (Cross-ref
D42/D43 charter, D57 comment rules, D58 core, PR #81 net.)

## D60 — Pre-PR Java comment pass, hook-enforced (`scripts/comment-pass.sh`)

**Date:** 2026-07-14. **Status:** accepted. **Phase:** 3 (process; workflow tooling only).

**Context.** After Cut 3 the maintainer asked for an automated guarantee that a "Java
comment update" pass happens before every PR. The D57 rule already assigns *what* the
pass checks, but its trigger was recall — and the failure mode is real: PRs #83/#84
touched `Thinlet.java` without trimming the pre-D57 `is()` javadoc, exactly the
opportunistic trim D57 prescribes. Recall does not survive sessions; a harness hook
does (the harness executes hooks, not the model).

**Decision.** A Claude Code `PreToolUse` hook on `Bash` (repo `.claude/settings.json`)
runs `scripts/comment-pass.sh hook` on every shell call. It denies `gh pr create` if and
only if (a) the command contains `gh pr create`, (b) the branch's diff vs `main`
contains `*.java` changes, and (c) no attestation marker matches the current HEAD SHA.
The pass itself stays a judgment task: `scripts/comment-pass.sh` prints the D57-derived
checklist (comment only what code cannot say, pin-or-tag; fix staleness; trim pre-D57
verbose javadoc in touched files; new-file header rules) plus the changed Java files;
`scripts/comment-pass.sh done` attests by writing the HEAD SHA to
`.git/java-comment-pass` (untracked, per-clone). New commits invalidate the attestation
by construction. Docs-only PRs never hit the gate.

**Known edges (accepted).** The command match is a substring — a shell command merely
*mentioning* `gh pr create` on a Java-diff branch triggers the gate (cost: one
attestation); PRs created outside the harness (maintainer running `gh` directly)
bypass it (the gate targets Claude's workflow, which is where the forgetting happens).

**Validation.** All four hook paths pipe-tested with synthesized stdin (non-matching
command; matching + no Java diff; matching + Java diff + no marker → deny JSON with
the checklist pointer; attested → allow). Live-fire proven via a sentinel prefix on a
harmless command, then removed. `jq -e` schema check green. (Cross-ref D42/D46 PR
workflow, D57 comment rules, D59 the near-miss.)

## D61 — Layout-state sidecar goldens: `:port`/`:view`/`:widths`/`:offset` pinned before Cut 4

**Date:** 2026-07-14. **Status:** accepted. **Phase:** 3 (Cut 4 net prerequisite; test
scope only — zero `src/main` change).

**Context.** The chartered Cut 4 prerequisite (PHASE-3-GOALS net-strengthening list):
the layout half of a golden records only class + `bounds`, so the scroll/layout state
the Cut 4 refactor will move — `layoutScroll`'s `:port`/`:view`, `doLayout`'s table
`:widths`, `layoutField`'s `:offset` — was almost unpinned (only `:view` had two
direction assertions in `InputScrollTest`). Fork mapping (NEXT-STEPS item 1) stays
blocked on the maintainer's sources; this is net work, not a seam commitment.

**Decision — sidecar files, not a format change.** The four keys are recorded into
**new sidecar goldens** (`trace/layout-state/{demo,drafts,amazon,interaction}/…`,
document shape `{"layoutState": […]}`), leaving every committed `{calls, layout}`
golden byte-untouched. The rejected alternative — extending `LayoutNode`/`TraceJson`
in place — would force re-recording all 90 existing goldens: a baseline replacement
that could silently absorb sub-tolerance drift against the original record (the exact
failure mode the D44/D52 discipline exists to prevent).

- **Node shape:** `class, x, y, w, h` (bounds anchor) + sparse `:port`/`:view`
  (`[x,y,w,h]`), `:widths` (int[]), `:offset` (int; negative = alignment branch).
  A node is emitted if and only if the widget has bounds and ≥1 of the four keys.
  D7 model: presence/class/array-length categorical-exact, numbers ±`defaultPx`.
- **Traversal:** `LayoutStateTrace` follows `:comp`/`:next` **plus the `:combolist`/
  `:popup` attachment edges** — popups are inserted as siblings of the parsed root on
  the private desktop content chain, so they are unreachable through child links;
  the combolist `:port`/`:view` (the `popupCombo → layoutScroll` call site) is pinned
  via the held-open combobox scenarios. The existing `LayoutTrace.walk` output is
  untouched (its goldens depend on it).
- **Bidirectional regression** (`GoldenLayoutStateTraceTest`): a non-empty walk
  requires a matching sidecar, an empty walk forbids one; orphan check mirrors the
  interaction net; `allFourKeysExercised` is a **permanent coverage guard** (all four
  keys + a non-zero `:view` scroll + a positive `:offset` must stay exercised).
- **One new scenario** (`offset-field-scrolled`): the positive (scrolled) `:offset`
  branch had zero coverage — corpus alignment fields only produce the negative
  branch. A fixed-size 40×20 field (`getPreferredSize` honors `width`/`height` only
  when **both** are set — the first record attempt with `width` alone silently laid
  out at the 80px default and wrote no sidecar) overflowed by six typed chars gives
  `:offset` 18, decisively past any ±2 px drift.

**Recorded set (CI container, D44).** 58 sidecars / 184 state nodes (41 static
renders → 24 non-empty; 50 scenarios → 34 non-empty); the record run round-tripped
all 49 pre-existing interaction goldens byte-identically (porcelain gate: additions
only). Static sidecars see more than the paint net: `doLayout` lays out every tab's
content, so never-painted tabs still pin their `:port`/`:view`.

**Accepted residual gap.** No scenario moves a horizontal scrollbar, so a non-zero
`:view.x` specifically is unexercised (the coverage guard requires x-or-y). Optional
follow-on only if Cut 4 shows the need.

**Cross-JDK posture.** `:view` content extents accumulate font-derived row heights;
assessed low-risk (the scrolled-list paint golden already embeds
`contentheight − portheight` in bottom-row y-coords and is green on 8/11/17). An
over-tolerance sidecar diff on a crossjdk row is a finding to triage — never widen
`defaultPx`, never re-record to mask (D7/D35); options are the reserved `perOp` hook
or a D-referenced fixture allowlist. (Cross-ref D7 tolerance, D24 harness, D44/D52
golden discipline, D45/D47/D53 interaction net, PHASE-3-GOALS Cut 4.)

## D62 — Tooltip capture: the last D45-deferred interaction golden (timer absorbed by a bounded poll)

**Date:** 2026-07-14. **Status:** accepted. **Phase:** 3 (net; test scope only — zero
`src/main` change).

**Context.** D45 designed the interaction goldens as held-state captures with no time
dependence and deferred exactly one state as timer-coupled: the tooltip (the single
`timer` thread's 750 ms delay). That left `paintDesktop`'s tooltip overlay the one
net-invisible paint path, blocking the `paintDesktop`/`paintReverse` extraction
(the D48 hoist's javadoc says so in place).

**Decision — real timer, bounded poll; no synthetic hook.** The capture drives the
production path end to end: `hover` lands as a `MOUSE_MOVED` onto a fresh widget,
which `processEvent` turns into `MOUSE_ENTERED` + `setTimer(750L)`; `showTip` then
fires on Thinlet's timer thread and writes `:tooltipbounds`. The new
`InputDriver.awaitTooltip` polls that key (25 ms steps, 10 s deadline) — the **only**
nondeterminism is *when* the timer fires, which the poll absorbs; the shown frame is
a pure function of the scripted pointer position (tooltip x,y = mouse + 10, clamped
to the desktop) and the tooltip text. Text kept short (`"Tip"`) so the
FontMetrics-derived width/height sit inside the D7 ±2 px gate. The cross-thread
write/read (timer thread → test thread) is unsynchronized in the 2005 code; the
sleep-poll plus the post-poll paint re-read make it benign in practice.

**Cost accepted.** Every regression run of the scenario waits the real ~750 ms
(per JVM row). One scenario; not worth a test-only injection seam that would touch
`src/main` during 3a.

**Validation (container, D44).** `tooltip-shown` golden records the full overlay
(border + fill + `drawString "Tip"` at mouse+10 — the `paintDesktop` branch);
porcelain gate additions-only: all 50 pre-existing interaction goldens and 58 D61
sidecars round-tripped byte-identical (the tooltip fixture itself carries no
scroll state — its sidecar is correctly absent). Base row 268 tests green; crossjdk
8/11/17 green. **Every interaction state D45 enumerated is now guarded; the
`paintDesktop`/`paintReverse` extraction is unblocked** (the next Cut 2 close-out
slice). (Cross-ref D45 determinism design, D47/D51/D53 the other packages, D48/D50
hoist + shared-helper gates, D61 sidecars.)

## D63 — Cut 2 closes: `paintDesktop`/`paintReverse` move to `Renderer` behind the D62 golden

**Date:** 2026-07-14. **Status:** accepted. **Phase:** 3 (Cut 2 close-out).

**Context.** D48 hoisted the `desktop` paint branch but left its body in `Thinlet` —
the tooltip overlay it paints was the one net-invisible path (D45), and the hoist
javadoc pinned the condition in place: "extraction waits for the tooltip capture."
D62 landed that golden.

**Decision.** Pure code motion, D48 seam style: `Renderer.desktop` (the widget-name
convention) + private `Renderer.paintReverse`, both static with explicit `Thinlet t`
context; the dispatch's last `t.`-callback branch now calls the local method, and the
`Thinlet` bodies are deleted. `paintReverse`'s recursive `t.paint(g, …)` shim call
becomes the direct `Renderer.paint(t, g, …)` — the shim is a pure forward (D55), so
the dispatch behavior is identical. Widened on demand, commented per D48: `content`
and `tooltipowner` (private → package-private). With this, **every 2005 paint branch
body lives in `Renderer`**; what stays in `Thinlet` are the D50-gated shared helpers
(`paintRect`/`paintScroll`/`paintArrow`/icon-text) called through the `t.` context.

**Validation (container, D44).** Behavior-preserving by the net: base row 268 tests
green — including `tooltip-shown` (the moved overlay) and the 48 D53 corpus-driven
scenarios that exercise `paintReverse`'s clip-overlap recursion under open popups —
plus crossjdk 8/11/17 green, zero golden diffs, japicmp unchanged. (Cross-ref
D45/D48/D50/D55/D62.)

## D64 — Input characterization net for the Cut 6 surface (three slices; getter-less state via handler recording + chain-walk)

**Date:** 2026-07-14. **Status:** accepted. **Phase:** 3 (Cut 6 net prerequisite; test
scope only — zero `src/main` change except one driver gesture in slice B).

**Context.** PHASE-3-GOALS' last blind spot: "menus, spinner, tooltip, slider,
tabbedpane, dialog drag/resize, scrollbar-mouse, context-menu, focus-traversal and
clipboard are unasserted." Cut 6 (event/input/focus) is last in the sequence, but its
net must exist before any of that code moves. Doctrine stays D37: public getters are
the exact assertion, an ephemeral same-JVM re-paint diff (`compare(…, 0.0)`) only
corroborates where the primary observable is not a public getter, no committed input
goldens, new fixture files only.

**Observability decisions (per area).**

- *Public getters:* spinbox `getString("text")` (see Q4), slider/`getInteger("value")`,
  tabbedpane `getSelectedIndex`, text/caret `getString`/`getInteger("start"/"end")`.
- *Getter-less state* reads the same interned-literal `Object[]` model the trace
  harness already reads — `InputDriver.property()` chain-walk for `:view`/`:port`/
  `:vertical`/`:horizontal` (scrollbars), `"bounds"` (dialog geometry), menubar
  `"selected"`/`":popup"`, `":tooltipbounds"`.
- *Behavior with no state at all* (which item fired, where focus went) is observed by
  **handler recording**: `RecordingHandler` (extends `InputHandler`) logs
  `action`/`menushown` invocations and `focusgained`/`focuslost` (both DTD-registered
  on every component) via Thinlet's `method(this.name)` String-argument binding — the
  focus-owner has no public getter, so the recorded sequence *is* the focus assertion.

**Timer posture.** Mouse presses on spin/scroll arrows and tracks arm the 375/300 ms
auto-repeat. Tests neutralize it structurally, never by timing: **clamp-adjacent
positioning** (start one step from the limit — the first `processSpin`/`processScroll`
succeeds, every repeat clamps to a no-op → exact final values with zero flake window);
keyboard paths (which never arm the timer) for mid-range arithmetic; knob drags
(timer-free) for proportional moves. Track-paging in `scroll.xml` is exact for free:
one page (the `:port` extent) exceeds the fixture's whole scroll range, so the click
lands at the clamp and repeats no-op. The 750 ms tooltip timer is absorbed by the
D62 bounded poll; timing *semantics* (e.g. no-reset-on-jiggle) are excluded as
untestable deterministically.

**Slicing.** A: spinbox/slider/tabbedpane/scrollbar-mouse (+`RecordingHandler`, this
entry, Q4–Q6). B: menubar/context-menu (+the one driver addition, `metaClick` —
`MOUSE_PRESSED` with `META_DOWN_MASK`; the MouseEvent constructor maps extended→legacy
masks so `isMetaDown()` holds on JDK 8→21; Thinlet's popup trigger is deliberately
`isMetaDown()`, not `isPopupTrigger()`). C: focus-traversal/clipboard/dialog/
tooltip-hide. One PR each (D46 flow).

**Slice A findings** (characterized, beyond the expected): a *mouse* tab-switch also
fires the pane's `action` (after `setNextFocusable` walks focus into the new tab) —
not just the keyboard switch; a synthesized `FOCUS_GAINED` lands initial focus on the
tree's first focusable; the mouse wheel drives only the vertical bar (a no-op on a
horizontal-only list — pinned); mouse-selecting a tab with no focusable content
throws focus past the pane entirely (quirk candidate, pinned
`documents-current-behavior`, maintainer call pending). Locked quirks: **Q4** (dead
spinbox `value` attribute), **Q5** (`editable` doesn't gate spinning), **Q6** (slider
jump-to-pointer, no track paging).

**Validation (slice A).** 29 new tests green in the CI container (base row) and on
the 8/11/17 rows; no FontMetrics-derived number is asserted anywhere (fonts only aim
gestures — arrow columns via `width-2`, bar thickness = the `:vertical`/`:horizontal`
rect's own thickness), so the crossjdk rows are safe by construction. (Cross-ref
D36/D37/D40/D41/D45/D51/D61/D62.)

## D65 — thinlet-testkit realized as the thinlet-core test-jar; live-Drafts playthrough consumes it

**Date:** 2026-07-14. **Status:** accepted. **Phase:** 3 (net/tooling; zero `src/main`
change).

**Context.** D37 deferred a standalone `thinlet-testkit` module on a real reactor
cycle: the module needs a compile dependency on `thinlet-core`
(`InputDriver.Funnel extends Thinlet` for the protected `processEvent`), while four
core test classes (`DescriptorContractTest`, `InternTripwireTest`, both quirks tests)
import `thinlet.trace.XvfbDisplayExtension` — `core(test) → testkit → core(main)`,
and Maven reactor cycles are scope-blind hard errors. D53 named the live-`Drafts`
playthrough as the awaited second consumer; it has arrived.

**Decision — the test-jar shape, no new module (supersedes D37's anticipated
module).** `thinlet-core` attaches a `tests`-classifier jar (`maven-jar-plugin`
`test-jar` goal); `thinlet-drafts` gains its first test tree consuming
`thinlet-core:test-jar` (test scope). One dependency edge, no cycle; nothing
relocates — the golden suite, the four cross-package extension imports, and core's
CI wiring stay exactly where they are, which is precisely the relocation cost D37
balked at. Verified: Maven 3.9.11's reactor resolves the tests classifier from
`target/test-classes` at the `test` phase, so CI's `test`-goal matrix rows work
unchanged (flagged as a watch item for future Maven upgrades). The tests jar
deploys with releases — suppressing one classifier is nonstandard gymnastics, the
artifact is inert and versioned, and shipping it makes the testkit consumable
outside the reactor (the actual "testkit" promise).

- **The `attach()` seam.** `InputDriver` gains a package-private
  `interface EventSink { void dispatch(AWTEvent e); }` and
  `static InputDriver attach(Thinlet host, Object root, EventSink sink)` — the
  same gesture library, driving an existing app host instead of a parsed fixture;
  `load()` is unchanged in behavior (its `Funnel` now implements the sink).
- **`DraftsHost` placement.** `Thinlet.parse(String, …)` resolves classpath-relative
  to the RUNTIME class's package, so the funnel subclass must live in package
  `thinlet.drafts`; the playthrough tests live in package `thinlet.trace` inside
  `thinlet-drafts/src/test` — a split package across the test-jar and the module's
  own test classes (classpath-legal; no JPMS anywhere; same rationale as the
  package's own "reuse the package-private trace types" javadoc).
- **`thinlet-demos` de-fanged (closes the D44 gotcha).** Explicit surefire
  `<skipTests>true</skipTests>` in the demos pom (configuration, not the
  `${skipTests}` property, so a CLI `-DskipTests=false` cannot re-arm it): the
  src/main-only module no longer dies on `-DexcludedGroups=robot` in a dirty
  workspace, and `local-ci.sh`'s crossjdk row now scopes
  `-pl thinlet-core,thinlet-drafts -am`.
- **Drafts surefire block**: `DISPLAY=:99` + `-Dfile.encoding=UTF-8
  -Dthinlet.strictIntern=${strictIntern}` only — no `trace.*` knobs; the
  playthrough commits **no goldens** (D37 getter-first doctrine).

**The playthrough (second PR of this pair).** Deterministic-page allowlist, from a
per-page audit of every `init=`/`action=` handler: interact on TabbedPane, TreeDemo,
Lists, MDI, DialogDemo, EventLogger, Internationalization, Looks; navigate-only on
Widgets, FolderBrowser, Choosers, BeanTest, FocusTest, ModalDemo, ProgressMonitor;
never visit Style (installed-font inventory), System/* (system properties, toolkit
colors, win.propNames), AutoFill (default locale + user.dir), ClassExplorer
(JDK-varying reflection), Chart (static unseeded Random). Never click: BeanTest
"Exit" (`System.exit`), ModalDemo "Start" (nested `EventQueue` loop — hang),
Choosers (native dialog — hang), ProgressMonitor "Start" (450ms thread), Widgets
"Load" (2005 relative path). Every scenario ends with a no-ExceptionDialog guard
(`getCount(desktop) == 1`) so `Drafts.handleException` cannot swallow a real
failure into a dialog. Q8 (FolderBrowser's hardcoded `C:` NPEs off-Windows into a
live ExceptionDialog) is pinned there.

**Validation (PR 1).** Full reactor `test` in the CI container: core 326 green
(the `attach()` refactor is behavior-neutral by the whole existing net), demos
"Tests are skipped", drafts `DraftsBootTest` green — the live app boots headless
through the seam, ten nav nodes, real paint frame. (Cross-ref D22/D37/D43/D44/D53,
PHASE-3-GOALS 3b — the Drafts app becomes the first living test bed.)

## D66 — Documentation tense rule: live docs speak in today's tense; only this log keeps its date's (extends D57)

**Date:** 2026-07-14. **Status:** accepted. **Phase:** 3 (documentation policy).

**Context.** The post-D65 staleness sweep (PR #94) fixed live-doc claims across seven
files, but left CLAUDE.md's "Background — Phase 2.x" paragraph carrying false
present-tense claims ("the input surface is untested") on the reasoning that a
Background header excused them. The maintainer challenged the skip; PR #95 fixed it.
The same confusion recurs from the other side: dated D-entries that still read
"deferred" look wrong to a reader expecting them to track the present. Neither rule
was written down — D57 governs single-homing and comment content, not tense.

**Decision — two rules, codified.**

1. **Live docs** (CLAUDE.md, README, ROADMAP, the charter, `project-docs/`, code
   comments): every present-tense sentence must be true as of the merge that touches
   the file, regardless of section labels — "Background"/"historical" headers exempt
   nothing. History is written in past tense (built/was/landed); standing claims cite
   the newest governing D-entry.
2. **`DECISIONS.md` alone keeps each entry's original tense.** Entries are dated
   records — evidence of what was decided and observed when — and are never retensed
   or rewritten; reality changes land as *new* entries citing what they supersede.
   Corollary for readers: never take a D-entry as current state; the live docs carry
   today's truth and point at the newest D-number.

**Enforcement.** CLAUDE.md's documentation-policy section states the rule (every
session reads it), and the D60 comment-pass checklist gains a prose-docs line so
mixed Java+docs PRs get prompted pre-PR. Docs-only PRs remain judgment-guarded (the
D60 hook gates on Java diffs only — extending it to markdown would gate every
handoff-file touch, not worth the friction). (Cross-ref D57 policy, D60 gate,
PRs #94/#95 the motivating pair.)

## D67 — Vocabulary decode + constants research done ahead of the parked decision (fork-mapping lull work)

**Date:** 2026-07-15. **Status:** accepted. **Phase:** 3 (research/documentation; no product
behavior).

**Context.** Fork mapping (the queue head) stayed blocked on the fork sources
(expected ~2026-07-17/19), and the maintainer asked two things of the lull: revisit
the constants-for-strings idea, and deep-dive the cryptic 2005 method parameter
names/values — best-effort judgments acceptable, reasoning tracked. The constants
*implementation* stays parked (D56/D58 deferrals; D43 visibility discipline makes
public vocabulary 3c work), but per the maintainer's direction the *research* was done
now and committed, so the Cut 4/5/6 typing decisions and the 3c public-vocabulary
decisions become a table lookup instead of a re-survey.

**Method.** Two independent read-only decode passes over `Thinlet.java` /
`Renderer.java` / `DescriptorTable.java` (part-token+geometry; boolean-groups+
accessors), cross-checked against the 2005 website (`docs/*.html`), the descriptor
table, and the existing pins; every claim confidence-tagged (checkable /
test-pinned / justified guess). Site counts by scripted simple-argument match over
`is(…, "literal")` — recorded as lower bounds with the method stated. Full narrative
decode delivered to the maintainer as a session report (uncommitted, per the
deliverable split below).

**Decision — what landed where (single-home, D57).**

- **In-source annotations** (comment-only commit, zero code change): part-token
  vocabulary above `processScroll` (pinned: `InputScrollBarTest`,
  `InputSpinBoxTest`); invalidation-depth vocabulary above `update`; the
  `part` token/node duality above `handleMouseEvent`; the `block` three-way name
  collision at the field; the reserved-`:`-key extension above `createImpl`
  (`:widths`/`:offset` pinned by `GoldenLayoutStateTraceTest`; `:anchor` by
  `InputListTest.shiftArrowExtendsMultiSelection`); `paintRect` edge-flag
  semantics; a quirk-candidate breadcrumb at `checkLocation`.
- **`project-docs/VOCABULARY-INVENTORY.md`** (new): the 11-vocabulary inventory with
  sizes/site-count floors, the same-word collision table, per-vocabulary
  absorb-at-cut recommendations with reasoning, and the count methodology.
- **Notable decode results** (details in the doc/annotations): `dx`/`dy` magic pairs
  in `getSize` equal the widget's paint-time chrome (border+`IconTextSpec` padding),
  verified pair-by-pair; `mode` is three unrelated vocabularies sharing a name;
  `block` is three unrelated meanings; the event-name vocabulary is exactly the 11
  distinct names of the `"method"`-typed descriptor rows (18 rows — D58); scroll
  arrows move a hardcoded 10 px unit while track clicks page by the `:port` extent.

**Quirk candidates surfaced (dispositions the maintainer's, alongside Q5–Q8;
behavior unchanged, none test-pinned yet):**

1. `checkLocation` passes `mousex` for `handleMouseEvent`'s y argument when
   re-synthesizing hover state after a layout change (verified by direct read).
2. The combobox icon glyph hit-tests as `"icon"` but the click branch excludes it —
   clicking the icon is a no-op.
3. The table sort glyph draws `"ascent"` as a downward triangle.

**Non-goals, restated:** no constants, no enums, no renames now — the inventory's
"absorb at" column is design input, each decision lands with its cut; public
vocabulary shapes wait for fork mapping + 3c (D43). (Cross-ref D27 doc layout, D43
visibility, D56/D58 the deferrals this research feeds, D57 single-home + comment
rules, D61/D64 the pins cited by the annotations.)

## D68 — The D67 candidates pinned: Q9/Q10 locked, the checkLocation bug proven unobservable

**Date:** 2026-07-16. **Status:** accepted. **Phase:** 3 (net strengthening; no product
behavior).

**Context.** D67 surfaced three quirk candidates without pins — against the D64 norm
(behavior pinned first, dispositions after). The maintainer asked for the pinning
tests. A rigorous observability trace, run before writing any test, changed one
deliverable's shape.

**The trace result (supersedes D67's "blast radius looks small").** The
`checkLocation` mousex-for-y call is **currently unobservable**: (a) the preceding
`findComponent(content, mousex, mousey)` recomputes `mouseinside`/`insidepart` from
the *correct* coordinates, so the buggy call's component/part arguments are right;
(b) every `MOUSE_ENTERED` consumer across `handleMouseEvent`'s classname dispatch
(including `processField`, `processScroll`, `setInside`, and the tooltip path, which
reads the untouched `mousex`/`mousey` fields) dispatches on component/part/id and
never reads the raw x/y parameters; (c) nothing persists the corrupted value —
`referencex`/`referencey` are PRESSED/DRAGGED-only. No scenario exists today where
the swapped argument changes observable state. A behavioral quirk-pin is therefore
impossible; fabricating one would pin nothing.

**Decision — what landed.**

- **`InputQuirkPinsTest`** (thinlet.trace; 4 tests, 3 new `input/` fixtures — new
  files only): **Q9** combobox-icon-click-dead (with a caret discriminator proving
  the click geometry lands — guarding against a vacuous pass) and **Q10**
  ascent-draws-south / descent-draws-north (via the ephemeral-trace 4-scanline glyph
  signature: widths 0/2/4/6, y-step −1 = south), both tagged
  documents-current-behavior; plus the **canary**
  `closingTheDropDownUnderTheCursorCommitsAndStaysConsistent` (untagged — it asserts
  *correct* behavior), which drives `closeCombo` → `checkLocation` with differing
  x/y and is positioned to fail if the dead y parameter ever turns live.
- **`KNOWN-QUIRKS.md`**: Q9/Q10 entries; the checkLocation finding recorded in the
  *Triaged (not behavior-locked)* section with the unobservability evidence —
  disposition: fix in Enhanced Thinlet (provably invisible today).
- The `checkLocation` in-source breadcrumb updated from "awaiting disposition, not
  test-pinned" to the resolved status.

**Red-green discipline.** All three pins were mutation-checked before commit: each
inverted assertion failed with the correct actual value ('S'/1/null); the untouched
test stayed green. Three consecutive container runs green (fresh surefire reports —
an earlier bisect briefly chased a stale report; runs are only trusted with the
report file deleted first).

**Harness finding (worth knowing for future input tests).** A gesture that mutates
model state (here: the caret write) defers re-layout via the 2005 negative-width
dirty flag; until a paint resolves it (`layoutIfDirty`), `findComponent` hit-tests
against the negative-width bounds and misses — subsequent synthetic mouse events go
nowhere. Real apps repaint between gestures, so the trap is driver-specific: tests
must interleave `d.paint()` after state-mutating gestures before relying on
hit-testing. Existing suites did this incidentally (their trace-compares paint);
`InputQuirkPinsTest` does it deliberately with a comment.

**Validation.** Targeted class green ×3 in the CI container; full container rows in
the PR run. (Cross-ref D44 container loop, D45 determinism, D51 no-op-press
discipline n/a here, D64 characterization norm, D67 the source findings.)

## D69 — Enhanced Thinlet (3c) opens in this repo, fork-independently; the enhanced line is 0.2.x

**Date:** 2026-07-17. **Status:** accepted. **Phase:** 3c opens (governance; no behavior
change in this slice).

**Context.** The fork sources had not arrived by their expected window, and the
maintainer directed: stop idling on them, move into Enhanced Thinlet now, and build
**no expectations on what the fork mapping might advise**. The "where" question
(this repo vs a new one) was settled from the record: every 3c mention in the repo
treats it as a phase of this repo (the ROADMAP's Phase 3 heading includes "Enhanced
Thinlet" and lists fork-independent 3c items); D42 already superseded the
"toolchain not library" freeze for Phase 3; the one repo-split precedent
(`thinlet-archive`) froze the past, not the future; and the regression net — the
goldens, quirk pins, input suites, container CI, and cross-JDK matrix — is exactly
the machinery that makes deliberate behavior change safe. A new repo would abandon
or copy it. A new module (or repo) remains a later call, reserved for the full
idiomatic-rewrite/new-API stage.

**Decision — the line split.**

- **v0.1.x is the frozen modernized-2005 line**, anchored by the published `v0.1.0`
  tag (GitHub Packages, D28). A maintenance branch is cut from that tag only on
  demand; no proactive branch.
- **`main` becomes the enhanced line: 0.2.0-SNAPSHOT** (all four poms). japicmp
  keeps gating binary breaks against the published v0.1.0 baseline (the apicheck
  profile's pinned oldVersion — unchanged, and the version bump moves main further
  from the "version equals baseline" no-op footgun).
- The behavior-preservation contract ("preserve the 2005 observable behavior
  exactly") is **scoped to the v0.1.x line**; on the enhanced line it is replaced
  by the change-control protocol below. CLAUDE.md/README retensed accordingly
  (D66).

**Decision — the behavior-change protocol (the net gets spent, deliberately).**

1. Every user-visible change starts from a **recorded disposition** — a
   `KNOWN-QUIRKS.md` disposition field or a D-entry.
2. The same PR **flips the pinned test** to assert the new behavior: the
   `documents-current-behavior` tag comes off and the test keeps a sentence name
   describing the new contract. A behavior change with no pin to flip first gets a
   pin (D64 norm), then the flip.
3. **Golden re-records are authorized per-change**: only the scenarios the change
   affects, citing the authorizing D-entry in the commit; the D44 mechanics stay
   (CI container only, `clean` before record, two runs byte-identical). This
   amends D44/D52's "never re-record" for the enhanced line — the prohibition
   still holds for *unexplained* diffs.
4. The `KNOWN-QUIRKS.md` entry is updated in place to **"fixed in 0.2.x"** with
   the fixing D-number — the file becomes the ledger of intentional behavior
   changes as well as preserved quirks.
5. **japicmp stays** (binary breaks gated); public API *additions* are now
   deliberate 3c decisions, made sparingly — every published addition is de-facto
   frozen the moment it ships (D43's logic, now applied intentionally).

**Scope of the fork-independent 3c backlog** (order agreed with the maintainer):
the already-dispositioned quirk-fix batch first (`checkLocation` y-arg → parser
null-source Q1 → `FileChooser` guard → FolderBrowser root Q8 → dialog glyphs Q7),
then the public vocabulary (D67 inventory rows marked 3c). **Unchanged and still
gated on the fork sources:** the fork mapping itself, Cut 4/5/6 seam commitments,
and the fork-sourced enhancement re-implementation. Undecided-disposition quirks
(Q5/Q6/Q9/Q10, the D64 candidates) stay pinned and untouched.

**Verification for this slice:** docs + version only — container rows green, zero
golden diffs, japicmp green. (Cross-ref D4/D28/D29 release machinery, D42 charter
supersession, D43 visibility logic, D44/D52 the amended re-record rule, D64 the
pin-first norm, D66 tense, D67/D68 the research and pins this backlog consumes.)

## D70 — First enhanced-line source change: the checkLocation y-argument fixed

**Date:** 2026-07-17. **Status:** accepted. **Phase:** 3c (first fix of the D69 batch).

**Context.** The gentlest possible opener for the D69 protocol: the 2005
`checkLocation` re-synthesis call passed `mousex` for `handleMouseEvent`'s y
parameter (found D67, verified by direct read; D68 proved it **unobservable** —
correct component/part already recomputed, no MOUSE_ENTERED consumer reads the raw
x/y, nothing persists the value). Disposition recorded in KNOWN-QUIRKS' triage
section: fix in Enhanced Thinlet.

**Decision.** The argument now passes `mousey`. Protocol notes: no pinned
quirk-test existed to flip (the finding was triaged *not behavior-locked* — there
was no behavior to lock); the D68 canary
(`InputQuirkPinsTest#closingTheDropDownUnderTheCursorCommitsAndStaysConsistent`)
already exercises the path with differing x/y and stays green across the change,
as it must for a provably-invisible fix. **Zero golden re-records requested or
needed** — and that is the empirical validation: the full net's indifference
confirms the D68 unobservability proof.

**Validation.** Container base row + crossjdk 8/11/17 green, `git status` clean
(zero golden diffs). KNOWN-QUIRKS triage entry updated to "fixed in 0.2.x (D70)".
(Cross-ref D67 finding, D68 proof + canary, D69 protocol.)

## D71 — Q1 fixed: unreadable parse sources throw descriptive IOException (the earmarked first enhancement)

**Date:** 2026-07-17. **Status:** accepted. **Phase:** 3c (second fix of the D69 batch;
first *observable* behavior change of the enhanced line).

**Context.** Q1 — `parse(String)` on an unresolvable path and `parse(InputStream)` on
a `null` stream threw `NullPointerException` (the 2005 code even carried a
`/* thows nullpointerexception*/` comment at the swallow site). Disposition recorded
in KNOWN-QUIRKS ("fix — throw a descriptive IOException"); PHASE-3-GOALS had
earmarked Q1 as the first enhancement.

**Decision.** Two guards in period style: `parse(String, Object)` throws
`IOException("unreadable source: " + path)` when resolution yields no stream (this
also covers the previously swallowed `URL.openStream()` failures — same quirk
family, "valid URL but unreadable"); `parse(InputStream, Object)` throws
`IOException("null input stream")` on a null argument. The swallow-site comment
updated (it documented the NPE consequence that no longer exists). No signature
changes — `parse` already declared `throws IOException` since 2005, so japicmp is
indifferent by construction.

**Protocol (D69) applied.** Pin flipped in the same PR:
`ParserNullSourceQuirkTest` → `ParserUnreadableSourceTest`, the
`documents-current-behavior` tag off, tests assert the new contract (exception type
+ message content), control test unchanged. **Red-green both ways**: the flipped
tests against the unfixed source fail with the old NPE (2/3, control green);
against the fixed source 3/3 green. Zero golden re-records (no paint surface).

**Validation.** Container base + crossjdk rows green, zero golden diffs; ledger +
PHASE-3-GOALS earmark line updated. (Cross-ref D69 protocol, D70 the invisible
opener; Q1 in KNOWN-QUIRKS.)

## D72 — The null-deref family retired: FileChooser guard + Q8 FolderBrowser fix; SpotBugs exclusions off

**Date:** 2026-07-17. **Status:** accepted. **Phase:** 3c (third+fourth fixes of the
D69 batch, one PR — coupling rationale below).

**Context.** Two dispositioned fixes remained in the null-deref family: the
`thinlet-demos` `FileChooser` fallback's unguarded `File.list()` (triaged, "fix —
guard the null") and Q8, the Drafts FolderBrowser's hardcoded `C:` root whose
expansion NPE'd off-Windows ("fix — guard the null and root at
`File.listRoots()`/`user.home`"). Removing the long-standing
`NP_NULL_PARAM_DEREF`/`NP_NULL_ON_SOME_PATH_FROM_RETURN_VALUE` SpotBugs exclusions
(a ROADMAP 3c item: "remove exclusions as the code is cleaned") surfaced **exactly
one** remaining hit — Q8 itself — so the exclusion removal binds the two fixes into
one atomic slice: they land together or the gate can't come off. That coupling is
the recorded reason this PR carries two fixes despite the one-fix-per-PR default.

**Decision.**

- `FileChooser.View.getFiles`: `null` listing → empty array. **Not test-locked** —
  recorded honestly: `thinlet-demos` pins `skipTests` (no harness) and `View` is a
  private inner class; the compensating guard is static: SpotBugs now reports any
  regression of this exact pattern.
- `FolderBrowser.init`: roots from `File.listRoots()` (fallback `user.home` if
  empty/null); `FolderBrowser.expand`: `null` listing → empty directory. Pin
  flipped per D69: `#folderBrowserExpandPopsAnExceptionDialogOffWindows` →
  `#folderBrowserExpandsTheRealFilesystemRootGracefully` (tag off; asserts the
  platform root, no ExceptionDialog, placeholder replaced). **Red-green both
  ways** (old code fails the new test at the root assert: `expected "/" but was
  "C:"`). The folder page stays **off** the playthrough determinism allowlist —
  its content is the live filesystem, environment-dependent by nature.
- `config/spotbugs/exclude.xml`: the two null-deref patterns removed;
  `BugInstance size 0` across all three modules confirms nothing else in the
  reactor trips them (the D71 parse guards cleared the core hits).

**Validation.** Container base + crossjdk rows green; zero golden re-records
(FolderBrowser paints only through the live playthrough, which is getter-asserted;
no goldens cover the folder page). Ledger updated (Q8 + the triage bullet →
fixed in 0.2.x). (Cross-ref D13 the original findings, D65 the playthrough, D69
protocol, D71 the parse guards this exclusion removal depended on.)

## D73 — Q7 fixed: the dialog close glyph is live; maximize/iconify glyphs undrawn (batch complete)

**Date:** 2026-07-17. **Status:** accepted. **Phase:** 3c (final fix of the D69 batch).

**Context.** Q7's recorded disposition was deliberately two-option ("wire the glyphs
… or stop drawing them"). Shown the quirk live in the Drafts showcase, the
maintainer chose the middle path: **wire close; undraw maximize/iconify; leave the
Drafts demo untouched**.

**Decision — the wiring.** The close glyph's rect (the Renderer's `controlx` math:
`bounds.width - titleheight - 1, 3, titleheight - 2, titleheight - 2`) becomes a
live `":close"` part in `findComponent`'s header carve-out. `handleMouseEvent`'s
dialog branch closes (public `remove(component)`) on MOUSE_RELEASED **only when the
live `insidepart` is still the glyph** — release anywhere else cancels, button-style;
the glyph is no longer part of the header drag handle. No hover/press tint (the
2005 glyphs had none; minimal surface). The maximize/iconify draws are removed —
those glyphs never had wiring; the attributes stay parseable and inert, and their
old screen area reverts to plain draggable header. No new callback API: closing is
exactly `remove(dialog)`, what apps already do; a close-veto/notify binding is a
future public-API decision, deliberately out of scope. The 2005 glyph-paint helper
keeps its now-unreached 'm'/'i' cases (verbatim, unchanged code).

**Protocol (D69) applied.** Pin flipped in the same PR:
`InputDialogTest#titleGlyphsHaveNoClickWiring` →
`#closeGlyphClosesTheDialogAndTheOtherGlyphsAreUndrawn` (tag off): cancel-drag
does not close and does not move; the old maximize spot drags the dialog (the
observable proof the glyph is undrawn); a clean click removes the dialog.
**Red-green both ways** — against 2005 code the flip fails at "the glyph is not a
drag handle" (the dialog moved by exactly the drag delta). **Zero golden
re-records**: no committed golden scenario sets any of the three attributes (only
the ephemeral-trace fixture `input/dialog.xml` does).

**Vocabulary note.** `":close"` joins the dialog part tokens (the
`handleMouseEvent` duality annotation and `VOCABULARY-INVENTORY.md` row 5 updated
in this PR). **The D69 quirk-fix batch is complete** (D70–D73); next 3c item: the
public vocabulary (D67).

**Validation.** Container base + crossjdk rows green; `git status` clean.
(Cross-ref D48/D63 the Renderer seam, D64 the original pin, D69 protocol, D67/Q7
ledger.)

## D74 — The 3c public vocabulary ships: choice enums + event-name constants (D67 rows #3/#9)

**Date:** 2026-07-18. **Status:** accepted. **Phase:** 3c (public API addition; zero behavior
change).

**Context.** The second item of the D69 fork-independent backlog: publish the D67
inventory rows marked 3c — the choice-value sets (row #3, "natural public enums")
and the event names (row #9). Both are the *user's* vocabulary, frozen by the
byte-identical DTD (D8), so the published tokens cannot be invalidated by whatever
the fork mapping later advises — the shape D69 requires. This is the first
published API addition of the enhanced line, made under D69's "sparingly, and
de-facto frozen on ship" rule; the DTD and the descriptor table were cross-checked
value-by-value before shaping (they agree on every enumeration, including button's
`alignment` row re-ordering only to move the default to `center`).

**Decision — the shape (all new files; zero edits to existing source).**

- **Eight public enums** in package `thinlet`, one per choice attribute:
  `Alignment` (`alignment`), `HorizontalAlignment` (`halign`), `VerticalAlignment`
  (`valign`), `Orientation` (`orientation`), `TabPlacement` (`placement`),
  `SelectionMode` (`selection`), `ButtonType` (button `type`), `SortOrder` (column
  `sort`). Each carries its DTD attribute name as `KEY`, the per-constant DTD
  token, `token()`, and `fromToken(String)` throwing `IllegalArgumentException`
  with the 2005 choice setter's exact message shape (`unknown <token> for <key>`);
  `fromToken(null)` throws too — deliberately stricter than the wire setter,
  where a null *value* resets to the row default. **Naming contract: each enum is
  named for its DTD attribute identity, not its rendered meaning** — hence
  `Alignment` (the `alignment` attribute: label-family/textfield content
  alignment) deliberately sits beside `HorizontalAlignment` (`halign`: the
  layout-cell alignment); the attribute name is the user's stable handle, and
  `KEY` makes the pairing checkable. Constant order mirrors the table's
  first-declared row per key — which puts *that row's* default first; no
  `default()` is exposed, because defaults are per-widget-row (button re-declares
  `alignment` with default `center`).
- **The `KEY` constants are a deliberate 8-row slice of inventory row #2**
  (attribute keys, otherwise Cut 5 territory): a value vocabulary is unusable
  without its key, and these 8 keys are as DTD-frozen as the values. The other
  ~101 keys stay unpublished.
- **`EventNames`**: a final holder with 11 String constants — the names of every
  `"method"`-typed descriptor row (18 rows, 11 distinct — D58/D67).
- **Deliberately excluded** (each a separate future decision, most Cut-5-shaped):
  a shared token interface, typed `setChoice`/listener overloads, per-widget typed
  accessors, `toString()` overrides. The D58/D67 objection to internal constants
  ("scaffolding Cut 5 demolishes") does not apply here: a later typed API
  *consumes* these value types rather than replacing them.
- **Internal code untouched:** the interned-String `is()` contract and every
  consumer stay as-is; the enums talk to the model only through the public
  `setChoice`/`getChoice`/`setMethod` surface.

**The anchor.** `PublicVocabularyContractTest` (untagged — deliberate new-API
contract, not a documents-current-behavior pin) welds the vocabulary to the live
table so drift on either side fails the build: set-equality of every choice row's
allowed values against its enum *and* coverage in both directions (every choice
row has an enum, every enum matches a row); declaration-order anchor;
every-token round-trips through the real `setChoice`/`getChoice` on real widgets;
`fromToken` bijection + rejection message; event-name set-equality against the
method-typed rows (reflected from the class, so the test cannot drift from the
published constants); a real `setMethod` bind via a constant. Mutation-checked
before commit (D68 norm): a mutated choice token failed all three choice anchors
with the correct actual values; a mutated event constant failed the set anchor.

**Validation.** Container base row green (337 core + 13 drafts, +7 for this
slice); zero golden interaction (no paint/layout/input change; nothing to
re-record); japicmp additions-only against v0.1.0 (the apicheck CI job). An
independent Opus pre-merge review (ship-with-nits) re-verified every token/KEY/
constant against both the table and the DTD and confirmed pure-addition +
`--release 8`; its flags — the naming contract, the null divergence, the
default-first nuance recorded above, and a `fromToken` reject-path coverage gap
(closed: the test now rejects on all 8 enums) — landed in the same PR.
(Cross-ref D8 DTD freeze, D43 the de-facto-freeze logic, D57 doc/comment rules,
D58 the internal-constants deferral kept in force, D67 the inventory, D69
protocol.)

## D75 — The remaining quirk dispositions: Q5/Q9 fixed, Q6/Q10 kept, `sort="none"` made silent

**Date:** 2026-07-22. **Status:** accepted. **Phase:** 3c (behavior changes on the
enhanced line, under the D69 protocol).

**Context.** The head of the D69 backlog was the set of quirks whose dispositions
were the maintainer's to give: Q5 (spinbox `editable` gates typing only), Q6
(slider jump-to-pointer), Q9 (click-dead combobox icon), Q10 (ascending sort draws
a downward triangle). All four were pinned by `documents-current-behavior` tests,
so either answer was cheap; what was missing was the decision, not the code. The
dispositions were given in session on 2026-07-22 and are recorded verbatim in
effect below.

**Decision — the four dispositions.**

- **Q5 → fix.** `editable="false"` now means read-only on every value path, not
  just typed digits. The gate sits in `processSpin`, which is the single choke
  point for all three callers (the Up/Down key branch, the arrow-block press, and
  the auto-repeat timer); returning false there also keeps a press from arming the
  375 ms repeat. 2005 gated only `processField`.
- **Q6 → keep.** Slider press stays jump-to-pointer with no knob/track distinction
  and no click-to-page. Confirmed as a feature, not a defect; the pin stays as the
  guard against silent drift in a later Cut 6 refactor. No code change.
- **Q9 → fix, by folding the icon strip into the text area.** The combobox text
  branch now accepts the `"icon"` part instead of excluding it, so the strip takes
  the caret, the text cursor, and the icon-width caret offset the branch already
  computed. The part token is deliberately **kept**: deleting it in `findComponent`
  would also reroute the drop-button hover repaint, a strictly larger blast radius
  for no model gain. Editable comboboxes only — `findComponent` reports `"down"`
  for the whole widget when `editable="false"`, which already opened the list.
- **Q10 → keep, and document why.** `sort="ascent"` keeps painting the
  south-pointing glyph. The mapping diverges from the prevailing convention
  (Windows Explorer, Swing's row sorter, most web tables paint ascending as an up
  wedge) but is self-consistent — it reads as "values increase downward" — and the
  glyph is pure decoration: `"sort"` is referenced nowhere outside `Renderer`'s
  header branch, because Thinlet never sorts data. The cost of flipping is
  asymmetric and undetectable by us: naive apps improve, but any app that
  *compensated* for the inversion (writing `descent` to get the arrow it wanted)
  silently becomes wrong in the other direction. Convention is not worth that.

**A fifth change, found while deciding Q10 — `sort="none"` paints nothing.** The
painter skipped the glyph only on a `null` sort, and `setChoice` stores `"none"`
verbatim (it substitutes the row default only for a `null` *value*), so an
explicit `sort="none"` drew the same north triangle as `"descent"` — an unset
attribute and an explicit "none" disagreed, while `getChoice(column, "sort")`
already reported `"none"` for both. The maintainer's call: "none" means no arrow.
The painter now skips it, making the paint agree with the accessor. Chosen
*because* Q10 was kept: with `ascent` still south, flipping Q10 instead would have
collided `none` with `ascent` rather than with `descent` — the collision moves, it
does not go away. Locked as **Q11**.

**Evidence, and the order it was gathered.** The `sort="none"` behavior was a
source-read when it was reported, so it was proven before it was changed: the new
pin first asserted the 2005 `'N'` glyph and passed in the CI container, and only
then flipped to the 0.2.x no-glyph assertion — the D69 flip-in-the-same-PR rule
applied to a quirk that had no pin to begin with.

**Golden impact: none, across all five changes.** Q5 and Q9 are input-path only.
The `sort="none"` fix touches paint, but the attribute value appears in no corpus,
`thinlet-drafts`, `thinlet-demos`, or `docs` file — the one corpus use of `sort=`
(`corpus/drafts/widgets.xml`) is `ascent`/`descent`, both unchanged. Q6 and Q10 are
disposition-only. No re-record was performed or needed, matching the D70–D73 batch.

**Pins.** Three `documents-current-behavior` tags come off as their tests flip to
the new behavior (`InputSpinBoxTest#nonEditableSpinboxRejectsSpinningAsWellAsTyping`,
`InputQuirkPinsTest#clickingTheComboboxIconGlyphPlacesTheCaretLikeTheTextArea`, and
the new `#explicitSortNoneDrawsNoGlyphAtAll`); two new untagged pins were added —
`#hoveringTheComboboxIconGlyphShowsTheTextCursor`, so the Q9 fold is pinned as
total rather than click-only, and the sort-none pin above. The Q5 test pairs every
no-op assertion with the identical gesture on an editable sibling, so a gesture
that silently missed the widget cannot pass it vacuously. Q6's and Q10's pins are
unchanged and keep their tags: the behavior they document is still current.
(Cross-ref D64 the characterization suite that locked Q4–Q7, D68 Q9/Q10, D69 the
change-control protocol, D70–D73 the previous batch.)

## D76 — Q12/Q13 catalogued: the last two D64 candidates enter KNOWN-QUIRKS, dispositions open

**Date:** 2026-07-22. **Status:** accepted. **Phase:** 3c (documentation; zero behavior
change).

**Context.** D75 settled every quirk with an open disposition *except* the two
candidates D64 surfaced in passing: mouse-selecting an empty tab throwing focus out
of the pane (slice A) and a release over a disabled menu item closing the menu
silently (slice B). Both were pinned `documents-current-behavior` when they were
found, but neither was ever written into `KNOWN-QUIRKS.md` — so the catalog, which
is meant to be the list of pinned 2005 behaviors, was missing two of them, and the
handoff wrongly described them as needing pins first.

**Decision.** Catalogue both as **Q12** and **Q13**, disposition *undecided*,
each citing its existing pin. No behavior change and no new test: the pins already
assert what the code does, so this is purely the catalog catching up with the net.

**Two mechanism details worth pinning in prose**, both read from source while
writing the entries:

- **Q12.** The escape comes from `setNextFocusable(component, false)` in the
  tabbedpane press branch — and the 2005 author left the alternative commented out
  on the two lines directly above it (`setFocus(tabcontent != null ? tabcontent :
  component)`). The quirk looks like an unfinished edit, not a considered choice,
  which is evidence for a *fix* disposition — but the call is still the
  maintainer's. Mouse and keyboard switches disagree today: the keyboard path
  keeps focus on the pane.
- **Q13.** The `enabled` check gates only the `invoke`; the `closeup()` beneath it
  is unconditional for any non-`menu` item. Keyboard navigation already skips
  disabled items, so here too the two input paths disagree.

**Correction.** The D75 handoff bullet claimed these two were "still
uncharacterized: pin each first". That was wrong — both were pinned in D64. The
handoff now names the missing piece accurately: the dispositions, nothing else.
(Cross-ref D64 the characterization suite that pinned them, D69 the protocol a
disposition would follow, D75 the batch that settled the others.)

## D77 — Q12/Q13 fixed: focus stays in the pane, a disabled menu item swallows the release

**Date:** 2026-07-22. **Status:** accepted. **Phase:** 3c (behavior changes on the
enhanced line, under the D69 protocol).

**Context.** D76 catalogued the last two D64 candidates and left their dispositions
open; both were given the same day. With these, every quirk carrying an open
disposition is settled — the fork-independent quirk backlog D69 opened is empty.

**Decision — both fixed.**

- **Q12 → fix, narrowly.** The tabbedpane press branch now asks whether the newly
  selected tab holds anything focusable before it walks: if not, focus stays on
  the pane, matching the keyboard switch. Tabs with focusable content keep the
  2005 walk into their first focusable.
- **Q13 → fix.** A disabled item swallows the release: nothing fires and the popup
  stays open for a retarget. The `closeup()` now sits under the same `enabled`
  check that already gated the invoke. A release over no item still dismisses.

**Two implementation choices worth recording, because the obvious versions are
worse.**

1. **Ask, don't restore (Q12).** The tempting fix is to let the 2005 walk run and
   then pull focus back if it escaped. That is observably different: the widget
   outside the pane receives a focus-gained (and an app's `focusgained` callback
   fires) before focus returns. Pre-checking with a new `hasFocusableInside`
   helper avoids emitting an event the fix would then have to retract. The helper
   deliberately mirrors `isFocusable` — including its "not in an unselected tab"
   rule — and is therefore called *after* `"selected"` is written; calling it
   earlier would silently invert its answer.
2. **The author's commented-out line was not used (Q12).** Two lines above the
   walk sit `// Object tabcontent = getItem(component, current); // setFocus(...)`.
   Landing them would also change *non-empty* tabs — focus would go to the tab
   container instead of its first focusable — moving pins that have nothing to do
   with this quirk. The sketch is evidence about intent, not a patch to apply.

**Golden impact: none.** Both are input-path only; no paint or layout change, so
nothing to re-record. The pins flip in the same PR (D69) and drop their
`documents-current-behavior` tags. The Q13 pin was extended past the flip: after
the swallowed release it retargets the enabled sibling and asserts that fires and
closes, so "the menu stays open" is pinned as *usable*, not merely un-torn-down.

**Validation.** Container base row green and all three cross-JDK rows (8/11/17)
green, including the `Drafts` live playthrough, which drives real menus and tabs.
(Cross-ref D64 the suite that pinned both, D69 the protocol, D75/D76 the rest of
the batch.)

## D78 — The table's behavior is recorded: a 14-test input suite, and Q14 (the inert column header)

**Date:** 2026-07-23. **Status:** accepted. **Phase:** 3c (test net; zero behavior
change).

**Context.** Every other major interactive widget carried a suite recording what it
does when clicked and typed at — list, tree, combobox, spinbox, slider, menus,
tabs, dialogs, text editing, scrollbars, tooltips. The **table** had none:
`input/table.xml` existed only as a paint-snapshot scenario
(`InteractionScenarios` `table-selected-lead-focus`), which compares drawing
commands and asserts nothing about behavior. That left the widget most exposed to
the remaining layout/hit-testing cuts with no regression net, and left an obvious
unfinished code path unexamined.

**Decision.** Add `InputTableTest` — 14 black-box tests over the public getters,
no stored snapshots — plus the fixture `input/table2.xml` (multiple / interval /
headerless / empty tables). `input/table.xml` is untouched, as a snapshot anchor.
Recording only; no behavior changed in this slice.

**What it records.** Mouse selection and its repaint; `selection="single"`
replacing rather than accumulating; arrow/Home/End keyboard movement; shift-click
extending from the lead; control-click toggling a disjoint row in `multiple`;
`interval` extending with shift but **collapsing to the clicked row** on
control-click (only `multiple` reaches the toggle branch of `select`); shift+arrow
extending from a lead the *mouse* set; the strip below the last row and an empty
table both absorbing clicks silently; a headerless table reserving only its 1 px
border above the rows.

**Two findings.**

1. **Q14 — the column header is inert.** No click reaches a column: the header
   swallows it (selection untouched, nothing fires). `findComponent`'s table
   branch is `if (!findScroll(component, x, y)) {}` — an empty body precisely
   where column hit-testing belongs, which reads as unfinished rather than
   decided. The user-visible consequence is that the `sort` glyph (D75 Q10/Q11)
   is app-driven only; no gesture can change it. Catalogued undecided and
   **deliberately parked**: wiring it adds new public behavior (which event, what
   an app binds to), and the maintainer's fork may already answer it.
2. **Double-click fires `action` once, then `perform`** — not twice. The second
   press re-selects an already-selected row, and `selectItem` fires nothing when
   the selection does not change. Recorded because the naive expectation
   (action, action, perform) is wrong.

**A harness limitation found the honest way — by being bitten by it.** The first
run "showed" that clicking row *n* selects row *n−1*. That was not Thinlet:
`InputDriver.origin` sums the `"bounds"` chain only, ignoring the `:port` top
inset and `:view` scroll, so every row of a table with a header is aimed one
header-height too high. Exact for a list (no header, no offset), wrong for a
table. The suite therefore aims rows through a local `clickRow` helper that adds
`:port` and subtracts `:view`, keeping the artifact out of the assertions rather
than recording it as behavior. **Generalizing the fix into `origin` itself was
deliberately not done here:** it is shared geometry under every input suite, and
changing it belongs in its own slice with its own re-verification, not inside a
recording-only PR.

**Testkit additions** (test-scope only, never in the published jar):
`InputDriver.clickAtWithModifiers` and `doubleClickAt` — Shift/Control ride every
event of the gesture, as a real toolkit delivers them.

**Validation.** Container base row green (353 core + 13 drafts) and the 8/11/17
rows green. The headline Q14 assertion was mutation-checked before commit (D68
norm): inverted, it failed with the correct actual value. No paint or layout
change, so nothing to re-record. (Cross-ref D64 the characterization suites this
extends, D69 the protocol any later fix would follow, D75/D77 the quirk batches,
D68 the mutation-check norm.)

## D79 — local-ci single-test filter shipped; the `InputDriver.origin` fix specified and deferred

**Date:** 2026-07-24. **Status:** accepted. **Phase:** 3c (tooling + a recorded
plan; zero library behavior change).

**Context.** Two loose ends surfaced while recording the table suite (D78). This
entry ships the small one and specifies the larger one so the next session can
execute it without re-deriving the analysis.

**Shipped — `local-ci.sh -t <pattern>`.** The faithful-local-CI script
(`.devcontainer/ci/local-ci.sh`, D44) ran only the whole net; iterating on one
suite meant hand-writing the `docker run … -Dtest=…` line every time (this session
did it ~10 times). It now takes an optional `-t <pattern>` that swaps the goal
from `verify` to `test` and forwards `-Dtest` — composing with the JDK-row
argument (`local-ci.sh 8 -t InputTableTest`). The effective Maven command is
echoed before it runs, per the self-evident-tooling norm. One gotcha found and
fixed while validating: the filter needs `-Dsurefire.failIfNoSpecifiedTests=false`
(not the older `failIfNoTests`) so a filter naming only a `thinlet-core` class
does not fail the `thinlet-drafts` module that matches none of it. The no-argument
full-`verify` path — the pre-push gate — is byte-for-byte unchanged.

**Specified and deferred — the `origin` scroll/header offset.** `InputDriver.origin`
sums the `"bounds"` chain up the parent chain, but a scrolling container positions
its children by its `:port` inset minus its `:view` scroll — the exact transform
`findComponent` applies as it recurses into children (`Thinlet.java` ~4054). So a
click on a child inside a headered or scrolled container is aimed short by that
offset. D78 was bitten by it: a table row appeared to select the row above,
because the header band (~one row tall) shifts every row's screen position.

- **The fix** is to mirror the transform in the walk: for each node, after adding
  its own `bounds`, add its *parent's* `(:port − :view)` when the parent has a
  `:port`. That makes `d.click(row)` land true and lets `InputTableTest.clickRow`
  (the local workaround) be deleted; `center`/`size` inherit the correction for
  free since they build on `origin`.
- **Why it is its own slice, not folded into D78.** `origin` underlies all 18
  input suites, so the full net is the verification gate for changing it — that
  belongs in a focused PR, not a recording-only one. The blast radius is real and
  cuts both ways: most suites aim at widget *centres* and were tolerant of the old
  ~1 px border error (they should stay green untouched), **but** any suite that
  already compensates for scroll by hand would then double-correct and break.
  `InputScrollTest`, `InputSplitPaneTest`, and `InputTreeTest` are the named
  suspects to audit first — named as suspects, not cleared. The slice: fix
  `origin`, delete `clickRow`, add a centre-based `clickWithModifiers`, run the
  base row + 8/11/17, and fix-or-explain any suite that moves.

(Cross-ref D44 the local-CI harness, D78 where `origin` was found wanting, D64/D65
the input net that gates the deferred fix.)

## D80 — `InputDriver.origin` made scroll/header-aware; the table row-click workaround removed

**Date:** 2026-07-31. **Status:** accepted. **Phase:** 3c (test harness; zero library
change). Closes the item D79 specified and deferred.

**Decision.** `origin` now adds each parent's content offset (`:port` inset minus
`:view` scroll) as it walks the `"bounds"` chain — the same transform
`findComponent` applies descending into a scrolling container's children. A click
on a child *inside* a headered/scrolled container therefore lands true, so
`d.click(row)` works and `InputTableTest`'s local `clickRow` helper (the D78
workaround) is deleted. `center`/`size` build on `origin` and inherit the fix.

**Blast radius, confirmed empirically — narrower than D79's worst case.** The fix
changes `origin(widget)` only when an ancestor carries a `:port`, so it touches
only child-object clicks inside a scrolling container. The full input net is
green, base row + 8/11/17:

- The D79 suspects are unaffected. `InputSplitPaneTest` — splitpane positions its
  children with no `:port`, so `origin` is unchanged. `InputScrollTest` —
  wheel-scrolls and asserts on `:view.y`; never clicks a scrolled child by
  coordinate.
- The genuinely-affected clicks — `InputListTest` (list items) and `InputTreeTest`
  (tree nodes) — stayed green: both containers reserve only a ~1 px border
  `:port` and aim at item centres, so the old under-aim already landed within the
  row and the exact aim still does. No suite moved; nothing to fix-or-explain.
- Popup/combolist item clicks address the item as an *offset on the container*,
  whose own `origin` is unchanged (its parent is the desktop, no `:port`), so they
  are untouched.

**Testkit deltas** (test-scope only): `InputDriver` gains centre-based
`clickWithModifiers`/`doubleClick`; the offset-based `clickAtWithModifiers`/
`doubleClickAt` added in D78 solely for `clickRow` are removed (they were used
only there), leaving no dead helpers.

**One golden re-recorded — the fix corrected a mis-aimed scenario.** The
interaction golden `table-selected-lead-focus` drove `d.click(getItem(tbl, 1))`,
but under the old `origin` that click landed one header-height high on **row 0**,
so the committed golden encoded row 0's selection/focus while the scenario's code
asked for row 1. With the fix the click lands on row 1, so the render trace moved
the selection+focus band from y=0 to y=18 (one row down) — the paint of "row 1
selected", not any renderer change. Re-recorded in the CI container, `clean`
first, all 51 interaction goldens regenerated with **only** this one changing
(D44/D69 discipline); the diff was inspected and is exactly the row-0→row-1 shift.
The layout-state golden for the same scenario did **not** move — selection changes
where the highlight paints, not the row geometry.

**Validation.** Full input net green including the re-recorded golden;
`clickSelectsTheRowUnderThePointer` was mutation-checked (D68 norm) — inverted, it
failed with `but was: 1`, proving the click lands on the addressed row rather than
the fix masking a mis-aim. Base row + 8/11/17 green; no library behavior change.
(Cross-ref D78 the symptom, D79 the deferral this closes, D44/D69 the golden
re-record protocol, D68 the mutation-check norm.)

## D81 — Q3 step 1: the icon miss becomes audible (log only; the return value is untouched)

**Date:** 2026-08-15. **Status:** accepted. **Phase:** 3c (enhanced line, 0.2.x).
Authorized by the maintainer 2026-08-15, together with D82/D83 — the three quirks whose
`KNOWN-QUIRKS.md` dispositions read `fix` but cited no entry, so they were proposals, not
decisions. `.claude/NEXT-STEPS.md` had reported that backlog as empty; corrected here.

**Decision.** `getIcon(String, boolean)` reports what it used to swallow. A non-empty path
that resolves to no image logs at WARNING; each failed resolution attempt logs its
`Throwable` at FINE; and on the `preload` path a `MediaTracker.isErrorID` check reports the
resolved-but-undecodable image — the "unloadable" half of Q3, which is invisible without the
tracker because `Toolkit.getImage` returns non-null for a URL that is not an image.

**Log only — no throw, and the return value is unchanged.** The maintainer's reasoning,
recorded because it generalizes: a throw is either a real behavior break for apps that do not
catch it, or pointless for apps that do. So `getIcon` still returns `null` for a miss and
still returns the broken `Image` for a decode failure; the public javadoc contract
(`@return the loaded image or null`) stays true, and no golden moves. Q3 is therefore
**half-fixed by design** — the silence is gone, the null remains.

**Scope boundaries.** An absent or empty `icon` attribute reaches `getIcon` as `null`/`""`
and stays silent: no icon was asked for, so there is no miss to report. No dedup cache — the
call site is attribute-set (`set(component, key, getIcon(value))`), not per-paint, so a static
path `Set` would add thread-safety and retention surface for no gain.

**`java.util.logging`, deliberately.** `thinlet-core` ships runtime-dependency-free (D31), so
the JDK logger is the only option that does not add a dependency to the published jar. The
logger name is `thinlet.Thinlet`, which is what the test attaches its handler to.

**Validation.** `IconResolutionLoggingTest` (4 tests, untagged) pins the WARNING for an
unresolvable path, the WARNING for a resolved-but-undecodable file URL, and silence for both
the resolvable and the absent-path cases. Red-green checked both ways: with the source change
stashed, the two behavior-asserting tests fail and the two negative controls still pass.
`GetIconSilentNullQuirkTest` and its `documents-current-behavior` tag stay as they are — the
null return is deliberately retained. Full container run green: 357 core (+4) + 13 drafts, no
golden re-record.

**The diagnostic immediately earned itself.** The suite now emits 33 WARNING lines, all but
one for `/icon/volume.gif` — the single asset genuinely absent in 2005 (D54), referenced once
at `corpus/drafts/widgets.xml:222`. That is the intended signal, not noise: the quirk's own
history is that this silence hid 25 unvendored corpus assets until D54.

**Step 2 deferred, with a brief.** The maintainer wants a supplied replacement asset shipped
with the library and painted in place of the missing icon — an Outlook-style blocked-image
indicator. Deferred deliberately: it changes layout wherever an icon is missing (widths stop
being zero), needs an asset that would be the first image resource in the published jar, and
raises a real API question — whether the placeholder is returned from public `getIcon`, which
would end `null` as the app-visible missing-icon signal. Recorded in `KNOWN-QUIRKS.md` Q3 and
the `ROADMAP.md` 3c backlog. (Cross-ref D54 the vendored assets, D69 the change-control
protocol, D31 the dependency-free constraint.)

## D82 — Q2: the splitpane clamp stops destroying the requested divider

**Date:** 2026-08-15. **Status:** accepted. **Phase:** 3c (enhanced line, 0.2.x).
Authorized by the maintainer 2026-08-15 (with D81/D83).

**Decision.** `"divider"` now means *the position the app asked for*, and `doLayout` clamps
to `maxdiv` for the current layout **without writing the clamp back**. A shrink past the
divider no longer overwrites the request, so growing the pane returns it to where it was.
The divider stays **absolute pixels**: rescaling it proportionally would change what
`getInteger(sp, "divider")` returns for every app that sets one, which is a separate
decision and probably wants a new attribute rather than a redefinition of this one.

**Not the one-line change it looked like.** The obvious fix — delete the write-back — paints
the divider bar off the pane's edge, because `Renderer.paintSplitPane` positions the bar from
the same model value that layout was clamping. The clamped position therefore has to be
published: `doLayout` writes `":divider"` (reserved-key convention, added to the model-schema
comment above `createImpl`) every layout, and the two readers that must see the *effective*
position now read it:

- **`Renderer`** paints the bar at `":divider"`, falling back to `"divider"` for a paint that
  precedes any layout.
- **The keyboard path** steps from `":divider"`, so Left/Right in a pane too small to honour
  the request move the visible bar instead of silently incrementing a request nothing shows.
- **The drag path needed nothing.** It derives `moveto` from the pointer and already clamps to
  `[0, size−5]`, reading `"divider"` only to skip a no-op write — so a drag is simply a new
  request, which is the right semantics.

**Divergence, stated plainly.** While a pane is too small, `getInteger(sp, "divider")` returns
the remembered request, not what is on screen. That is the point of the fix (the app's property
is no longer silently rewritten by the layout), but it does mean the two can differ until the
pane has room again.

**Validation.** `InputSplitPaneTest#dividerIsAbsolutePixels_andSurvivesAShrinkPastIt` replaces
the pin of the destructive clamp (tag `documents-current-behavior` off) and asserts both halves
black-box: the request survives the shrink, and the first pane's *width* — the effective
divider — is clamped to 145 and then restored to 200. A second test pins that a drag while
clamped replaces the remembered position rather than snapping back on grow; it passes against
2005 too (there was no memory to clear), so it is a guard, not a discriminator. Red-green
checked: with the source stashed, the flipped pin fails on the remembered-request assertion.
Full container run green — 358 core (+1) + 13 drafts — with **no golden re-record**: no corpus
splitpane starts clamped, so the derived `":divider"` equals `"divider"` everywhere the goldens
look, and `":divider"` is not one of the keys the D61 layout-state sidecars record.

**Still 2005 by choice:** the non-proportional half of Q2. Growing a splitpane does not keep
the split ratio; the entry stays in `KNOWN-QUIRKS.md` recording that, now as the open half.
(Cross-ref D69 the change-control protocol, D61 the sidecar key set, D48 the Renderer seam.)

## D83 — Q4: the spinbox's `value` attribute stops being dead storage

**Date:** 2026-08-15. **Status:** accepted. **Phase:** 3c (enhanced line, 0.2.x).
Authorized by the maintainer 2026-08-15 (with D81/D82), closing the last of the three
uncited dispositions.

**Decision.** `value` and `text` mirror each other on every path that writes either, so the
DTD-declared integer is the spinbox's live state instead of a slot nothing read. `text`
stays authoritative for display and editing — apps read `getString(sp, "text")` today and
must keep working — and `value` follows it.

**The attribute is kept, not removed.** The other half of the recorded disposition
("remove the dead attribute") would edit `thinlet.dtd`, which is byte-identical 2005 (D8)
and its own decision. No DTD change was needed: `value CDATA '0'` is already declared.

**Four write paths, two directions, one rule.** Two private helpers (`spinValueFromText`,
`spinTextFromValue`) both no-op for any component that is not a spinbox, and both write the
model directly rather than through the public setters, so the mirror cannot recurse:

- `processSpin` — arrows, Up/Down keys, and the auto-repeat timer.
- `changeField` — typed digits, the shared field-edit commit (the spinbox guard keeps
  textfield/textarea/combobox untouched).
- `setString(sp, "text", …)` and `setInteger(sp, "value", …)` — the public setters, each
  driving the other property.
- `addAttribute` — the XML parse path, where the interesting case lives (below).

**A declared `text` wins a declared conflict, in either parse order.** `value` seeds the
display only when no `text` is present; when `text` is already there, the `value` attribute
is reconciled *down* to it. So `<spinbox text="5" value="42">` and
`<spinbox value="42" text="5">` both end at 5 — the display is the truth, which is what
2005 apps encode. The input fixture ships exactly that conflicting pair, and the pin asserts
5, not 42.

**Non-numeric text stays inert.** `<spinbox text="SpinBox">` is real — the 2005 corpus ships
it in `drafts/{looks,widgets}.xml` — and already no-ops through `processSpin`'s
`NumberFormatException`. The mirror does the same: `value` keeps its last numeric state
rather than inventing one, so an unparseable spinbox reads back the DTD default 0.

**Validation.** `InputSpinBoxTest` — the Q4 pin flips to
`valueTracksTheSpinState_andADeclaredTextWins` (tag `documents-current-behavior` off) and
three tests are added: typed digits move `value`, the two public setters drive each other,
and a new fixture (`input/spin-value.xml`, new file per the no-modify-fixtures rule) covers
`value`-only seeding plus the non-numeric case. Red-green checked: with the source stashed
all four fail. Full container run green — 361 core (+3) + 13 drafts — with **no golden
re-record**: `value` is never painted, and no corpus spinbox declared it. (Cross-ref D64 the
suite that locked Q4, D75 the `editable` gate in the same method, D8 the verbatim DTD.)

## D84 — the D61 residual gap closed: `:view.x` is pinned by a horizontal-scroll scenario

**Date:** 2026-08-16. **Status:** accepted. **Phase:** 3c (test net; zero library
change). Closes the residual gap D61 recorded and left open.

**The gap was real and measurable.** Across all 58 committed layout-state sidecars
`:view.x` was `0` everywhere; exactly one node carried any scroll at all
(`:view.y = 317`). The horizontal half of the scroll state was therefore pinned
only at rest — a regression that moved `view.x` would have gone unseen. The
coverage guard did not catch this because `allFourKeysExercised` tested
`view[0] != 0 || view[1] != 0`, and the single vertical scroll satisfied it.

**Decision.** New interaction scenario `arrows-hlist-scrolled-right` on the
existing `/input/arrows.xml` fixture (no fixture modified, per the new-files-only
rule), and the guard's either-axis check split into two assertions so neither axis
can silently go unexercised again.

**Why a knob drag and not the wheel.** The first attempt wheeled the list and
recorded a still-zero `:view.x`: the wheel drives the vertical bar only, which
`InputScrollBarTest.wheelIsANoOpWithoutAVerticalScrollbar` already pinned. The
scenario uses the knob drag `InputScrollBarTest.horizontalKnobAndArrowsClampExactly`
asserts against — dragged past the track's right end it clamps to
`view.width - port.width`, so the recorded offset is a **clamp**, not a
drag-distance computation, and no auto-repeat timer is armed (the D51 constraint).

**Metric stability, checked rather than assumed.** The clamp derives from the
fixture's very wide row, so the recorded `:view.x = 473` is a text-metric-derived
number, which D7 treats as tolerant-within-±2px rather than exact. `view.width`
(1487) was already recorded in existing sidecars that pass on every row, so the
derived clamp was expected to be stable; it was then confirmed on all four JDKs.

**Validation.** Red-green: with the scenario absent the split guard fails on
`a non-zero horizontal :view.x scroll offset exercised`. Two new goldens recorded
in the CI container (D44), all 52 interaction goldens and 59 sidecars regenerated
with **only** the two new files appearing — no existing golden moved. Base row 363
(+2) + 13 drafts, and JDK 8/11/17 rows green at 360.

**Unrelated hygiene folded in.** `mvnw.cmd` was committed by the D-less Dependabot
merge (#117) with CRLF bytes in the blob, while `.gitattributes` specifies
`*.cmd text eol=crlf` — store LF, check out CRLF. Git's clean filter therefore
reported the file permanently modified in every worktree. `git add --renormalize`
stores the LF blob; the checked-out file stays CRLF. No wrapper behavior change.
(Cross-ref D61 the sidecar net and the gap, D7 the tolerance model, D51 the
timer-free-gesture constraint, D44 the container-record rule.)

## D85 — a home for started-then-stopped work: `project-docs/UNFINISHED-IDEAS.md`

**Date:** 2026-08-25. **Status:** accepted. **Phase:** documentation only (no
library, build or test change).

**The gap is a class of fact with nowhere to live.** Four homes already partition
the project's documentation (D27/D57): `DECISIONS.md` holds rationale,
`project-docs/ROADMAP.md` holds work intended but not begun, `.claude/NEXT-STEPS.md`
holds current state and the ordered next work, and `KNOWN-QUIRKS.md` holds behavior
contracts. None of them answers *"there is a half-built tool on a branch — what was
it for, and what stopped it?"*. Started-then-stopped work is neither an intention
nor current state, and its rationale is worth recording precisely when the work is
**not** being decided on. Left unhomed, the artifact survives in git while
everything that makes it legible survives only in the conversation that produced
it — and conversations end.

**Decision.** A fifth home, `project-docs/UNFINISHED-IDEAS.md`, owning exactly one
class of fact: work that was started and then stopped — where the artifact is, how
far it got, what stopped it, and what a cold resume costs. It cross-references the
other four rather than restating them, and names the parked items that stay put
(Q14 per D78, the fork mapping, Q3 step 2, D5's `--release 8` hedge) so they are
never copied in. An entry is not a commitment to finish anything.

**Why `project-docs/` and not `.claude/`.** `.claude/MANIFEST.md` advertises that
directory as a one-pass deletion — every path in it is listed as safe to remove.
A record whose entire purpose is to outlive the context that produced it cannot
live somewhere designed to be thrown away. The file is durable project
documentation and is therefore not listed in the manifest.

**Two format rules the class forces.** Entries describe work that usually does not
exist on `main`, so every path must be **qualified by the branch it lives on** or
the reference dangles for a reader on trunk. And the narrative fields carry **no
length cap** — Intent in particular should run as long as the idea needs, because
it is the field that must survive the loss of the conversation; under-writing it
is the expensive failure mode. Only Status and the two dates are meant to be terse.

**Retention.** Entries are updated in place as the facts change (the `last
reviewed` date makes staleness visible on the page), and are removed only by the
maintainer when the work resumes or is abandoned outright. Unlike this log, the
file is not append-only and keeps no history: it speaks in today's tense (D66).

**Seeded with `loop-modernise`.** The branch — an autonomous behavior-preserving
modernisation loop over `thinlet-core/src/main/java`, three commits, never merged
— was pushed to `origin` on 2026-08-25 so it stopped being a single local copy,
and is recorded rather than finished. It has never completed a pass: its preflight
cannot run japicmp because GitHub Packages needs read auth even for public reads
(D4) and this host has no `~/.m2/settings.xml`. The entry records all three routes
past that, so they are not re-derived.
(Cross-ref D27 the directory layout, D57 the single-home rule, D66 the tense rule,
D4 the packages-auth fact the seed entry rests on.)

## D86 — the SAX and DOM parser modes get a net, ahead of any automated rewrite

**Date:** 2026-09-02. **Status:** accepted. **Phase:** 3c (test-only; no library,
build or behavior change).

**The gap was found by using the net, not by auditing it.** `loop-modernise`'s
first completed `--dry-run` (D85) proposed retiring nine redundant `new String(…)`
copies in `Thinlet.java`. The slice passed every gating row — base, JDK 8/11/17,
japicmp — and that green result was weaker than it looked: five of the nine edits
sit inside `if (mode == 'D')` and the SAX `else` branches of `parse`, and nothing
under `src/test` called `parseXML` or `parseDOM`. The rows were green partly
because that code never ran.

**Why this is a net gap and not a slice problem.** The loop's whole premise is
that a green net proves nothing observable moved, so the maintainer's attention is
spent on the disposition rather than the diff. That premise holds only where the
net reaches. Neither of the loop's other guards closes this: the stray-path guard
passes because `Thinlet.java` is squarely in the target tree, and japicmp passes
because removing a `new String(…)` changes no signature. An unattended run over
`thinlet-core/src/main/java` would keep proposing changes to `parse`, and the only
thing standing between an uncovered branch and a committed regression would be the
slice author's own judgement.

**Decision.** Two characterization suites, `ParserSaxModeTest` and
`ParserDomModeTest`, pinning the 'S' and 'D' branches through their documented
surfaces — the `startElement`/`characters`/`endElement` callbacks, and the
`getDOM*` accessors. 19 sentence-named tests (D57: the tests are the spec). They
cover the callback sequence, attribute delivery, element text, whitespace
collapsing with the trailing-space trim, nesting, and the declaration/comment/
doctype skips. SAX-mode attribute assertions sort the raw `Hashtable` first,
because its iteration order is unspecified and would otherwise vary by JDK row.

**Proven to have teeth, not merely to pass.** Passing on first run proves nothing
about a characterization suite, so the five previously-uncovered lines were each
mutated to a constant and the suites re-run: **15 of 19 tests failed**, spanning
both classes and every assertion kind. The mutations were then reverted and the
source confirmed byte-identical to its pre-mutation state. The four DOM tests that
survive the mutation assert null-returns and tag counts, which those five lines do
not feed.

**The slice that prompted this is safe — now demonstrably.** Re-run against the
new suites, all 19 pass. That was the likely outcome; the point is that it is now
a result rather than an assumption. The slice remains uncommitted (a `git stash`
entry on `loop-modernise`), because proving the loop works is not authorization to
modernise the source.

**What this does not claim.** Only the five lines the slice touched were mutation-
checked, so this is a net for the branches the parser modes expose, not a coverage
guarantee for `parse` as a whole. The `'T'` GUI branch was already covered by the
golden corpus and is untouched here.
(Cross-ref D85 the `loop-modernise` record this came out of, D57 the
sentence-named-tests-are-the-spec rule, D43 the public-API discipline japicmp
enforces, D52 the string-literal rewrite hazard this class of change sits nearest.)

## D87 — `loop-modernise.sh` lands on `main`; the loop runs in a worktree on its own branch

**Date:** 2026-09-02. **Status:** accepted. **Phase:** 3c (tooling only; no
library, build or behavior change — the script is not wired into the build and
nothing invokes it automatically).

**What changes.** `scripts/loop-modernise.sh` moves from the parked
`loop-modernise` branch onto trunk. D85 recorded it as started-then-stopped work
precisely because it had never completed a pass; that reason is gone. It is
unblocked (the japicmp credential gap, D85), it has completed a verified slice,
its net now reaches the code it was most likely to damage (D86), and the parser it
must not touch is fenced with an enforced guard, not a prompt rule.

**Why the tool belongs on trunk and its output does not.** Keeping the script on
the same branch as the slices it produces would mean every future PR of modernised
code also carried the tool's history, and a reviewer would have to separate the two
by hand. On `main`, the script is a tool like `local-ci.sh` or `comment-pass.sh`,
and a run branch contains **only** slices — which is the thing being reviewed.

**Runs happen in a linked worktree, not the primary checkout.** The loop rewrites
`thinlet-core/src/main/java` and rolls back on failure; doing that in the working
copy someone is reading conflicts with ordinary work, and the loop refuses to start
on a dirty tree anyway. A worktree also isolates `target/` and `.m2/`, which is
what makes concurrent use safe at all. Two facts were checked rather than assumed:
`local-ci.sh` bind-mounts the worktree, whose `.git` is a *file* pointing outside
the mount, and the base row still runs green (no build plugin here reads git); and
a fresh worktree has no `.m2/`, so it is seeded by copy — which also carries the
japicmp baseline, letting the API gate resolve offline.

**One bug this surfaced.** The `flock` used `$root/.git/loop-modernise.lock`,
which assumes `.git` is a directory. In a worktree it is a file, so the redirect
failed and took the run with it under `set -e` — the loop could only ever have run
in the primary worktree. It now uses `git rev-parse --git-dir`, deliberately not
`--git-common-dir`: the lock is meant to be per-worktree, because it is sharing
`.m2/` and `target/` that corrupts build state, and separate worktrees share
neither.

**What is still unproven, and is why the first real run is capped.** No slice has
ever failed verification, so the **repair** path has never executed; nor has the
**commit** path, which `--dry-run` deliberately stops short of. The first real run
is capped at three slices for that reason — enough to exercise committing and to
show whether quality holds across passes, small enough to review as one batch.
Merging what it produces stays a separate, ordinary PR decision; nothing here
authorizes the loop's output onto `main`.
(Cross-ref D85 the record this closes out, D86 the parser net, D69 the
change-control protocol any observable change would still go through, D44 the
container-only golden rule the loop's verification rests on.)

## D88 — the trace records `setRenderingHint`, which immediately exposed Q15: a JVM-wide antialiasing latch

**Date:** 2026-09-05. **Status:** accepted. **Phase:** 3c — **behavior change**
(D69 protocol: recorded disposition, the pinning test lands in the same PR),
plus the harness change that found it.

**The gap that started it.** `Thinlet.paint` sets two antialiasing hints on every
paint and nothing observed them. `TracingGraphics2D.setRenderingHint` delegated to
the real `Graphics2D` without calling `rec(...)` — unlike `fillRect` and its
neighbours — and no golden mentioned the op. The trace was byte-identical whether
both hints were set or dropped entirely.

**Found by using the loop, not by auditing the harness.** This is the second time
`loop-modernise` modified code the net does not watch and reported green: first the
DOM/SAX parser branches (D86), then its run-1 slice retiring the 1.4 reflection
behind these hints. Dead 2005 compatibility scaffolding is simultaneously the code
least likely to be covered and the first thing an automated modernizer reaches for,
so two occurrences in two runs is a pattern rather than a coincidence.

**What recording the hints revealed within one CI run: Q15.** The 2005 code cached
the reflectively-resolved `setRenderingHint` `Method` in a **static**, keyed to the
first `Graphics` class the process ever painted with. A second implementation made
`invoke` throw `IllegalArgumentException`, and the catch set `TXT_AA = null` — the
flag guarding the whole block. Antialiasing then stayed off for every later paint,
in every instance, for the life of the JVM: a one-way latch, not per-paint
degradation.

It surfaced as the base row failing what the JDK 8/11/17 rows passed, and what the
same suite passed locally — because the trigger is test *order*, whichever suite
paints through a raw `Graphics2D` first. Rather than infer, the mechanism was
isolated directly: a probe painting one `Thinlet` through `TracingGraphics2D`,
then a raw `Graphics2D`, then `TracingGraphics2D` again, recorded **2 hints, then
0**. That probe is now `AntialiasingPersistenceTest`.

**Decision on the behavior (D69).** Q15 is fixed, not documented: an application
painting through a printer or image `Graphics` alongside the screen one is
ordinary, and silent permanent loss of antialiasing is not a contract worth
keeping. `paint` now tests `g instanceof Graphics2D` and calls `setRenderingHint`
directly — no cached `Method`, no static flag, nothing to latch. The guard is kept
deliberately: it preserves what the reflective `NoSuchMethodException` gave a
non-`Graphics2D` `Graphics`, which is to skip the hints for *that paint only*.

**The fix is the loop's own slice**, unaltered. It was proposed as retiring dead
weight at the Java 8 floor, and turned out to repair a defect neither the loop nor
its reviewer knew existed — the review had called it "correct by inspection, but
unverified". Both halves of that were true and neither was the whole story.

**Recording details.** Key and value go in by `toString`, which the JDK does not
specify; all four strings were compared on rows 8, 11, 17 and 21 and are identical
on every one, so D7's categorical-exact rule holds across the matrix, and later
drift surfaces as a cross-JDK divergence. The two `Map`-taking setters stay
unrecorded: `Map` iteration order is unspecified, and nothing under
`thinlet-core/src/main/java` calls either today.

**The re-record is auditable rather than trusted.** 93 paint goldens (41 static +
52 interaction) each gained the two calls; the 59 layout-state sidecars record
layout, not paint calls, and are untouched. D44 and D52 forbid re-recording to make
an unexplained diff go away, so the diff was proved to be exactly the explained
one: **every one of the 93 files is `+2/-0`, the added lines across all of them
reduce to exactly two distinct strings, and not one line was removed.** A
re-record that laundered anything else could not produce that shape.

**Proven to have teeth.** Dropping a single hint from `paint` now fails **93 of 94**
golden tests, where before it failed none. `AntialiasingPersistenceTest` fails on
the 2005 code (2 hints, then 0) and passes on the fix, which is the D69 requirement
that the pinned test move to the new behavior in the same change.
(Cross-ref D86 the first instance of this blind spot, D87 the loop that found both,
D69 the change-control protocol this behavior change follows, D7 the tolerance
model, D44/D52 the re-record discipline this diff satisfies.)

## D89 — run-2's real output is two proven blind spots: an unrecorded `drawImage` source rectangle, and a workaround that hides inside the tolerance

**Date:** 2026-09-05. **Status:** accepted. **Phase:** 3c — three
behavior-preserving source slices, plus the findings that reviewing them
produced. No harness, net or behavior change here; the dispositions the
findings force are deliberately left open.

**What run-2 committed.** Three slices, three of three, capped as run-1 was:
the deprecated `Integer`/`Long` constructors retired for `valueOf` (nine
sites), `getSelectedItems`' quadratic array regrow replaced with an
`ArrayList`, and the four redundant `new String(…)` copies that sit outside
the fenced parser — the ones D86 predicted a later run would find again in
`addAttribute` and `getMethod`. Twenty insertions, twenty-one deletions across
two files. Six slices have now been produced across two runs and **the repair
path has still never executed**: no slice has yet failed verification, so that
branch of `loop-modernise.sh` remains untested code.

**Reviewed by mutation, not by inspection.** D88 records that run-1's
antialiasing slice was reviewed as a behavior-preserving tidy-up while it was
in fact repairing a live defect, and that a green net plus a plausible diff is
not a review. So every risky line these three slices touched was broken on the
committed tree, run through `local-ci.sh -t`, and reverted — fourteen runs.
The slices' own live edits are watched: perturbing `setInteger`'s boxed value
fails ten slider/spinbox tests, swapping the keystroke pack fails
`DescriptorContractTest`, perturbing the string-attribute branch fails nine
golden tests, forcing the font family fails three, and dropping items from
`getSelectedItems` fails `InputListTest`. Four probes came back green, and
those four are the finding.

**Blind spot #3: `TracingGraphics2D` records only half of a scaled blit.**
Both ten-argument `drawImage` overrides call
`recImage(dx1, dy1, dx2 - dx1, dy2 - dy1)` — the **destination** rectangle. The
four source arguments are dropped. The only caller of that form is
`Thinlet.fill`, the gradient background painter, so the sampling of the
gradient image is unobservable. Proven as a paired experiment on the same call:
moving the recorded destination argument by 40 px fails **76 of 94** golden
tests, which establishes the path is live and heavily covered; collapsing the
source rectangle to 1×1 — a flat fill where a gradient belongs — passes **94 of
94**. Same shape as D88, one method along, and the third instance of the loop
touching code the net does not watch.

**The same call is invisible a second way, and this one is new.** `Thinlet.evm`
is `-1` only under the Insignia Jeode JVM; nothing in the net sets that vendor,
so all 26 `+ evm` terms are zero in every row. That much is ordinary dead
scaffolding. What is not ordinary is that setting `evm = -1` — turning the whole
2005 workaround **on** — passes all **189** golden tests. The workaround exists
to correct a one-pixel error, and D7's numeric tolerance is ±2 px
(`trace-tolerance.json` is `defaultPx: 2.0`, no per-op overrides, applied to
every numeric argument by `TraceComparator`). The net is therefore blind to
`evm` in both directions, not merely when it is inactive. D86 named uncovered
code and D88 named unrecorded effects; this is a third category — **recorded,
executed, and still invisible because the change is smaller than the tolerance
band**. The 2005 comment above the initializer names the exact call it hides
behind: *"EVM has larger fillRect, fillOval, and drawImage(part)"*.

**The portability inventory inherited the blindness.**
`project-docs/backend-portability/RENDERING-PRIMITIVES.md` was curated from the
traces (D34), so where the trace is silent the document is confident and wrong:
it lists `drawImage` as `x, y, w, h`, describes it as an icon/glyph blit, and
gives `drawImage(img, x, y, w, h)` as the Canvas equivalent. A histogram of the
committed goldens puts 2 920 of 3 887 `drawImage` ops at exactly 15×15 with a
ragged tail of partial widths at height 15 — the signature of `fill` tiling a
`block`-sized gradient (`block = getFontMetrics(font).getHeight()`), not of
icons. A backend written to that row renders flat blocks where the 2005 UI has
gradients. The row is corrected in this change; the trace it was derived from
is not.

**What is not decided here.** Whether the harness should record the source
rectangle (it should, and it costs a re-record of every golden carrying
`drawImage`, under the D44/D52 audit discipline); whether the tolerance model
needs a per-op exact rule for arguments that are known to be small; and whether
the Insignia EVM workaround survives at all on the enhanced line. The last is
now a ROADMAP 3c backlog item alongside the parser question, for the same
reason: there is no sense modernising 26 sites that may be deleted, and
deleting them is a D69 behavior change the net cannot referee.

**One incidental, recorded once.** The declared integer defaults in
`DescriptorTable` are duplicated as literals at every internal call site
(`getInteger(component, "maximum", 100)`, `getInteger(column, "width", 80)`,
`getInteger(component, "unit", 5)`), so the table's value is consulted only by
the public two-argument getters and by the setters' remove-at-default rule.
That is why mutating three of those defaults changes nothing observable, and it
is a second source of truth that can drift silently. `DescriptorContractTest`
pins the pattern for `colspan` and `mnemonic` only.
(Cross-ref D86 the first blind spot and the parser fence, D88 the second and the
`setRenderingHint` precedent for closing one, D87 the loop and its still-unrun
repair path, D7 the tolerance model this entry stresses, D34 the curation that
inherited the gap, D65 the Drafts playthrough that turned out to be the only
teeth on `putProperty` and the constant-argument binding.)

## D90 — the build measures coverage; the blind-spot hunt gets an exhaustive answer for one of its three tiers

**Date:** 2026-09-05. **Status:** accepted. **Phase:** 3c (build tooling +
findings; no library, behavior or golden change).

**Why now.** Two runs of `loop-modernise` produced four net gaps between them
(D86, D88, D89 × 2) and every one was found by accident — by a slice happening to
land on unwatched code, or by an audit prompted after the fact. The build carried
no coverage instrumentation at all, so "does anything execute this?" had no cheap
answer and was repeatedly answered by shipping a change and seeing what happened.
That is the wrong order.

**What was added.** JaCoCo behind an opt-in `coverage` profile, plus
`scripts/coverage.sh` (run the net under the agent, then report) and
`scripts/coverage-summary.py` (per-class table, worst first; `--uncovered` lists
every method with zero covered instructions). It **reports and does not
threshold**: no gating row runs it, and choosing a coverage floor is a separate
decision that would need its own entry. The exec file is one shared file at the
reactor root with the agent's default `append`, so the `thinlet-drafts`
playthrough counts towards `thinlet-core`'s numbers — D89 found lines whose only
teeth are in that module, and a per-module report would have scored them dead.

**One build trap, recorded because it fails silently.** Both test modules set an
explicit surefire `<argLine>`, which shadows the agent's default `argLine`
property, so the agent is exported as `jacocoArgLine` and each `argLine` prepends
it. It must be prepended with surefire's **late-binding `@{jacocoArgLine}`**, not
`${jacocoArgLine}`: the `${}` form is interpolated against the model before
`prepare-agent` runs, expands to the empty default, and the build then succeeds,
runs every test green, and writes **no exec file at all**. The first run here did
exactly that.

**The baseline, over the whole net.** `thinlet-core` is at **85.9 % instructions,
74.0 % branches, 89.2 % methods** — 27 methods never entered, 1 103 branches never
taken. Per class: `FrameLauncher` **0.0 %** (262 instructions, 52 branches — the
entire class), `Thinlet` 84.2 % / 74.1 %, `Renderer` 90.1 % / 78.1 %, and every
other class 100 %.

**Tier 1 — code nothing executes — is now answered exhaustively.**
`FrameLauncher` is a `public` class with a `public` constructor in the published
jar, gated by japicmp, and no test has ever instantiated it; its ten methods are
the largest single block of dead coverage. In `Thinlet` the never-entered methods
include three user-facing behaviors, not just accessors: `findText` (100
instructions — the type-ahead that jumps a list, tree or combobox to the item
starting with the key just pressed), `selectAll`, and `hasAccelerator` — so
**pressing a menu accelerator has never been exercised**, although the corpus
declares accelerators and the golden net paints their labels. The public
`getItems`, the `setFont`/`getFont`/`setComponent`/`getComponent` pairs,
`getPreferredSize`, `update(Graphics)`, `handleException` and `destroy` are also
never entered. The three SAX callback bodies show as dead only because the base
implementations are empty and `ParserSaxModeTest` overrides them (D86); that is
not a gap. Among partially covered methods the worst ratios are `changeCheck`
(2 branches covered, 14 missed), `getListItem` (15/27), `processList` (15/25),
`getChars` (14/22) and both `popup` methods.

**Coverage does not answer the other two tiers, and the D89 findings prove it.**
Every gap D89 recorded sits on a line JaCoCo scores as covered: the gradient blit
runs on nearly every paint, `getSelectedItems` is called by `InputListTest`, and
the `evm` terms execute constantly. Coverage answers *"did this line run?"*;
neither of the other two questions — *"was its effect recorded?"* and *"was the
recorded difference larger than the tolerance?"* — is visible to it. A green
coverage number is therefore not a regression net, and the mutation probe stays
the only instrument that settles the other two.

**Tier 2 gains a second confirmed instance: the trace records image geometry, not
image identity.** `recImage` stores position and size, so painting entirely
different pixels at the same place and size is invisible. Proven by swapping
`hgradient` and `vgradient` at their two call sites in `Thinlet.fill` — same
dimensions, opposite pixel content, every gradient in the UI then running the
wrong way: **94 of 94 golden tests pass**. This is not a defect in the recorder's
design (hashing pixels would be JDK-variable and would defeat D7's whole
tolerance model) but it is a boundary worth stating, and it bears directly on
KNOWN-QUIRKS Q3 step 2 — a supplied missing-image placeholder drawn at the size
of the icon it replaces would not move a single golden.

**Tier 3's band is now measured rather than inferred.** D89 showed the whole
Insignia workaround hides inside the tolerance. The boundary is exactly where
`defaultPx: 2.0` puts it: shifting every text label in the UI by **+2 px passes**
all 41 static golden tests, and by **+3 px fails 39 of them**. So the net's
guarantee on any single coordinate is "within two pixels of 2005", and a change
that stays inside that band is unreviewable by the goldens however many of them
there are.

**What this entry does not do.** It fixes nothing. `FrameLauncher`, `findText`,
`selectAll` and accelerator dispatch are recorded as uncovered, not scheduled;
recording the `drawImage` source rectangle (D89) still costs a re-record of every
golden carrying the op; and no coverage threshold is set. The living inventory is
the report itself — `scripts/coverage.sh` regenerates it — so these numbers are a
dated baseline here, not a document to maintain.
(Cross-ref D89 the two gaps that motivated this and the mutation discipline it
cannot replace, D86 and D88 the two before them, D65 the playthrough the shared
exec file exists to include, D7 the tolerance model tier 3 measures, D31 the
cross-JDK model this profile deliberately stays out of.)

## D91 — the pure-logic surface becomes package-private static seams, so tests can reach it

**Date:** 2026-09-05. **Status:** accepted. **Phase:** 3c — preparatory. No
behavior change, no API change, no golden re-record.

**Why now.** D90 measured the net at 85.9 % instructions / 74.0 % branches and
named what it could not reach: `hasAccelerator` (accelerator dispatch never
exercised), `findText`, `changeCheck` (2 of 16 branches), `getListItem` (15 of
27), `processList` (15 of 25), `getChars` (14 of 22 branches missed). Every one
of them is `private`. Tests live in package `thinlet`, so they reach
package-private, protected and public members — **not private** — and the only
way to drive these today is through an input event, which is precisely the
heavyweight style a plain unit test is meant to avoid. The charter's *"where the
net is thin, shore up coverage first"* had no cheap route to these methods.

**What changed.** 42 method names in `Thinlet.java` are now package-private
`static`, in three tiers:

- **Tier 0 — already `static`, merely `private`** (8): `filter`, `createImpl`,
  `set`, `getItemCountImpl`, `getItemImpl`, `getDefinition`, `get`,
  `logIconAttempt`. One word each, no body movement.
- **Tier 1 — pure bodies, no `Thinlet` parameter needed** (12): including
  `hasAccelerator`, `instance`, `getSum`, `getIndex`, `insertItem`,
  `removeItemImpl`, `addImpl`, `setKeystrokeImpl` and the three 4-argument
  `setChoice`/`setBoolean`/`setInteger`. Declaration-only: private methods are
  non-virtual, so `static` cannot move dispatch, and call sites need no receiver.
- **Tier 2 — an explicit `Thinlet t` threaded through** (22): the D48 seam style
  `Renderer` already uses (`static void label(Thinlet t, …)`), covering the
  selection family, the keyboard-navigation family, the text/geometry family and
  the element/method-binding family.

**Why this is cheap, measured rather than assumed.** 25 of the 27 candidate
methods reference **zero instance fields** — the only exceptions are
`addAttribute` (`resourcebundle`) and `update` (`content`, already
package-private). `Thinlet` keeps almost all of its state in the `Object[]` model
that is passed *in* as a parameter, not in fields, which is why both production
forks were able to decouple it with static utilities (D48).

**The D52 risk does not transfer, and the audit proves it.** D52 records this
repo's only refactoring regression: a blanket regex rewrote `"font"` into
`"t.font"` **inside a string literal**, and it compiled. Threading `t` here is
*receiver insertion before a call* (`t.repaint(component, null, item)`), never
identifier renaming, so a missed insertion inside a `static` body is a **compile
error rather than a silent behavior change** — which is how `findScroll` was
caught reading `insidepart` and `block` after being mis-classified as pure. The
transformation ran on string- and char-literal-masked segments only, and the
round-trip audit (D56) is decisive: **all 1 294 string literals in `Thinlet.java`
are byte-identical to `main`.** Comments were *not* masked, which the audit caught
— one trailing comment was rewritten to `// component -> t.getParent(lead)` and has
been reverted, so all 537 line comments are byte-identical too. Mask comments as
well as literals in any future pass.

**One trap, recorded because a future scripted pass will hit it.** Converting one
overload rewrites *call sites* of that name everywhere — including the other
overload's own **declaration**, which matches the same call pattern. Both
`findScroll` declarations were corrupted (`findScroll(t, Thinlet t, …)` and
`findScroll(Thinlet t, this, …)`) and their six shared call sites were
double-threaded. Overloaded names must be converted one at a time, with the
declaration lines excluded from call-site rewriting.

**What is deliberately not extracted.** `parse(InputStream, char, Object)` — it is
already reachable through three public entry points and pinned by D86's
`ParserSaxModeTest`/`ParserDomModeTest`, so extraction buys no reachability, and
the open ROADMAP 3c question about whether the hand-rolled parser survives at all
makes it the worst place to spend risk. Also left alone: `getSize` (23 call sites,
and the name collides with the inherited `Component.getSize()`), `setRectangle`
(19 sites, a trivial setter), `update` (14 sites) and `findComponent`. These are
recorded as remaining, not scheduled.

**Not a Cut 4/5/6 seam commitment.** In-place staticisation creates no new file
and chooses no subsystem boundary — it is the reversible half of the seam work,
so it does not touch what the fork mapping gates (D48/D50/D61/D69). The maintainer
confirmed that reading in-session before the work started.

**The gate.** All four JDK rows green (383 core + 13 drafts on the base row; 380
on the crossjdk rows, which exclude `robot`), **zero golden re-records**, and
japicmp reports zero `MODIFIED`/`REMOVED`/`NEW` public API entries against a
resolved v0.1.0 baseline jar.

**What this unblocks.** Step 0 of the characterization-loop plan: a
`loop-characterise.sh` that writes plain-JUnit tests against this surface, gated
by mutation testing rather than by coverage delta, with `guard_no_main` absolute
so it can never make a test pass by editing the code under test.
(Cross-ref D90 the coverage baseline that named these methods, D48 the seam style
and the two forks that validate it, D52/D56 the regression this avoids and the
audit discipline, D86 the parser net and the fence, D43 the visibility discipline
that keeps these package-private, D69 the enhanced-line protocol.)

## D92 — mutation testing becomes the teeth gate; the spike found two silent-success traps and corrected the gate's design

**Date:** 2026-09-05. **Status:** accepted. **Phase:** 3c — build tooling. No
library, behavior or golden change.

**Why.** D90 established that coverage answers only *"did this line run?"* and
that the two questions it cannot reach — *"was the effect recorded?"* and *"was
the difference larger than the tolerance?"* — are settled only by mutation. D89
did exactly that by hand, fourteen runs, and called the four green probes the
finding. This makes the instrument routine.

**Why PIT rather than automating D89's hand-mutant discipline.** The obvious move
is to have each pass emit a mutant manifest alongside its test and check the test
fails against each. That is the wrong instrument for an autonomous loop: **a pass
that authors both the test and the mutants grades its own homework** — it can pick
mutants its shallow test happens to catch, and catching a shallow test is the
gate's entire purpose. PIT derives mutants mechanically from bytecode and cannot
be gamed.

**What was added.** `pitest-maven` 1.19.1 + `pitest-junit5-plugin` 1.2.2 behind an
opt-in `mutation` profile, `scripts/mutation.sh` (scoped to one test class) and
`scripts/mutation-summary.py`. It **reports and never gates**: no CI row runs it,
and the default build is unchanged (383 core + 13 drafts still green). The script
mirrors `coverage.sh` — image read out of `local-ci.sh` so the two cannot drift,
container-only for the pinned fonts and Xvfb (D44), writes only under `target/`.

**Trap 1: `targetTests` matches fully-qualified names.** `ParserSaxModeTest`
matched nothing. PIT reported **4 426 mutants, 0 killed, every one NO_COVERAGE,
score 0.0 %** — and the build succeeded.

**Trap 2: PIT auto-adds `java.awt.headless=true`.** Its own log names the plugin —
*"Auto add java.awt.headless=true to keep keyboard focus on Mac OS"*. Every test
that touches `Thinlet` needs the real Xvfb `:99` display (D22), so the coverage
minion collected nothing, PIT reported *"Calculated coverage in 0 seconds"*, and
**the build succeeded again**. Fixed with an explicit `-Djava.awt.headless=false`
ahead of the harness JVM args.

Both traps produce a green run that measured nothing — the same shape as D90's
`${jacocoArgLine}` trap, and the third time this repo has been handed a passing
build with no data behind it. `mutation-summary.py` therefore **exits non-zero
when no mutant ran at all**, and says so in words rather than printing 0 %.

**The measured correction to the gate's design.** With PIT working, an A/B on the
same parser paths:

| Test | Covered mutants | Killed | Survived | Score |
|---|---|---|---|---|
| `ParserSaxModeTest` (9 assertions) | 95 | 67 | 28 | **70.5 %** |
| A probe asserting **nothing** | 92 | 46 | 46 | **50.0 %** |

An assertion-free test still kills **half** the mutants, because PIT scores a
thrown exception as a kill and many parser mutations simply crash. **An
assertion-free test is a crash test, not a no-op** — so roughly 50 % is the floor
for merely executing the code, and a flat "score ≥ N %" threshold is a weak gate
that a toothless test can clear. The gate the loop will use instead is scoped to
the slice's assigned target: **within the target method's mutants, require killed
> 0 and survived == 0.** The crash floor buys nothing there, because a surviving
mutant in the assigned method is by definition an unwatched line the slice claimed.

**One incidental finding, recorded and not scheduled.** `ParserSaxModeTest` leaves
**28 survivors**, of which 8 are negated conditionals inside `parse` itself — the
D86 net executes those branches without watching them. That is a fourth instance
of the D86/D88/D89 pattern, found in the first five minutes the instrument existed.
(Cross-ref D90 the coverage baseline and the tier model this completes, D89 the
hand-mutation discipline it automates, D86 the parser net whose survivors it just
measured, D44 the container discipline, D22 the display the headless trap broke.)

## D93 — `loop-characterise.sh`: the test-writing loop, and the guard bug a deliberate-violation test caught

**Date:** 2026-09-05. **Status:** accepted. **Phase:** 3c — tooling. No library,
behavior or golden change.

**What it is.** `scripts/loop-characterise.sh` is `loop-modernise.sh`'s mirror
image: same skeleton (whole body in `main()`, per-worktree `flock` on the resolved
`--git-dir`, `run_logged`, scoped rollback, resumed session, `[N]` / `--new` /
`--dry-run`), **scope inverted**.

| | `loop-modernise` | `loop-characterise` |
|---|---|---|
| Writable tree | `thinlet-core/src/main/java` | `thinlet-core/src/test/java` (+ `KNOWN-QUIRKS.md`, append-only) |
| New files | forbidden | **required** — exactly one per slice |
| Existing files | edited in place | **never modified** |
| `japicmp` | every pass | dropped — `guard_no_main` already proves it |

**Why `guard_no_main` is the load-bearing one.** A loop that can edit both the
code and its tests can make a test pass by changing the code, and **no gate in
this design would catch that** — the mutation gate grades the test, not the
diff. D91 exists so this guard can be absolute rather than negotiable.

**The guard bug, and why the plan insisted on testing guards by deliberate
violation.** `guard_no_main` was written with the pathspec `'*/src/main/java'`
and **silently passed a modified `Renderer.java`**. A git pathspec containing a
wildcard is a wildmatch against the *whole path*, so that pattern matches the
directory name and nothing beneath it. Measured across four candidates: bare
`*/src/main/java` **misses**; `*/src/main/java/*`, `:(glob)*/src/main/java/**`
and `*src/main/java*` all catch. The guard now uses the explicit glob form, and
also reverts main source itself — preflight proves the tree was clean, so
anything there is the slice's, and stopping the run while leaving the library
edited would be the worst of both outcomes. All five guards were then re-tested
by deliberate violation and every one exits non-zero and leaves the tree clean.
**A guard that has never been fired is not a guard**; `loop-modernise`'s repair
path has still never executed across six slices (D89), which is the same gap one
step along.

**Target selection is data, not judgment.** `coverage-summary.py` gains a
`WORKLIST=true` mode (and `coverage.sh --worklist`) emitting, per partially
covered method, the exact source lines JaCoCo still records as missed —
attributed by method line-range, cross-checked against JaCoCo's own per-method
counters for all **125** methods with **zero mismatches**. The loop intersects
that with a curated pure-logic allowlist: **29 targets today**, worst branch
coverage first, `parse` and `getListItem` and `processList` and `findText` and
`getChars` and `changeCheck` at the top. The allowlist decides what is in scope;
coverage decides the order and supplies the lines handed to the slice. A pass
that cannot reach its target without an AWT event writes its reason to
`.characterise-declined` and the loop moves on — that file becomes the worklist
for the later event-driven phase rather than being guessed at now.

**The gate, and its honest limit.** Per D92 the condition is scoped to the
slice's assigned method: **at least one mutant killed, and zero survivors.** That
is a statement about the *quality* of what the test covers, not the *quantity* —
a test reaching one line of a large method can pass it. Quantity is measured
separately by the coverage delta the loop reports at the end of a run. Splitting
the two is deliberate and follows D90: coverage answers *"did it run?"*, mutation
answers *"was it watched?"*, and neither substitutes for the other.

**Not yet run end to end.** The guards, the worklist, the gate and the summariser
are each tested; no slice has been generated. `--dry-run` produces one, verified
and uncommitted, for review before anything is committed.
(Cross-ref D92 the gate and the measurement that shaped it, D91 the extraction
that makes `guard_no_main` affordable, D90 the coverage instrument the worklist
reads, D87 the loop this mirrors, D44/D52 the frozen-fixture rule the
new-files-only guard enforces.)
