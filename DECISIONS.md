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
  accepted for the Phase-0 baseline and earmarked for `KNOWN_QUIRKS.md` entries
  locked by tests in Phase 1 — not fixed in Phase 0.

As internal refactors (Phase 3) and Enhanced Thinlet address these, exclude
entries are removed so the linters fail on regressions again.

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
