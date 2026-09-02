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

- **Status:** Parked, and no longer blocked. The credential gap is closed and one
  slice has been verified end to end (2026-09-02); what remains undone is the
  decision to let it run for real, which is the maintainer's.

- **Where it lives:** branch **`loop-modernise`**, pushed to `origin`. Not merged,
  no PR. Four commits — `4563769` (unadapted import), `087b3d5` (adapted to this
  repository's gates), `3da36c1` (detect the japicmp no-op), `ea4fafe` (mint the
  Packages credential per run) — rebased onto `main` at `197180d`, which is why
  the first three carry different hashes than the ones D85 recorded. The whole
  branch is one new file, **`scripts/loop-modernise.sh`**, **which exists on that
  branch only and not on `main`**.

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

- **Progress:** A complete pass now exists. On 2026-09-02
  `scripts/loop-modernise.sh --dry-run` reached `preflight green (base verify +
  japicmp)` and then `slice 1 verified; left uncommitted (--dry-run)` — the
  milestone the work had never reached, and the first evidence that the prompt and
  the verification path work rather than merely being written down.

  What the slice did is worth recording, because it is the only sample of this
  loop's judgement that exists. It retired redundant `new String(…)` copies in the
  XML parser and the `parse`/`putProperty` helpers (11 insertions, 9 deletions in
  `Thinlet.java`) — and it left the `new String(new byte[0], 0, 0, enc)` in the
  `?xml encoding` branch alone, adding a comment that it is a charset probe rather
  than a copy. Distinguishing those two is the discrimination the prompt asks for
  and the thing a blanket rewriter gets wrong. The slice passed the base row, JDK
  rows 8/11/17 and japicmp: seven Maven builds, seven `BUILD SUCCESS`, no golden
  diff. It is preserved on `loop-modernise` as a `git stash` entry, not committed —
  proving the loop works is not authorization to modernise the source.

  Confirmed by watching the run rather than reading the script: the nested agent is
  invoked `--print --permission-mode acceptEdits`, so it edits freely but its own
  attempts to run `spotless:apply` and `local-ci.sh` are declined at the permission
  prompt. That is the design working — verification belongs to the script, which
  ran it — not a defect to fix.

  Still unexercised: the **repair** path (no slice has yet failed verification),
  the **commit** path, and any run past a single slice.

- **Blocker:** none. It was the preflight's japicmp gate, cleared on 2026-09-02.

  What it was. japicmp compares the build's public API against the **published**
  `v0.1.0` jar in GitHub Packages, which requires a token even to read public
  artifacts (D4). This host had no `~/.m2/settings.xml`, so Maven authenticated
  with nothing, took a 401, and wrote only a `thinlet-core-0.1.0.jar.lastUpdated`
  marker recording `status code: 401`. `MAVEN_USER_HOME` could not help — `mvnw`
  uses it only to locate `wrapper/dists`, never to resolve settings. That stopped
  the run rather than being tolerated because of `3da36c1`: an unresolvable
  baseline makes japicmp a **no-op** (it reports `Comparing … against ` with an
  empty right-hand side, scores every member as NEW, and still succeeds), so the
  script proves the baseline structurally instead of trusting the exit status.

  Three ways past it were recorded on 2026-08-25 and are named here because the
  reasoning still matters: **(1)** have the script mint an ephemeral
  `settings.xml` at run time; **(2)** create a permanent `~/.m2/settings.xml`
  mirroring what CI's `setup-java` writes; **(3)** drop japicmp from the loop and
  rely on CI's `api-compat` job at PR time.

  **The route actually taken was a fourth one none of those considered: seed the
  local cache, and the credential stops being needed at all.** The loop
  resolves with `-Dmaven.repo.local=.m2/repository`, so the repo-local cache — not
  `~/.m2/repository` — is where the jar had to land. `thinlet-core-0.1.0.jar` and
  its pom, plus the `thinlet-parent` pom the parent chain needs, were fetched from
  GitHub Packages over HTTPS with the `gh` token (HTTP 200; 77,131 bytes; sha256
  `0cf508eb…`) and placed with `install:install-file`. Maven never re-resolves a
  release it already holds, so `./mvnw -o -Papicheck …` now passes **offline**.

  Two things checked rather than assumed, because both could have made this fail
  quietly: the stale 401 `.lastUpdated` marker does **not** interfere once the jar
  is present, and `install:install-file` writes `_remote.repositories` with an
  **empty** repository id (`thinlet-core-0.1.0.jar>=`), which marks the artifact
  locally installed and resolvable by any build — so `-Dmaven.legacyLocalRepo=true`
  was never needed.

  Route 1, the ephemeral mint, is wired into the script as well by `ea4fafe`, so
  a machine without a seeded cache is not stuck: `maven_settings()` mints a `settings.xml` carrying a
  `github-nomixer` server from `gh auth token`, mode 600, deleted by an `EXIT`
  trap, passed with `-s`. It mints nothing when the jar is already cached. Proven
  by moving the cached jar aside and re-running: the baseline came back through
  the minted settings byte-identical, with no credential left in `/tmp`.

  A mint failure deliberately returns 0 rather than erroring, because the
  authority must stay the `BASELINE_JAR`-on-disk check from `3da36c1`. A missing
  credential therefore still surfaces as *the API gate did not run*, and can never
  read as a gate that passed.

  Route 2, the permanent `~/.m2/settings.xml`, was not taken — the seeded cache
  reaches the same place with no credential at rest. Route 3, dropping japicmp,
  was not taken: it is precisely the outcome `3da36c1` exists to prevent.

  **What a reader should take from this:** the two live routes are the seeded
  cache (offline, nothing at rest, but per-machine and lost on a fresh clone) and
  the per-run mint (portable, needs `gh` logged in). They compose — the script
  prefers the cache and falls back to the mint.

- **How it stalled:** the 2026-08-18 00:39 run stopped in preflight and printed a
  two-step instruction addressed to the maintainer (grant `read:packages`, add a
  `github-nomixer` server to `~/.m2/settings.xml`). No settings file was created,
  the branch was not pushed, and the conversation was not resumed — eight days of
  silence, with no emergency implied. The thread simply ended. `read:packages` is
  present on the token today, but there is no evidence recording when it was
  granted, so whether the first step was ever taken is unknown. The stall ended on
  2026-09-02, when the branch triage that found the branch also cleared the gate;
  neither step of that original instruction turned out to be the way past it.

- **Cost to resume:** on this host, nothing — `scripts/loop-modernise.sh` starts
  and preflight passes. On a fresh clone the repo-local cache is empty, so either
  re-seed the baseline or let `maven_settings()` mint from a logged-in `gh`; the
  script's preflight failure message names both routes. Budget roughly two minutes
  of verification per slice on top of whatever the slice itself costs, and note
  that the repair and commit paths are still unexercised, so the first real run
  wants watching. A branch the loop produces carries Java changes, so `gh pr
  create` on it is blocked until the D60 comment pass is attested — the script's
  own closing reminder says so.

- **Last touched:** 2026-09-02 (last commit `ea4fafe`) / 2026-09-02 (last run,
  the first `--dry-run` to complete a slice). **Last reviewed:** 2026-09-02.
