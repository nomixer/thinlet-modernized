---
name: java-refactor
description: >-
  Expert guidance for refactoring Java so it is idiomatic at a specific language
  floor (8, 11, 17, or 21) — with honest payoff judgment (big win vs. minor vs.
  cosmetic vs. false gain / regression) and strict behavior-preservation. Use
  this whenever you are refactoring, modernizing, cleaning up, de-crufting, or
  "making more idiomatic" any Java code; whenever you must judge whether a
  proposed Java change is worth it, idiomatic, or safe at the target version; and
  especially for legacy, single-class, or hot-path code where an obvious-looking
  modernization can quietly regress performance or change behavior. Reach for it
  even when the user says "tidy up", "is this good Java?", "should I use streams
  here", "extract this method", or "bring this up to Java N" without naming a
  skill.
---

# Refactoring Java: make claims that survive checking

A refactoring recommendation is not an opinion. It is a bundle of **factual claims** —
this is safe, nothing else touches it, this API has been there since 7, there are
thirteen call sites, that copy was always pointless — and each one is either true or
false. You know Java well enough to reach the right verdict on most requests. Where
recommendations actually fail is one step later: a correct verdict resting on a claim
that doesn't hold, which an engineer then sizes real work from.

So the discipline here is narrow and specific: **be right about the details you assert,
and be honest about the ones you didn't check.**

---

## Establish the floor first

The floor is the Java version the code must compile at — a hard ceiling on syntax and
APIs, not a style preference. Read it from the build, never from the code's appearance:
old-looking code is often *required* to stay old.

- Maven: `maven.compiler.release`, `<source>`/`<target>`, or `<release>` on
  `maven-compiler-plugin`. Gradle: `sourceCompatibility`/`targetCompatibility`,
  `options.release`, or a toolchain block. Otherwise: a `--release N` flag in the build
  or CI config. With no build file at all, **ask**.

`--release N` is authoritative where present: unlike `-source/-target` it also blocks
APIs added after N. The floor is normally *below* the JDK you are running on, and that
gap is where the classic failure lives — your proposal compiles for you and breaks the
release build. When you are unsure whether something predates the floor, check the
Javadoc `@since`, the JEP, or the release notes. Approximately right about a version
boundary is wrong.

Then find how a safety claim gets checked here: the build command, the test suite, any
golden/snapshot/characterization suites, and any behavior-change protocol the project
enforces. Every later claim you make depends on these.

---

## Claim discipline

### Negative and numeric claims are the dangerous ones

"Nothing else touches this." "It's contained to three methods." "There are thirteen call
sites." These get relied on hardest — a blast-radius estimate is what someone scopes the
work from — and they are the easiest to get wrong.

- **State the scope with the claim.** Which trees you searched, what pattern you used.
  A claim scoped to `src/main/java` is useful and honest; the same claim stated
  unqualified is a landmine.
- **Search the tests, and any testkit the project publishes.** Harness code that walks a
  data structure directly is part of that structure's blast radius even though it isn't
  production code. This is the single most common way a containment estimate comes out
  too small.
- **Don't cite a count you didn't run.** Run the command, or describe the shape without a
  number. A wrong count discredits the analysis around it.
- An honestly narrow claim beats a confidently broad wrong one, every time.

### Historical claims need checking too

Legacy code was written against a JDK whose behavior may differ from today's. "That was
pointless even when it was written" is a *historical* claim, not a safe default — the
idiom may have been a real fix for a platform behavior that has since changed. When you
retire an old workaround, name the version that made it unnecessary and confirm the floor
is above it. Getting this right is also the difference between telling someone their
predecessor was sloppy and telling them the ground moved.

### Quote verbatim or don't quote

If you cite a document, a comment, or a test name, use its actual words and actual name.
A paraphrase in quotation marks reads as evidence while being unverifiable.

### Derive it; don't repeat it

Your conclusion is worth the evidence under it. A project's conventions doc — including
any reference file shipped with this skill — tells you *where to look and what to ask*.
It is not evidence for your conclusion. "The guide says this is load-bearing" is the
weakest argument available to you, and it is visibly weaker than "I found the call sites
and the test that pins it, here they are."

Research the wider world when a payoff claim is genuinely contested, an API may have been
superseded, or a micro-optimization's direction is non-obvious on a modern JIT. Prefer
JEPs, release notes, official Javadoc and JMH measurements over undated blog posts, and
cite what you used. Don't research settled basics.

---

## Work out what the change actually changes

A refactor is **observably null**. These are the things that quietly aren't:

- `==` vs `.equals()`, where identity may be deliberate (interned tokens, sentinels).
- Null flow; which exception is thrown, and *when* — eager vs. lazy, evaluation and
  side-effect order, short-circuiting.
- Iteration and encounter order; sort stability.
- Numeric behavior: integer vs. floating division, widening, overflow, boxed-value
  identity and caching.
- Serialization shape; and any signature or *name* reached by reflection, XML or config.
- **Ordering, not just existence.** Removing a duplicated cleanup — an inline `close()`
  that a `finally` seems to repeat — is rarely pure duplication: on some paths the inline
  one is the effective one. Deleting it moves *when* the work happens relative to
  everything in between, which can push a resource release past a user callback. Walk
  each path and say which copy actually fires there.

The test is: **could any existing test, or any caller, observe the difference?** If yes,
it is a behavior change, not a cleanup — say so and route it through the project's change
process instead of folding it into a tidy-up.

Then check rather than assert. Name the specific suite or command that would catch you if
you were wrong, and run it when you apply the change. If a golden diff or a test moves,
that is a signal you changed behavior — investigate it, never re-record it to make it
quiet.

---

## Say whether it is worth doing

Lead with the verdict, before the diff: **big win**, **real but minor**, **cosmetic**, or
**false gain — recommend against**. Streams and lambdas in a per-frame paint or a tight
inner loop, `Optional` as a field, an interface plus factory for one implementation, and
replacing a representation that is load-bearing all belong in the last bucket; say what
each costs.

There are two ways to be useless here, not one. **Over-compliance** agrees that
everything asked for is worth doing. **Over-caution** refuses to propose anything because
the code looks old or the project has gates — and a clean, legal, well-understood
improvement declined with "best left alone" is a worse answer than no answer. When the
honest position is "here is one good slice, and here are three things I'm deliberately
not doing", say exactly that.

---

## One slice at a time

1. **Survey and rank** — grade the candidates, and name the false gains you are
   deliberately not doing so nobody "fixes" them later.
2. **Propose one slice** — what and where, the verdict, floor-legality, the
   behavior-preservation argument, and the command that verifies it. Then stop and wait,
   unless told to run autonomously.
3. **Apply** — the minimal edit, matching surrounding style. Never restyle lines you are
   not functionally changing.
4. **Verify** — run the build and the net; report what actually happened.
5. **Document** — record why the change was safe, so the next reader doesn't undo it.

Saw something else on the way? Note it for a later slice.
