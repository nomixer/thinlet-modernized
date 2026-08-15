# This repository — refactoring guardrails for Thinlet

Pointer-shaped by design (repo policy D57, "single home per fact"): this file
*cites* the repo's own docs instead of copying them, so it can't drift as the
protocol advances. When it names a doc, go read that doc for today's truth.

## Floor

**Java 8.** Source of truth: `maven.compiler.release` in `pom.xml`. Compilation
runs on JDK 21 with `--release 8`, so an accidental post-8 API compiles for the
author and breaks the release — treat the Java-8 checklist in
`references/java-versions.md` as a hard gate. The floor moves deliberately (the
project plan walks 8 → 11 → 17 → 21); confirm the current floor from the POM each
session rather than assuming.

## How it's built and how regressions are caught

- **Build / verify:** `./mvnw -B verify` (Spotless/palantir-java-format +
  Checkstyle + SpotBugs gate). See `CLAUDE.md` → "Key constraints".
- **Faithful local CI:** `.devcontainer/ci/local-ci.sh` runs the net inside the
  exact CI container. **Never trust golden diffs from the bare host** — the
  regression net is font/JDK-sensitive (see `CLAUDE.md`).
- **Regression net = behavior contract:** golden paint + layout traces over the
  vendored corpus, plus the `@Tag("input")` input-capture suites. This is what
  makes "observably null" checkable — a moved golden or a red input test means
  you changed behavior, not refactored. Details:
  `project-docs/backend-portability/` and `KNOWN-QUIRKS.md`.

## Behavior-change protocol (when a "refactor" is actually a change)

This repo does **not** allow behavior changes to sneak in under cleanup. A change
that alters observable behavior goes through the recorded-disposition protocol:
record the disposition → flip the pinned test to the new behavior in the same PR →
re-record only the affected goldens, citing the authorizing decision. Authority
and current rhythm:

- **`DECISIONS.md`** — the append-only decision log; the authority. Cite the
  newest relevant D-number.
- **`CLAUDE.md` → "Current work"** and **`.claude/NEXT-STEPS.md`** — the live
  floor target, the active track, and the ordered next work. Read these for
  what "now" means.

If your proposed cleanup crosses any behavior-preservation line (see SKILL.md),
it is a disposition, not a slice — route it there.

## Load-bearing traps specific to `Thinlet.java`

The library is a single ~6000-line class in 2003 style (`Object[]` widget model,
`import java.awt.*`, package-private fields). The ugliness is frequently
load-bearing. Known traps a naive modernizer will hit:

- **Interned-token `==` identity (D42/D43).** Comparisons like `entry[0] == key`
  and the `is(token, literal)` helper are deliberate identity checks on interned
  strings, pinned by `InternTripwireTest`. **Never** rewrite `==` to `.equals()`
  here — it breaks the contract and the tripwire throws.
- **The `Object[]` widget model is the representation, not an accident.** Widgets
  are `Object[]` rows walked via a `next` pointer at `entry[2]`; the class is
  `Serializable`. "Map-ifying" it, or turning the walk into a stream, changes the
  serialization shape and the `Renderer` seam — a cross-cutting behavior change,
  never a slice. The model schema is documented above `createImpl` in
  `Thinlet.java` (D57).
- **Package-private seams are intentional (D48).** Fields exposed to `Renderer`
  are package-private on purpose and japicmp-invisible; don't "encapsulate" them
  behind accessors without checking the seam.
- **Hot paint/layout paths.** Rendering and layout run per frame; the false-gain
  stream/allocation cautions in SKILL.md apply with force here.

## Before opening a PR

Java changes require the D57/D60 **comment pass** before `gh pr create` (a hook
enforces it). Run `scripts/comment-pass.sh` to see the checklist + changed files,
do the review, then `scripts/comment-pass.sh done`. See `CLAUDE.md` → "Working
agreements".

## What a good slice looks like here

Small, one concern, floor-8-legal, observably null against the golden + input
net, verified via `.devcontainer/ci/local-ci.sh`, and documented where the repo
expects it. When in genuine doubt whether something is a refactor or a change,
treat it as a change and cite `DECISIONS.md`.
