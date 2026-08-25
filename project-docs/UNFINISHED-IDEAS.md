# Unfinished ideas — work that was started, then stopped

> Decision record: `DECISIONS.md` **D85**.

This file exists because work sometimes stops mid-flight — a branch with real
commits, a half-built tool, an idea explored far enough to be worth something and
then left. When that happens the artifact survives in git, but *everything that
made it legible* — what it was for, how far it got, what stopped it — lives only
in the conversation that produced it. Conversations end. This file is where that
knowledge is written down instead, in enough detail that someone who has never
seen the original discussion can pick the work up cold.

An entry here is not a promise to finish anything. It records what exists and why
it stopped, so that resuming it is a decision rather than an excavation.

## What belongs here — and what does not

**Belongs here:** work that was *started and then stopped*. There is an artifact
— a branch, a script, a draft — and it is not finished.

**Does not belong here** (each already has a home, and the single-home rule of
D57 means this file points rather than repeats):

| Kind of fact | Home |
| --- | --- |
| Why a decision was made | `DECISIONS.md` |
| Work intended but never started | `project-docs/ROADMAP.md` (the 3c backlog) |
| The current state and the ordered next work | `.claude/NEXT-STEPS.md` |
| A behavior quirk and its disposition | `KNOWN-QUIRKS.md` |

Things that stay where they are, named so they are never copied in: **Q14** (the
inert table column header, parked behind the fork sources — D78), the **fork
mapping** itself (arrival-triggered), **Q3 step 2** (the missing-image indicator,
ROADMAP 3c backlog + KNOWN-QUIRKS Q3), and **D5**'s `--release 8` deprecation
hedge. None of those were started; they are intentions, not unfinished work.

## Entry format

```
### <name> — <short description>
- **Status:** Parked / Blocked / Abandoned / Superseded
- **Where it lives:** branch, whether it is pushed, commits, files
- **Intent:** what it was for and why it was worth doing
- **Progress:** what demonstrably works; what has never been run
- **Blocker:** the precise obstacle, with the evidence actually checked
- **How it stalled:** the honest cause
- **Cost to resume:** what a cold start must redo
- **Last touched / last reviewed:** two dates
```

Two rules the format depends on:

- **Qualify every path with its branch.** Entries describe work that usually does
  *not* exist on `main`, so a bare path here would dangle for anyone reading it
  from trunk.
- **Only the bookkeeping fields are terse.** Status and the dates are one-liners;
  **Intent** in particular has no length cap and should run as long as the idea
  needs, because it is the field that has to survive the loss of the conversation.
  Under-writing it is the expensive mistake.

## Entries

### `loop-modernise` — an autonomous behavior-preserving modernisation loop

- **Status:** Parked. Blocked on one piece of local credential wiring (below);
  nothing about the idea is in doubt.

- **Where it lives:** branch **`loop-modernise`**, pushed to `origin` on
  2026-08-25 so it is no longer a single copy. Not merged, no PR. Three commits
  — `5489f3f` (unadapted import), `b32a8df` (adapted to this repository's gates),
  `bd1f5c3` (detect the japicmp no-op) — on top of `main` at `8825515`, so no
  rebase is owed. The whole branch is one new file, **`scripts/loop-modernise.sh`**
  (344 lines), **which exists on that branch only and not on `main`**.

- **Intent:**

  The modernization phases built a regression net — golden paint traces, layout-state
  sidecars, the input suites, the pinned quirks (current tally in
  `.claude/NEXT-STEPS.md`) — strong enough that a behavior-preserving change to the
  2005 source can be *proved* rather than argued. The source itself, though, is
  still 2005 Java: pre-generics collections, manual `StringBuffer` work, no
  `@Override`, resource handling that predates try-with-resources. Modernising it
  by hand means a slice, a PR, a maintainer round-trip, repeated dozens of times
  for changes that are individually trivial and collectively large.

  The loop is the machine that spends the net instead of the maintainer's
  attention. Each pass, Claude picks one small slice of
  `thinlet-core/src/main/java` and modernises it; the script then runs every
  gating CI job locally — `local-ci.sh` on the base row plus JDK rows 8, 11 and
  17, and japicmp against the published `v0.1.0` API — and commits **only** if all
  of them pass. If a slice leaves every golden byte-identical, every input
  assertion green on all four JDKs, and the public API unchanged, then by this
  project's own standards nothing observable moved. That is the entire argument
  for letting it commit unattended.

  Everything else in the design follows from not trusting the loop. One slice per
  pass, so a failure is attributable to a single change rather than to a batch.
  A **scoped** rollback that reverts only `thinlet-core/src/main/java` — never
  `git reset --hard`, which is unbounded and would throw away the maintainer's
  unrelated work along with the bad slice. A stray-file guard that stops the run
  outright if anything outside the target tree changed, which is what keeps the
  frozen artifacts safe by construction: `thinlet.dtd` (D8), the XML corpus
  (D9/D12), the golden traces, the 2005 GIFs, and the script itself. A refusal to
  create new files, because extracting classes is gated Cut 4/5/6 seam work and
  not this loop's job. A `flock` on the worktree, because `local-ci.sh`
  bind-mounts the repo and two concurrent runs would corrupt each other's build
  state. And a preflight that proves the tree is *already* green before the first
  slice, so any later failure is attributable to the loop rather than inherited.

  **A golden diff is a regression signal, never a re-record.** This is the load-
  bearing inversion. D44 and D52 already forbid re-recording to make an
  unexplained diff go away; an unattended loop permitted to re-record at all could
  launder a real regression into a new baseline with nobody watching, so the
  prompt forbids it absolutely — fix the code, never the golden.

  It runs on the **bare host**, not inside the dev container, and that is not a
  preference. Verification goes through `.devcontainer/ci/local-ci.sh`, which
  `exec`s `docker run`; the dev container's only feature is `desktop-lite`, so it
  has neither a docker CLI nor a socket and cannot invoke the container that does
  the verifying. The same constraint explains two smaller oddities: the script
  runs `spotless:apply` itself and then commits with `--no-verify`, because the
  pre-commit hook framework is installed only inside the container and exits 1 on
  the host — and its one job is the formatting the script has just done.

  japicmp gates **every pass**, not just an eventual PR, because D43 forbids
  public-API change and an unattended loop that discovered a break only at PR time
  would have to unpick a stack of otherwise-green commits to find which slice
  caused it. It costs about twelve seconds a pass. There is a cheaper heads-up
  alongside it — a scan of the diff for added `public`/`protected` declarations —
  but that is deliberately advisory only, because reformatting an existing
  declaration reads as an addition to a regex; japicmp is the authority.

  The prompt's hard rules encode this repo's scars. The Java-8 floor with its
  explicit ban list exists because `maven.compiler.release=8` means nothing above
  Java 8 compiles here at all, and the banned idioms — `var`, records, text
  blocks, `List.of`, `String.isBlank` — are exactly what a modern model reaches
  for by reflex. The rule against mechanical substitution across spans containing
  literals is **D52**: a blanket regex rewrote `"font"` into `"t.font"` *inside a
  string literal* and broke a path the goldens did not cover. It is the only
  refactoring regression this repository has had, and the loop is the highest-risk
  place for it to happen again.

- **Progress:** The script is complete and its gates work as far as they have been
  exercised. Under the loop's own harness the dev-container base row runs green
  end to end (`.modernise-verify.log` from the 2026-08-18 run). What has **never**
  happened is a single complete slice — not even under `--dry-run`, which
  verifies one slice and deliberately stops short of committing. No pass has ever
  reached the point of proposing a change, so the prompt, the repair path and the
  commit path are all unexercised.

- **Blocker:** the preflight's japicmp gate cannot run on this machine.

  japicmp compares the build's public API against the **published** `v0.1.0` jar,
  which lives in GitHub Packages — and GitHub Packages requires a token even to
  read public artifacts (D4). CI has one: the `Set up JDK 21 + GitHub Packages
  read auth` step in `.github/workflows/ci.yml` (on `main`) uses `actions/setup-java`
  to write a `~/.m2/settings.xml` containing a `github-nomixer` server. This
  machine has no `~/.m2/settings.xml` at all, so Maven authenticates with nothing,
  takes a 401, and writes only a `thinlet-core-0.1.0.jar.lastUpdated` marker whose
  recorded error is `status code: 401`. The `MAVEN_USER_HOME` the script exports
  cannot help — `mvnw` uses that variable only to locate `wrapper/dists`, never to
  resolve settings.

  The reason this now stops the run rather than being silently tolerated is
  `bd1f5c3`. An unresolvable baseline makes japicmp a **no-op**: it reports
  `Comparing … against ` with an empty right-hand side, scores every member as
  NEW, and the build still succeeds. Exit status alone therefore scores an
  ungated build as a pass, so the script checks for the cached baseline jar and
  refuses to start without it.

  Checked 2026-08-25: the token itself is sufficient — `gh auth` carries
  `read:packages`, and the baseline jar fetches directly over HTTPS (HTTP 200,
  77,131 bytes). Only the Maven wiring is missing.

  Three ways past it, recorded so they are not re-derived:

  1. Have the script mint an ephemeral `settings.xml` from `gh auth token` at run
     time (mode 600, deleted on exit) and pass it with `-s`. No token at rest
     anywhere; works only while `gh` is logged in.
  2. Create a permanent `~/.m2/settings.xml`, mirroring what CI's `setup-java`
     writes. Works for every Maven invocation, at the cost of a credential file
     on the host.
  3. Drop japicmp from the loop and rely on CI's `api-compat` job at PR time.
     Cheapest, but each slice then commits without the D43 public-API gate having
     actually run — which is the thing `bd1f5c3` was written to prevent.

- **How it stalled:** the 2026-08-18 00:39 run stopped in preflight and printed a
  two-step instruction addressed to the maintainer (grant `read:packages`, add a
  `github-nomixer` server to `~/.m2/settings.xml`). No settings file was created,
  the branch was not pushed, and the conversation was not resumed — eight days of
  silence, with no emergency implied. The thread simply ended. `read:packages` is
  present on the token today, but there is no evidence recording when it was
  granted, so whether the first step was ever taken is unknown.

- **Cost to resume:** pick one of the three routes above and wire it in; run
  `scripts/loop-modernise.sh --dry-run` to prove a single slice end to end, which
  is the milestone this work has never reached. Budget roughly two minutes of
  verification per slice attempt on top of whatever the slice itself costs. Note
  that a branch the loop produces carries Java changes, so `gh pr create` on it is
  blocked until the D60 comment pass is attested — the script's own closing
  reminder says so.

- **Last touched:** 2026-08-17 (last commit) / 2026-08-18 (last run).
  **Last reviewed:** 2026-08-25.
