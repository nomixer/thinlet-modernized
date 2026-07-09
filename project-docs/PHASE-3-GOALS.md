# Phase 3 — Goals & Charter (seed)

> **Status:** living seed document. **Build on it or correct it.** Fable and future work may
> edit this freely as understanding sharpens. It is the readable *charter*; the authoritative
> *decision* is **DECISIONS.md D42**, which this doc expands. A material change to the
> goals/scope here should also be recorded as a new `DECISIONS.md` entry (adjusting/superseding
> D42) so the append-only log stays the authority.
>
> **Created:** 2026-07-07 (Opus 4.8 session). Aligns with D42, `ROADMAP.md`, and the readiness
> assessment summarized in `.claude/FABLE-NEXT-STEPS.md`.

---

## Mission (north star)

**Modernise the Thinlet library internals into clean, idiomatic, typed modern Java — then
enhance.** Turn the 7,779-line `Thinlet.java` God class into a maintainable base **without
changing what it does**, so that (a) the maintainer's real applications can run on it and
(b) the prior production enhancements can be re-implemented cleanly on top.

## Why now (the pivot)

Phases 0–2 built the safety net (golden-trace paint+layout + input-capture, cross-JDK
8/11/17/21). The earlier posture — *"modernize the toolchain, not the library"* — was correct
**while building the net**. Phase 3 is where the net finally gets **spent**: the library
itself changes for the first time. This supersedes the "toolchain not library" posture
(**D42**).

## Goals — what Phase 3 delivers

1. **Decompose the God class** into typed, cohesive subsystems (model · DTD/parse · layout ·
   paint/renderer · event/input · focus), replacing the untyped `Object[]{key,value,next}`
   model and the interned-`String` `==` contract with real types.
2. **Preserve 2005 observable behavior exactly.** Every behavior-preserving cut produces
   **no golden/input diff** (±2 px, D7), proven in CI across JDK 8/11/17/21.
3. **Preserve the public API.** Apps compile and run unchanged; enforced by japicmp.
4. **Java 8 floor.** No library language/API feature above Java 8 (enterprise / old-JRE reality).
5. **Produce a clean base real apps can run on** — the maintainer's applications become
   additional living test beds (sub-phase 3b).
6. **Enable clean re-implementation of the prior enhancements** (from the two custom
   `Thinlet.java` forks) on that base (sub-phase 3c).

## Non-goals — explicitly out of scope for **3a** (the modernise sub-phase)

- No user-visible change; no behavior change; **no golden re-record**.
- **No quirk fixes.** Q1 (`parse()` NPE on unreadable source → `IOException`) and Q2
  (splitpane divider) are deferred; Q1 is *earmarked as the first enhancement*, later.
- **No new or changed public API.** A new API / full idiomatic rewrite is a later step (after
  apps run on the clean base).
- No new features or enhancement functionality during the modernise phase.

## Principles / constraints

- **Behavior + API are frozen through 3a** — both non-negotiable.
- **Net before refactor.** No cut lands without the net that guards it; where the net is thin,
  shore up coverage *first* (see blind spots below).
- **Net-strength-driven sequencing.** Modernise the best-covered subsystems first (paint), the
  thinnest-covered last (event/input).
- **Small, verifiable cuts.** Each cut is an independent, behavior-preserving PR that CI proves
  green.
- **CI-autonomous workflow (D42).** Claude runs local gates (compile + Spotless/Checkstyle/
  SpotBugs) and drives CI; the maintainer is not a manual verification dependency. Merge to
  `main` stays the maintainer's gate unless delegated.
- **Tolerance discipline (D7/D35).** The regression gate is ±2 px; `perOp` tolerance stays
  empty until CI's cross-JDK diff surfaces a real over-tolerance position — never widen
  `defaultPx`, never re-record to make a diff go away.
- **Visibility discipline (D43).** japicmp gates API *breaks*, not *additions* — any new
  public type published in a v0.1.x release becomes de-facto frozen API — and the Java-8
  floor (no JPMS) means subpackages force public types. So every class/member extracted
  during 3a stays in package `thinlet`, **package-private**; the clean subpackage layout
  waits for the later new-API phase.
- **Interning tripwire (D43).** The whole test net runs `is()` strict
  (`thinlet.strictIntern=true` via surefire argLine): an equals-but-not-interned token — a
  broken interning chain, the contract's silent failure mode — fails loud. A tripwire hit is
  a finding to triage, never a failure to silence.
- **Seam style (D48).** Extracted subsystem classes are **stateless with explicit context**
  (`render(Thinlet t, Object component, Graphics g, …)`): the maintainer's two production
  forks decoupled behavior from the God-object both ways (public-static utilities;
  instance-as-parameter), and this style makes either a thin 3c wrapper. Package-private
  through 3a per the visibility discipline above.
- **Shared-helper gate (D50).** Shared paint helpers with unguarded transient states
  (`paintScroll`/`paintArrow`: scrollbar/spinbox arrow hover+press, tab hover, menubar)
  stay in `Thinlet` — called via the explicit `t.` context — until their interaction
  goldens land. A widget slice moves only its own branch plus already-guarded helpers
  (e.g. `paintField`); it must not smuggle a shared helper out.
- **Hoist, don't relocate — review-enforced (D48/D50).** Held-state paint goldens cannot
  discriminate a paint-side write hoisted in place from one relocated to an earlier
  event; the timing argument is source reasoning. The `:lead` Down-before-repaint race
  is additionally pinned by two `InputListTest` tests (D50); any other paint-side-effect
  move needs the same scrutiny at review time.

## Success criteria — how we know 3a is done

- The God class is decomposed into **typed subsystems**; the untyped `Object[]` model and the
  interned-`String` `==` contract are **gone** (replaced by types).
- The golden + input net is **still green** across JDK 8/11/17/21, with **no behavior diff**
  accumulated across all cuts.
- **japicmp shows the public API unchanged.**
- The maintainer's real apps **run unchanged** on the modern base (the 3b gate).
- **Closing checklist (D50):** re-narrow any package-private member the decomposition
  widened but no longer uses, so the later subpackage split inherits no phantom surface.

## Sub-phase structure

- **3a — Modernise internals** (current). Behavior + API locked. No user-visible change.
- **3b — Stand the real apps up** on the clean base; they become living test beds that cover
  far more surface than the 2005 corpus.
- **3c — Re-implement the enhancements** cleanly (from the two forks); then (later) the full
  idiomatic rewrite / new public API.

## The sequenced cuts (3a) — net-strength-driven

Detailed rationale in D42 and the readiness assessment (`.claude/FABLE-NEXT-STEPS.md` §2).

| Cut | Scope | Status |
|-----|-------|--------|
| **1** | Neutralise the interned-`String` `==` contract behind one helper (`is`) | ✅ **done** (merged `7796f79`; follow-up + tripwire: D43) |
| **2** | Paint → typed Renderer (net captures the full primitive stream) | ⏳ **in progress** — hoists (D48) + `Renderer` pilot: label/button (D49), then checkbox and the shared `paintField` helper, extracted zero-diff; remaining branches follow slice by slice (golden-guarded first; D50 shared-helper gate) |
| **3** | DTD → typed descriptors + accessor-façade cleanup | pending |
| **4** | Layout → per-widget strategies (a hub; second) | pending |
| **5** | `Object[]` model → typed `Widget` (late; highest blast radius) | pending |
| **6** | Event/input/focus (last; thinnest net) | pending |

Cuts 2 and 3 are **overlappable** (D43): Cut 2's prerequisites (the local CI loop, the
interaction-golden determinism design) take real time, and Cut 3's descriptor-table core can
proceed behind `getDefinition` meanwhile — design the Renderer dispatch anticipating typed
descriptor keys so Cut 3 doesn't force a re-key.

**Net-strengthening prerequisites** (interleaved): the **dev-container local CI loop** —
✅ done (D44, `.devcontainer/ci/local-ci.sh`); interaction-state
paint goldens (before lifting hover/press/focus/selection paint branches) — determinism
design ✅ done (D45, `project-docs/INTERACTION-GOLDENS-DESIGN.md`: paint has **no time
dependence** — the caret does not blink, contrary to D43's premise; hover/press are
held-state captures); capture harness + first 10 goldens ✅ landed (D47) — remaining
scenarios (scrollbar/spinbox arrows, tabs, menubar, tooltip) follow as 2.y fixtures land;
`LayoutTrace` extension to record `:port/:view/:widths/:offset` (before Cut 4);
input characterization tests for the unasserted widgets (before Cut 6) — i.e. **finishing
Phase 2.y**.

## Known blind spots to close (net coverage)

- **Interaction-state paint** — was fully untraced; the first guarded slice landed with
  D47 (button/checkbox hover+press, focus rects, caret, field/textarea selection, list
  selected+lead, open combolist); scrollbar + spinbox arrow hover+press followed with
  D51 (no-op-press protocol — 19 goldens total), making `paintScroll`/`paintArrow`
  extraction-eligible under the D50 shared-helper gate. Still unguarded: tab hover,
  menubar hover/armed, tooltip. **Combobox is *partially* guarded** (D50):
  the open popup + lead highlight is captured, but its arrow/body hover+press and the
  editable-field caret path are not — its extraction waits for those goldens.
- **The input surface is thin** and *source-derived, not trace-backed*; menus, spinner,
  tooltip, slider, tabbedpane, dialog drag/resize, scrollbar-mouse, context-menu,
  focus-traversal and clipboard are unasserted.
- **The cross-JDK trace diff is informational, non-gating**; the hard gate is same-JVM golden +
  getter-based input assertions.

## Where the two custom forks fit

The forks are **multi-file decompositions** (D48) — the maintainer already split Thinlet by
layer in production (paint, layout-inducing actions, …), decoupling behavior from the
God-object via public-static methods (Fork A) and instance-as-parameter methods (Fork B).
That *validates* the cut structure and sets the seam style (Principles). Sources + the apps
built on them arrive the **week of 2026-07-13**; the first task on arrival is the **fork
mapping** (not a file diff): fork files → subsystems; the battle-tested boundaries vs the
Cut 2–6 seams; the functional enhancement backlog; and the static-ability map as empirical
state-coupling evidence. The mapping lands **before the Cut 4/5/6 seam commitments**; the
apps become the 3b test beds.

## Related docs

- **`DECISIONS.md` D42** — the decision this charter expands (also D7 tolerance, D31 single-jar
  + cross-JDK matrix, D35 tolerance posture, D36/D37 input net as refactor-safety net).
- **`project-docs/ROADMAP.md`** — phase-level navigation.
- **`.claude/FABLE-NEXT-STEPS.md`** — session handoff: readiness assessment, Cut 1 detail +
  correctness evidence, and open review questions.
- **`project-docs/backend-portability/`** — the reverse-engineered paint/layout/input spec.
- **`KNOWN-QUIRKS.md`** — Q1/Q2 (deferred; the earmarked first enhancements).
