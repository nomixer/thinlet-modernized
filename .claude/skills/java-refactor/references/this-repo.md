# This repository — where to look

Navigational only. This file names **where to look and what to ask**. It deliberately
does not tell you what you will find, and it is not evidence for any conclusion you
draw — see "Derive it; don't repeat it" in `SKILL.md`. Read what it points at.

## Floor

Declared in the root `pom.xml` as `maven.compiler.release`. Read it there each session;
the project raises the floor deliberately, so a remembered value goes stale. Compilation
runs on a much newer JDK than the floor, so a post-floor API compiles for you and breaks
the release.

## How a safety claim gets checked

- `./mvnw -B verify` — the gate (formatter, Checkstyle, SpotBugs).
- `.devcontainer/ci/local-ci.sh` — the net inside the real CI container image. The
  behavior net is font- and JDK-sensitive; a golden diff taken on the bare host proves
  nothing. Use this before believing any "no diff" result.
- The contract is enforced by golden paint/layout traces over a vendored corpus plus
  tagged input-capture suites. Before you call a change observably null, find the
  specific suite that would fail if you were wrong, and name it.
- Note what the trace serializer normalizes and what it does not: anything it normalizes
  is a difference the net will *not* show you, and anything it preserves is a difference
  it will. Both facts matter when you predict whether a change moves a golden.

## What governs a behavior change

Read these and cite the newest relevant entry rather than a remembered one:

- `DECISIONS.md` — the append-only log, and the authority. Entries are dated records
  superseded by later ones, so never read a single entry as current state.
- `KNOWN-QUIRKS.md` — behaviors deliberately pinned rather than fixed.
- `CLAUDE.md` and `.claude/NEXT-STEPS.md` — current posture, the active track, and which
  work is gated.

Behavior changes are not accepted as cleanup here. One that alters observable behavior is
a recorded disposition with its own decision entry and a pinned test flipped in the same
change — not a slice. When you cannot tell which you are holding, treat it as a change.

Some work is deliberately gated pending an external dependency. Check whether what you
are proposing sits behind such a gate before proposing it — and check what the gate
actually covers, since a gate on a subsystem boundary does not necessarily bar an
in-place improvement inside one file.

## The code

A single very large class in early-2000s style, plus a renderer, plus a test-jar testkit
that downstream modules consume. Some of what looks accidental is a contract and some is
just old; which is which is not recorded here on purpose. Work it out from the code, the
call sites and the tests — including the testkit, which walks internal structures
directly and is therefore inside the blast radius of any representation change. Where a
construct's purpose is documented, it is documented next to the construct.

## Before opening a pull request

Java changes require the repository's comment-review pass first; a hook enforces it. Run
`scripts/comment-pass.sh` for the checklist and changed files, review, then
`scripts/comment-pass.sh done`.
