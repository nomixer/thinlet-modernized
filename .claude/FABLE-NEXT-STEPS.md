# Phase 3 handoff / review brief — for Fable

**Written:** 2026-07-07 by Opus 4.8 (1M), end of session.
**Audience:** Fable, asked to review this work.
**Status of the code:** Cut 1 done, CI-green, and **merged into `main`** (squash `7796f79`).
**How to review the change:** everything is on `main` — review there. `git show 7796f79` is
the exact Cut 1 diff.
**This file:** a deletable Claude meta/handoff doc (lives under `.claude/`, per the D27
doc-layout rule). Move to repo root if you'd rather.

---

## 0. TL;DR

We opened **Phase 3** of the Thinlet modernization: restructuring the 7,779-line
`Thinlet.java` God class into idiomatic modern Java **behind the existing golden-trace +
input-capture net**, holding 2005 observable behavior *and* the public API constant. This
is a deliberate change of direction from the prior "modernize the toolchain, **not** the
library" posture — recorded as **DECISIONS.md D42**.

**Cut 1 is complete and CI-green**: we neutralized the interned-`String` `==` contract
(449 identity comparisons — reviewed count, see §10) behind one helper, as the enabling prerequisite for every later
typed refactor. It is **provably behavior- and API-preserving** (golden + input net passed
on JDK 8/11/17/21; japicmp passed). It **merged into `main`** via **PR #38** (squash
`7796f79`); `main` now contains Cut 1.

---

## 1. Context — the project and the pivot

- **Project:** modernization fork of the 2002–2005 Thinlet GUI toolkit. Goal historically:
  *modernize the toolchain, not the library — preserve 2005 behavior exactly.* Phases 0–2
  built the safety net (golden-trace paint+layout recorder `TracingGraphics2D` + a
  `LayoutTrace`; an input-capture harness `InputDriver` driving real AWT events through
  `processEvent`; a cross-JDK 8/11/17/21 CI matrix on pinned fonts + Xvfb `:99`).
- **The maintainer's actual goal (this session):** **modernise, then enhance.** Restructure
  the library internals into a clean, idiomatic base, *then* re-implement — cleanly — prior
  production enhancements. Those enhancements exist as **two custom `Thinlet.java` forks +
  their apps** (battle-tested solo-supporting a global investment bank, ~2006+; "functional
  but hacky, never given real thought"). The apps aren't runnable here (proprietary deps),
  but the forks are **diffable against the 2005 baseline** to derive the enhancement backlog.
- **Constraints:** **Java 8 floor** (enterprise/old-JRE reality). Behavior **and** public API
  held constant through the modernise phase. Full idiomatic rewrite / new API is a *later*
  step (after real apps run on the clean base).

This pivot is **D42**. It supersedes the "toolchain not library" posture *and* the "consult
before opening a PR" note, for Phase 3 onward.

---

## 2. Readiness assessment — the analytical basis (please sanity-check)

Three parallel deep-reads of `Thinlet.java` by subsystem produced this. **Net strength
drives the refactor order.**

- **The dominant obstacle:** an **interned-`String` `==` contract** — 449 identity
  comparisons (reviewed count, §10) (`"combobox" == classname`, `placement == "top"`, `part == "down"`,
  `getClass(x) == "..."`, `definition[0] == "..."`). Correctness silently depends on the
  strings being interned (DTD literal pool + `create()` re-canonicalization + SAX/DOM
  interning). **Any typed/enum refactor flips these to `false` with no compile error and no
  test** for most widgets. Neutralizing it is the prerequisite for everything else.
- **Paint** (~L1537–3533): near-pure sink; the golden net captures its *entire* observable
  output (the primitive stream, ±2 px, 41/42 corpus files — corrected, §10). **Safe first modernisation cut.**
  Caveat: interaction-state branches (hover/press/focus/selection/caret) are **untraced** —
  goldens are static renders. Two stray model writes to relocate first: the `:lead` write
  (~L2962) and the lazy-layout negative-width kick (~L1618–1621, paint re-runs `doLayout`
  mid-render).
- **Model/DTD/parse:** the DTD-descriptor layer + typed accessor façade + parse chain are a
  **good early cut** (best net coverage; single chokepoint `getDefinition`; public signatures
  preserved). The `Object[]{key,value,next}` assoc-list swap is the **worst** cut (get/set are
  the most-called methods; reserved geometry keys co-mingled with attributes) → **late**.
- **Layout** (~L193–1532): a **hub** — `bounds` + `:port/:view/:widths/:offset` feed paint,
  repaint offset math, scrolling, hit-testing. Only `bounds` is traced directly; the other
  stashed rectangles are covered *indirectly*. **Second**, after paint stops reaching back
  into the model.
- **Event/input/focus** (~L726–1007, 3539–5866): a **15-field transient interaction bus**
  (`mouseinside`, `insidepart`, `mousepressed`, `focusowner`, `popupowner`, `timer`,
  `findprefix`, …) that paint+layout also read; enter/exit is *synthesized* by diffing fields;
  `findComponent` is a hit-test that mutates state. **Thinnest net** — menus, spinner,
  tooltip, slider, tabbedpane, dialog drag/resize, scrollbar-mouse, context-menu,
  focus-traversal, clipboard are all **unasserted**. **Last;** backfill characterization tests
  first; safest internal seam is the `invoke`/`invokeImpl` callback boundary.

Full plan file: `~/.claude/plans/pure-singing-tiger.md` (Opus session plan; not in-repo).

---

## 3. The sequenced plan (D42)

1. **Cut 1 — neutralise the `==` contract.** ✅ **DONE (this session), CI-green, PR #38.**
2. **Cut 2 — paint → typed Renderer**, one widget branch at a time (net captures the full
   primitive stream). Relocate the two stray paint-side writes first. Prereq for the
   interaction-state branches: **generate interaction-state paint goldens** (drive events via
   `InputDriver`, re-trace the repaint).
3. **Cut 3 — DTD → typed descriptors + accessor-façade cleanup** (behind `getDefinition`'s
   4-tuple; public signatures unchanged).
4. **Cut 4 — layout → per-widget strategies** (hub; second). Prereq: extend `LayoutTrace` to
   record `:port/:view/:widths/:offset`.
5. **Cut 5 — `Object[]` model → typed `Widget`** (late; highest blast radius).
6. **Cut 6 — event/input/focus** (last; thinnest net). Backfill characterization tests, then
   extract `invoke` boundary, then hit-test/focus, then the `processEvent`/`handleMouseEvent`
   switchboards.
- **Where the forks slot in:** Cuts 1–3 don't depend on the enhancement shape — proceed.
  **Review the two forks before committing the model (Cut 5) and event (Cut 6) seams**, so the
  enhancement backlog informs where the extension points go.

---

## 4. What was done this session — Cut 1 in detail (the main review target)

**Change:** route all interned-`String` identity comparisons through one helper, added after
`get()` (~L5941 of `Thinlet.java`):

```java
private static boolean is(Object token, String literal) {
    return token == literal;
}
```

- Semantics are **deliberately identical to `==`** (reference identity), **NOT `equals`** —
  the tokens are interned literals; the toolkit relies on that identity. Wrapping is a pure
  mechanical seam, *not* a correctness fix.
- **449 sites converted** (reviewed count; estimated ~418 at the time) via three scripted `perl -pe` passes over specific, verified
  operand families (classnames, `part`/`insidepart`/`pressedpart`, enum-like values —
  `placement`/`halign`/`valign`/`itemclass`/`iclass`/`fieldclass`/`selection`/`sort`/
  `alignment`/`target`/`parentclass`/`mode`, plus `getClass(ident)`, `get(component,"…")`,
  `definition[N]`). Negations became `!is(...)` with parentheses preserved.
- **Deliberately NOT touched:** the model-core key compares `entry[0] == key` in
  `set`/`get` (~L5912, L5934) — that's the `Object[]` model, rewritten in Cut 5.

**Correctness evidence (what a reviewer can re-check):**
- **String-literal multiset identical before/after** — verified by extracting every `"…"`
  literal pre/post and diffing (empty diff). The transform only re-homes comparisons; no
  string was altered.
- Compiles at `--release 8`.
- **Spotless clean, Checkstyle 0 violations, SpotBugs BUILD SUCCESS** locally (the helper's
  `Object == String` did not trip SpotBugs).
- **CI green across the whole matrix** on PR #38: build (JDK 21); **tests JDK 8/11/17**
  (golden + input net → **no diff**, behavior identical); **japicmp** vs v0.1.0 (**public API
  unchanged**); cross-JDK trace diff (no new divergence).
- 7 comment lines (commented-out code fragments) were incidentally rewritten to `is(...)` —
  cosmetic, harmless (was reported as 3; corrected, §10).

**Reproduce the local gates (no display needed):**
```
./mvnw -B -pl thinlet-core spotless:apply
./mvnw -B -pl thinlet-core -DskipTests verify   # spotless:check + checkstyle + compile + spotbugs
```
The **behavioral** net (golden + input) needs Xvfb `:99` + pinned fonts → it runs in **CI**,
not on a bare host (see §6).

**Artifacts:** **PR #38 — MERGED** (squash **`7796f79`** on `main`); it lands the
`Thinlet.java` sweep, **D42** (DECISIONS.md), and the `CLAUDE.md` PR-workflow note. See the
exact Cut 1 diff with `git show 7796f79`. (The pre-merge feature branch was squash-folded and
has since been deleted — all its content is in `7796f79`.)

---

## 5. Open questions for Fable to weigh in on

1. **Is Cut 1 truly behavior/API-preserving?** Scrutinize the `is(Object, String)` semantics,
   whether any `==` site was missed or wrongly wrapped, and the SpotBugs implication of the
   single `Object == String` in the helper.
2. **Is a single generic `is(Object, String)` the right seam,** or should classname vs.
   part vs. enum-value comparisons be *distinguished now* (e.g. `isClass(...)`) to make the
   eventual enum migration (Cut 5) cleaner? We chose one uniform helper (KISS; specialize
   later) — is that the right call?
3. **Is the net-strength-driven ordering sound?** (== first, paint next as the safest cut,
   layout second, model + event last.) Any reason to resequence?
4. **Biggest blind spots we should shore up before deeper cuts:** interaction-state paint is
   **untraced** (goldens are static); the input surface is thin and *source-derived, not
   trace-backed*; the cross-JDK trace diff is **informational, non-gating**; the tolerance
   config is a single ±2 px default. Which of these must be closed before Cut 2/Cut 4/Cut 6?
5. **Anything about the modernise-then-enhance strategy** that looks risky given behavior +
   API are frozen and the enhancements arrive later as two hacky forks.

---

## 6. Operating model (how work now runs — D42)

- **CI is the autonomous behavior net.** `ci.yml` fires only on `pull_request → main` (push
  to `main` is blocked), so **Claude opens PRs itself** to run the golden + input net across
  JDK 8/11/17/21; runs compile + Spotless/Checkstyle/SpotBugs **locally pre-push**; and drives
  each PR to green — the maintainer is **not** a manual verification dependency.
- **Merge to `main` stays the maintainer's** 1-click gate unless delegated (opt-in
  "auto-merge" ⇒ GitHub squash-on-green). **PR #38 was merged with the maintainer's explicit
  go-ahead** — a one-time authorization, not a standing auto-merge delegation.
- **Local golden runs are not faithful on the bare host** (no Xvfb; host fonts/hinting differ
  from the container the goldens were recorded against → false ±2 px diffs). A faithful
  **local CI** loop (build the dev container: Xvfb `:99` + pinned fonts + JDKs) is a **later
  joint task**; it's the natural point to stand up before Cut 2's paint iteration.

---

## 7. Repo state at handoff

- **Review on `main`** — everything is on the trunk. All feature branches from this session
  are merged and deleted; the only other branch is your `manual/caret-probe` scratch branch,
  intact at `d5f147e`.
- **`main`:** **`9caa655`** — Cut 1 (#38), this brief + the goals charter (#39), and the
  `.gitignore` add (#40). **No open PRs.**
- **PRs #38 (Cut 1) / #39 (docs) / #40 (`.gitignore`): all MERGED.**
- **Housekeeping done this session:** merged dependabot PRs **#33** (GH Actions bumps) and
  **#35** (Spotless 3.8.0 / Checkstyle 13.7.0); deleted stale local branches; deleted the
  stale untracked `.devcontainer/devcontainer-lock.json` (a June-14 relic of the first
  dev-container build, not automation) and added it to `.gitignore` (#40).

---

## 8. Immediate next steps

1. **Cut 1 (#38), docs (#39), `.gitignore` (#40) — all merged.** ✅ `main` at `9caa655`.
2. **Cut 2 (paint → typed Renderer).** First **build the dev container** for a faithful local
   loop; **generate interaction-state paint goldens** (prereq); then extract one static widget
   (label/button) as the Renderer pilot, verify against the recorded op stream, then scale.
3. Before Cut 5/Cut 6: obtain and diff the **two custom `Thinlet.java` forks** to derive the
   enhancement backlog and place the model/event seams where the enhancements need them.

---

## 9. Key references

- **`project-docs/PHASE-3-GOALS.md`** — the Phase 3 **goals charter (seed)**: mission, goals,
  non-goals, principles, success criteria, sub-phases. *Living doc — build on it or correct it.*
- `DECISIONS.md` — **D42** (this pivot + workflow); D7 (±2 px tolerance); D31 (single jar +
  cross-JDK test matrix); D36/D37 (input net as refactor-safety net).
- `KNOWN-QUIRKS.md` — Q1 (`parse()` NPE on unreadable source → the earmarked *first*
  enhancement, deferred out of the modernise phase) and Q2 (splitpane divider).
- `project-docs/backend-portability/` — RENDERING-PRIMITIVES / LAYOUT-ALGORITHMS /
  INPUT-SURFACE / CROSS-JDK-TRACE-DIFF (data-derived spec of the paint/layout/input surface).
- `.claude/PAINT-PIPELINE-MAP.md` — line-numbered map of the paint pipeline + `Object[]` model.
- **Cut 1:** PR #38 · squash commit `7796f79` on `main` (`git show 7796f79`).

---

## 10. Review outcome (2026-07-07, Fable)

Reviewed as requested; **plan endorsed** — refinements, no resequencing. The full record is
**DECISIONS.md D43** (which answers §5's five questions) and the updated
`project-docs/PHASE-3-GOALS.md`. Highlights:

- **Cut 1 verified behavior/API-preserving** by an independent sweep. One seam escapee found
  and wrapped (the Ctrl+A select-all `!=` at ~L4473); an **interning tripwire** added inside
  `is()` (`thinlet.strictIntern`, always on in the test net) so a broken interning chain
  fails loud instead of silently comparing false — armed before Cut 3 touches the chain.
- **japicmp already covers protected** (plugin default) — the subclass surface was never at
  risk. New 3a rule: extractions stay package-private in package `thinlet` (japicmp doesn't
  gate additions; no JPMS on the Java 8 floor).
- Figures corrected in place in this brief: **449** wrapped sites (not ~418), **41/42**
  corpus files (not 40/41), **7** comment-line rewrites (not 3).
- Fork sources expected 2026-07-08; first task on arrival is a catalog diff (verify, don't
  assume, that Cuts 2–4 miss the enhancement surface).
