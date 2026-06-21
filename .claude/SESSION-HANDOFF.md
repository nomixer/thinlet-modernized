# Session handoff — current status (deletable meta)

> Orientation pointer for resuming work (this session or a fresh one). Captures the
> *live* state the repo's permanent docs don't: what's in flight and the open decision.
> Safe to delete once Phase 2.x is accepted/merged. Source of truth remains the repo
> (`DECISIONS.md`, `ROADMAP.md`); this only records "where we are right now."

**As of:** 2026-06-21. **Branch:** `claude/confident-curie-rbymwo`. **PR:** #28 (open, into `main`).

## Where we are

At the **Phase 2.x acceptance gate**. The input-capture **feasibility probe has landed**
(D36) and is green on JDK 21; the decision to proceed to the first real build (the MVP) is
**pending**.

## What's done and pushed (PR #28 = D35 + D36)

- **D36 — feasibility probe** (test scope): `InputProbeDriver`/`InputProbeTest`/
  `InputProbeHandler` + `input/probe.xml` under `thinlet-core/src/test/java/thinlet/trace/`.
  Drives scripted AWT input through the real `processEvent` headless on Xvfb `:99`; asserts
  black-box via getters + re-paint `Trace` diffs. All four seams green & deterministic
  (mouse→state, click→action, re-paint determinism, **keyboard+synthetic focus**).
  `./mvnw -B verify` green (51 tests, 0 Checkstyle, 0 SpotBugs).
- **D35 — Phase 2.0 close-out** also rides this PR (source-derived `input-surface.md`, matrix
  bullet ✅, `perOp` posture) — it had not been merged before this branch continued.
- Docs: `ROADMAP.md` (Phase 2.0 ✅, Phase 2.x ⏳ gate, Phase 3 blocked), `DECISIONS.md`
  **D36**, `input-surface.md` retargeted, `CLAUDE.md` current-work.
- **Gate artifact:** `project-docs/backend-portability/input-harness-probe.md`.

## Open decisions (for the user)

1. **Accept Phase 2.x → authorise the MVP?** Recommendation in the findings note: yes
   (feasible). Options: accept & start MVP / accept & pause for cross-JDK CI / not yet.
2. **MVP scope** (post-acceptance): broaden fixtures/scenarios (list/tree/combo, drag),
   graduate `input-surface.md` to trace-backed. Deferred regardless: drag pseudo-events,
   tooltip/auto-repeat timers, keyboard type-ahead timing.

## Caveats / next checks

- **Cross-JDK (8/11/17) is unproven locally** (only JDK 21 in the container) — it rides the
  `crossjdk` CI matrix on PR #28. Check that run; a focus/keyboard divergence there is the
  first MVP finding to absorb.
- **History note:** the branch carries a pre-squash D34 commit (D34 merged via #27), so PR
  #28's file view may show already-merged D34 files; net change is only D35 + D36. A clean
  rebase/force-push was intentionally **not** done without explicit approval.
