# java-refactor — evaluation record

What was measured, and why the shipped `SKILL.md` looks the way it does. Method:
each eval prompt is answered independently by agents that read a candidate skill and
by an agent forbidden from reading any skill, then a grader marks all arms blind
(labelled A/B/C, positions rotated per case) against the assertions in `evals.json`.
All arms run Sonnet, may only read the repository, and write one recommendation file
outside it. Scoring is pass=1, partial=0.5, fail=0.

## Round 3 — the decisive round (6 cases × 2 reps, 48 agents)

| | v3 (shipped) | v2.1 | no skill |
| --- | --- | --- | --- |
| total | **52** | 49.5 | 41.5 |
| cases won (of 12) | **7** | 5 | 0 |
| held-out case 5, per rep | **4.0 / 5.0** | 3.0 / 3.5 | 2.0 / 2.5 |
| advice that would change behavior | **0** | 2 | 2 |
| grader-verified false claims | 9 | 12 | 4 |

**Case 5 decides it.** Cases 0–4 are contaminated: both candidates were written after
reading earlier graders' findings on them, so a win there is partly hindsight. Case 5
(the `Hashtable`/`Vector` swap) was authored after both candidates were frozen, on
hazards neither mentions — dropped synchronization, `Hashtable`'s null rejection, and
iteration order. v3 wins it on both reps by the widest margin in the suite.

**Where the arms tie, they tie because the answer is easy.** Cases 0 and 3 flip winners
between reps: that is noise, not a result. Cases 1 and 4 are 5.0/5.0 for both candidates.

**The honest caveat on false claims.** No-skill records the fewest, because it makes the
fewest checkable claims at all — it asserts less and cites less. Both skills push an
agent to cite specifics, which raises the error count along with the value. Between the
two candidates the direction still holds: v3 scores higher while making fewer false
claims than v2.1.

## Why the shipped skill is organized around claim discipline

Across every case in every round, the grader's deciding reason was whether specific
factual claims survived checking — not tier vocabulary, not disposition-routing. Counts
that do not reproduce under `grep`, blast-radius estimates from under-scoped searches,
quotations that do not exist in the file they are attributed to, and assertions about
what an old API "always" did are what separate a good answer from a bad one here. That
is what `SKILL.md` is built to prevent.

## Earlier rounds

- **Round 1** (4 cases, n=1) — the 2026-07-17 draft 16.5 vs no-skill 14.0. Case 0, the
  intended showpiece, tied: this repository's own docs already steer an agent correctly,
  so a skill that restates guardrails adds nothing there.
- **Round 2** (5 cases, n=1) — a from-scratch rewrite 20.5, no-skill 19.5, the draft 19.0.
  Margins inside the noise at n=1, which is why round 3 added reps. No-skill won case 4
  outright. The draft scored 2.0 on case 3 by refusing a clean, legal improvement — the
  over-caution failure the shipped skill names explicitly.

Round 3's fuller separation comes from grading that requires verifying claims against
the source; where that is not measured, the arms look much closer.

## Reproducing

Grade every assertion in `evals.json` against each arm's output; `pass_rate` is not
computed for you. Note that skill-creator's viewer only understands two arms, so a
three-arm comparison has to be projected to winner-vs-baseline for that tool.
