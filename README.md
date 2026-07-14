# thinlet-modernized

> **Thinlet** was created by **Robert Bajzat** (2002–2005) — a small,
> dependency-free, XML-driven Swing-on-AWT GUI toolkit. Original project:
> **https://thinlet.sourceforge.net/**. All credit for the library belongs to
> him; this repository is an unaffiliated modernization fork.

A modernization fork of the 2005 Thinlet GUI toolkit (`thinlet-2005-03-28`).
The goal is to bring the original library back as a usable, dependency-free
Java 8 baseline that runs cleanly across modern JDKs — **without changing its
observable behavior**.

## Project posture

- **Public artifacts, unsupported.** Published to GitHub Packages, but with no
  expectation of users, contributors, or third-party adoption. Free; caveat
  emptor. No SLA, no commitment to triage outside issues.
- **Modernize first, enhance later.** This project preserves 2005 Thinlet's
  behavior and modernizes the toolchain around it. User-visible changes are out
  of scope and deferred to a future "Enhanced Thinlet" effort.
- **Quality over speed.** The elaborated toolchain (Spotless, Checkstyle,
  SpotBugs, japicmp, golden traces, Dev Containers) exists because we want a
  model project, not because of market pressure.

The original library is preserved verbatim in the frozen
[`nomixer/thinlet-archive`](https://github.com/nomixer/thinlet-archive) repo.

## JDK matrix

A single Java-8-targeted artifact is validated to behave identically —
*within a defined metric tolerance* (see below) — across:

| JDK | 8 | 11 | 17 | 21 |
|-----|---|----|----|----|

The build runs on a modern LTS JDK (21) and targets Java 8 bytecode via
`--release 8`; CI then runs that one jar's test suite on real JDK 8/11/17
runtimes via Maven toolchains, with the JDK 21 runtime covered by the build job
itself — so the matrix is *runtime* coverage, not a per-JDK rebuild. (JDK 25 is
deferred for now.)

**Why one jar instead of one per JDK.** From a single Java-8 source, compiling
for a newer JDK yields a *behavior-identical* jar that is merely **less**
portable (it won't load on older JVMs) — no speed or behavior gain. So the
deliverable stays one maximally-portable Java-8 jar; what's worth proving is that
it *runs* the same across JDKs, which is a property of the test runtime, not the
bytecode level. Per-JDK artifacts only become worthwhile once the source actually
differs per version ("Enhanced Thinlet", Phase 3). This is also why the **build**
tooling (formatter, Checkstyle, etc.) can track the latest releases freely, while
the **test** libraries (JUnit, AssertJ) are pinned to the majors that still run
on the oldest test JDK — JUnit 5.x / AssertJ 3.x. See `DECISIONS.md` (D31).

**"Identical within tolerance," not "byte-identical."** Pinned fonts fix the
glyph source, but the JDK's pixel-metric math (`FontMetrics` etc.) can differ
across JDKs. The regression gate compares method/arg *structure* and
*categorical* values exactly, and numeric coordinate/size values within a
configured pixel tolerance (default ±2 px). See `DECISIONS.md` (D7).

## Modules

```
thinlet-modernized/
├── pom.xml            parent: versioning + plugin management
├── thinlet-core/      the library JAR — NO runtime deps; PUBLISHED to GitHub Packages
├── thinlet-demos/     example apps; depends on thinlet-core; NOT published
└── thinlet-drafts/    draft samples; depends on thinlet-core; NOT published
```

`thinlet-core` is dependency-free at runtime. Only `thinlet-core` publishes —
the library jar plus a `tests`-classifier jar (the reusable test harness, see
`DECISIONS.md` D65). `thinlet-demos` and `thinlet-drafts` are reactor modules
for in-repo examples and the consumer-compat CI job; `thinlet-drafts` also
hosts the live-app playthrough test suite, which consumes that test-jar.

## Building

Use the Maven wrapper (no system Maven required):

```sh
./mvnw -B verify
```

`verify` runs compile + Spotless check + Checkstyle + SpotBugs + tests +
packaging. To auto-format before committing:

```sh
./mvnw spotless:apply
```

A Dev Container (`.devcontainer/`) provides a pinned JDK, Xvfb (for headless
AWT), a fixed font set, and a working `pre-commit` so local and CI runs match.
Inside it, `mvn` is a shim for `./mvnw`, so the CLI uses the exact pinned Maven
version CI runs. A noVNC desktop (Fluxbox) is served in the browser at forwarded
port **6080** (password `vscode`) for visually running the demos — the default
`DISPLAY` (`:1`) targets it, while the automated trace tests use a separate
controlled headless display (`:99`). See `DECISIONS.md` D22.

To launch the bundled 2005 example apps (`thinlet-demos` + `thinlet-drafts`), use
`scripts/example.sh` (run it with no argument to list them); it builds the module
incrementally and launches — no separate build step. Full guide in
`project-docs/RUNNING-EXAMPLES.md`.

For a **faithful local CI run without VS Code** — the same golden/input net CI
executes, against the same pinned fonts and Xvfb (bare-host runs produce false
±2 px trace diffs) — use the helper script, which runs the build inside the
exact CI dev-container image published to GHCR (Docker required; see
`DECISIONS.md` D44):

```sh
.devcontainer/ci/local-ci.sh      # JDK-21 full verify (lint + traces + input net)
.devcontainer/ci/local-ci.sh 8    # cross-JDK row: tests forked on JDK 8 (also 11, 17)
```

> **Open the Dev Container on a clone, not a linked `git worktree`.** A
> worktree's `.git` is a pointer into the *main* repository, which is not
> mounted into the container — so git is non-functional inside it (Source
> Control, commits, and `pre-commit` all fail with `fatal: not a git
> repository`). Use a normal clone, or `git clone --branch <branch>` into its
> own folder, and open that. See `DECISIONS.md` D20.

## Consuming `thinlet-core` (GitHub Packages auth required)

`thinlet-core` is published to **GitHub Packages**, which **requires
authentication even for public reads**. Consumers need a GitHub token with
`read:packages` and a `~/.m2/settings.xml` server entry:

```xml
<settings>
  <servers>
    <server>
      <id>github-nomixer</id>
      <username>YOUR_GITHUB_USERNAME</username>
      <password>YOUR_GITHUB_TOKEN_WITH_read:packages</password>
    </server>
  </servers>
</settings>
```

…with a matching `<repository>` pointing at
`https://maven.pkg.github.com/nomixer/thinlet-modernized`. Coordinates:

```xml
<dependency>
  <groupId>com.nomixer.thinlet</groupId>
  <artifactId>thinlet-core</artifactId>
  <version>0.1.0</version>
</dependency>
```

(`0.1.0` is the first published release; the current `main` may be pre-release.)

## Releasing (maintainers)

Releases are tag-driven and publish `thinlet-core` (plus the parent POM it
depends on) to GitHub Packages. `main` stays on `-SNAPSHOT` — the release version
comes from the tag, so there is no release commit to manage.

1. Make sure `main` is green and points at the commit you want to release.
2. Push a version tag, e.g.:
   ```sh
   git tag -a v0.1.0 -m "thinlet-core 0.1.0" <main-sha>
   git push origin v0.1.0
   ```
3. The **Release** workflow (`.github/workflows/release.yml`) derives the version
   from the tag (`vX.Y.Z` → `X.Y.Z`) and runs `mvn deploy`, publishing to GitHub
   Packages.

API-compatibility checking (japicmp) activates after `v0.1.0` exists, comparing
later releases against the published baseline (see `DECISIONS.md` D10/D28).

## License

LGPL 2.1 — see [`LICENSE`](LICENSE). The original LGPL header notices in the
source files are preserved verbatim and no new copyright is claimed over the
original code. See `AUTHORS` for attribution and `DECISIONS.md` for the
rationale behind the toolchain and branching choices.
