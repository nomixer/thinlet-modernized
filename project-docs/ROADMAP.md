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
  locked as `KNOWN_QUIRKS` Q1 (PR #10).
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

## Phase 2 — Cross-JDK matrix + backend-portability docs ⬜

- Expand the execution matrix to JDK 11 / 17 / 21 / 25 rows (after the JDK-8 row,
  D14), with per-signature `trace-tolerance.json` tuning where cross-JDK metrics
  require it (D7).
- Cross-JDK trace diff.
- A `trace-curator` agent populates `project-docs/backend-portability/`
  (rendering primitives, layout algorithms, input surface) from the trace JSON.

## Phase 3 — Internal refactors / Enhanced Thinlet ⬜

- Remove SpotBugs exclusions as the code is cleaned, so the linters fail on
  regressions again (D13).
- Deliberately fix the locked quirks per their `KNOWN_QUIRKS` dispositions (e.g.
  the parser null-source NPE; the `FileChooser` fallback null deref).
- HiDPI / alternative rendering backends, informed by the backend-portability
  docs.
